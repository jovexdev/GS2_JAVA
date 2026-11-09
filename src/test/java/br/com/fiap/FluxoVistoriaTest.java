package br.com.fiap;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "aegis.gestor.email=gestor@example.test", "aegis.gestor.senha=senha-teste",
        "aegis.agente.email=agente@example.test", "aegis.agente.senha=senha-teste"
})
@AutoConfigureMockMvc
class FluxoVistoriaTest {
    private static final String URL = "jdbc:h2:mem:aegis;MODE=Oracle";
    private static Connection conexaoDoEsquema;
    private static final String RELATO = """
            {"descricao":"Água em baldes","localizacao":"Endereço fictício","aguaParada":true,
             "reservatorioAberto":true,"materiaisExpostos":false}
            """;
    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) throws SQLException, IOException {
        conexaoDoEsquema = DriverManager.getConnection(URL, "teste", "teste");
        try (var script = Files.newBufferedReader(Path.of("database/01_schema.sql"))) {
            RunScript.execute(conexaoDoEsquema, script);
        }
        registry.add("aegis.db.url", () -> URL);
        registry.add("aegis.db.driver", () -> "org.h2.Driver");
        registry.add("aegis.db.usuario", () -> "teste");
        registry.add("aegis.db.senha", () -> "teste");
    }

    @AfterAll
    static void fecharBanco() throws SQLException {
        if (conexaoDoEsquema != null) {
            conexaoDoEsquema.close();
        }
    }

    @Test
    void percorreCadastroCrudPermissoesEVistoria() throws Exception {
        mvc.perform(get("/api/saude/oracle")).andExpect(status().isOk());
        mvc.perform(get("/api/ocorrencias")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"perfil":"EMPRESA","nome":"Empresa piloto","documento":"11.222.333/0001-81",
                 "email":"empresa@example.test","endereco":"Endereço fictício","responsavel":"Pessoa teste",
                 "senha":"senha-teste"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"perfil":"CIDADAO","nome":"Cidadã piloto","documento":"123.456.789-09",
                 "email":"cidada@example.test","endereco":"Endereço fictício","senha":"senha-teste"}
                """)).andExpect(status().isCreated());
        MockHttpSession empresa = login("empresa@example.test");
        MockHttpSession cidada = login("cidada@example.test");
        MockHttpSession gestor = login("gestor@example.test");
        MockHttpSession agente = login("agente@example.test");

        String rascunho = criarOcorrencia(empresa);
        mvc.perform(delete(rascunho).session(empresa)).andExpect(status().isNoContent());
        mvc.perform(get(rascunho).session(empresa)).andExpect(status().isNotFound());

        String ocorrencia = criarOcorrencia(empresa);
        mvc.perform(get(ocorrencia).session(cidada)).andExpect(status().isNotFound());
        mvc.perform(post(ocorrencia + "/vistoria").session(cidada)).andExpect(status().isForbidden());
        mvc.perform(put(ocorrencia).session(empresa).contentType(MediaType.APPLICATION_JSON).content(RELATO))
                .andExpect(status().isOk());
        String vistoria = mvc.perform(post(ocorrencia + "/vistoria").session(empresa))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(post(ocorrencia + "/vistoria").session(empresa)).andExpect(status().isConflict());
        mvc.perform(delete(ocorrencia).session(empresa)).andExpect(status().isConflict());
        mvc.perform(get(vistoria).session(agente)).andExpect(status().isNotFound());

        mvc.perform(put(vistoria + "/prioridade").session(gestor).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"prioridade":"INSTAVEL","justificativa":"Confirmar a extensão em campo."}
                        """)).andExpect(status().isOk()).andExpect(jsonPath("$.prioridade").value("INSTAVEL"));
        String horario = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1)
                .withSecond(0).withNano(0).toString();
        mvc.perform(put(vistoria + "/agendamento").session(gestor).contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipeId\":1,\"agendadaPara\":\"" + horario + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AGENDADA"));
        mvc.perform(get(vistoria + "/ocorrencia").session(agente)).andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridadePreliminar").value("CRITICA"));
        mvc.perform(put(vistoria + "/conclusao").session(agente).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resultado":"Vistoria simulada concluída."}
                        """)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));
        mvc.perform(get(vistoria + "/historico").session(empresa)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].acao").value("CONCLUIDA"));
        mvc.perform(delete("/api/sessao").session(empresa)).andExpect(status().isNoContent());
        mvc.perform(get("/api/sessao")).andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/sessao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"senha-teste\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String criarOcorrencia(MockHttpSession sessao) throws Exception {
        return mvc.perform(post("/api/ocorrencias").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content(RELATO)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.prioridadePreliminar").value("CRITICA"))
                .andReturn().getResponse().getHeader("Location");
    }
}

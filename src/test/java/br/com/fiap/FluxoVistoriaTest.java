package br.com.fiap;

import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.entities.Perfil;
import br.com.fiap.excecoes.ApiException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Autowired
    private UsuarioBO usuarios;

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

    @Test
    void protegeCadastroDeOperadoresEConferePerfilSelecionado() throws SQLException {
        var agente = new UsuarioBO.CadastroOperador(Perfil.AGENTE, "Agente autorizado",
                "agente-autorizado@example.test", "senha-teste", "AG-002", null, 1L);
        assertThatThrownBy(() -> usuarios.cadastrarOperador(agente, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("autorização de um gestor");

        var gestor = new UsuarioBO.Login("gestor@example.test", "senha-teste");
        var novoGestor = new UsuarioBO.CadastroOperador(Perfil.GESTOR, "Segundo gestor",
                "segundo-gestor@example.test", "senha-teste", "GS-002", "Operações", null);
        assertThatThrownBy(() -> usuarios.cadastrarOperador(novoGestor, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("Já existe um gestor");

        UsuarioBO.Login loginAgente = new UsuarioBO.Login(agente.email(), agente.senha());
        assertThat(usuarios.cadastrarOperador(agente, gestor).equipeId()).isEqualTo(1L);
        assertThat(usuarios.autenticar(loginAgente, Perfil.AGENTE).nome()).isEqualTo("Agente autorizado");
        assertThatThrownBy(() -> usuarios.autenticar(loginAgente, Perfil.GESTOR))
                .isInstanceOf(ApiException.class).hasMessageContaining("perfil selecionado");
    }

    @Test
    void acompanhaRelatoCidadaoEConcluiVistoriaSomenteComAgenteDesignado() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"perfil":"CIDADAO","nome":"Cidadã do bairro","documento":"98765432101",
                 "email":"bairro@example.test","endereco":"Rua do Bairro","senha":"senha-teste"}
                """)).andExpect(status().isCreated());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"perfil":"EMPRESA","nome":"Estabelecimento de teste","documento":"11222333000262",
                 "email":"loja@example.test","endereco":"Rua Inicial","responsavel":"Pessoa Teste","senha":"senha-teste"}
                """)).andExpect(status().isCreated());

        MockHttpSession cidada = login("bairro@example.test");
        MockHttpSession empresa = login("loja@example.test");
        MockHttpSession gestor = login("gestor@example.test");
        MockHttpSession agente = login("agente@example.test");
        var gestorLogin = new UsuarioBO.Login("gestor@example.test", "senha-teste");
        var designado = usuarios.cadastrarOperador(new UsuarioBO.CadastroOperador(Perfil.AGENTE, "Agente designado",
                "designado@example.test", "senha-teste", "AG-DES", null, 1L), gestorLogin);
        MockHttpSession agenteDesignado = login("designado@example.test");

        mvc.perform(get("/api/empresa/estabelecimento").session(cidada)).andExpect(status().isForbidden());
        mvc.perform(put("/api/empresa/estabelecimento").session(empresa).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"razaoSocial":"Estabelecimento atualizado","nomeFantasia":"Loja Teste","endereco":"Rua Atualizada",
                         "atividadeEconomica":"Comércio","responsavel":"Pessoa Teste","telefone":"0000-0000",
                         "caracteristicasInstalacoes":"Pátio e depósito"}
                        """)).andExpect(status().isOk()).andExpect(jsonPath("$.cnpj").value("11222333000262"));

        String relatoCidada = mvc.perform(post("/api/ocorrencias").session(cidada).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"descricao":"Água acumulada em praça","localizacao":"Praça fictícia"}
                        """)).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        String decisao = """
                {"realizarVistoria":false,"justificativa":"Encaminhamento preventivo sem vistoria."}
                """;
        mvc.perform(post(relatoCidada + "/analise").session(cidada).contentType(MediaType.APPLICATION_JSON).content(decisao))
                .andExpect(status().isForbidden());
        String encerrada = mvc.perform(post(relatoCidada + "/analise").session(gestor)
                .contentType(MediaType.APPLICATION_JSON).content(decisao))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(get(encerrada).session(cidada)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
        mvc.perform(get(encerrada + "/historico").session(cidada)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].detalhe").value("Encaminhamento preventivo sem vistoria."));

        String relatoEmpresa = criarOcorrencia(empresa);
        String vistoria = mvc.perform(post(relatoEmpresa + "/vistoria").session(empresa))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(put(vistoria + "/prioridade").session(gestor).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"prioridade":"INSTAVEL","justificativa":"Confirmar as condições em campo."}
                        """)).andExpect(status().isOk());
        mvc.perform(put(vistoria + "/designacao").session(gestor).contentType(MediaType.APPLICATION_JSON)
                .content("{\"agenteId\":" + designado.id() + "}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.agenteNome").value("Agente designado"));

        String horario = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(2)
                .withSecond(0).withNano(0).toString();
        mvc.perform(put(vistoria + "/agendamento").session(gestor).contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipeId\":1,\"agendadaPara\":\"" + horario + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(vistoria).session(agente)).andExpect(status().isNotFound());
        mvc.perform(get(vistoria + "/local").session(agenteDesignado)).andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco").value("Rua Atualizada"));

        mvc.perform(put(vistoria + "/inicio").session(agenteDesignado)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        mvc.perform(put(vistoria + "/conclusao").session(agenteDesignado).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        mvc.perform(put(vistoria + "/inspecao").session(agenteDesignado).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"texto":"Recipiente com água encontrado."}
                        """)).andExpect(status().isOk());
        mvc.perform(put(vistoria + "/recomendacoes").session(agenteDesignado).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"texto":"Esvaziar os recipientes e cobrir o reservatório."}
                        """)).andExpect(status().isOk());
        mvc.perform(get(vistoria).session(empresa)).andExpect(status().isOk())
                .andExpect(jsonPath("$.inspecao").doesNotExist());
        mvc.perform(get(vistoria + "/historico").session(empresa)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(put(vistoria + "/conclusao").session(agenteDesignado).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));
        mvc.perform(get(vistoria).session(empresa)).andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendacoes").value("Esvaziar os recipientes e cobrir o reservatório."));
        mvc.perform(put(vistoria + "/encerramento").session(gestor).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"texto":"Resultado de campo revisado."}
                        """)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADA"));
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

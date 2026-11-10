package br.com.fiap.main;

import br.com.fiap.AegisApplication;
import br.com.fiap.bo.EpidemiologiaBO;
import br.com.fiap.bo.LiaBO;
import br.com.fiap.bo.OcorrenciaBO;
import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.conexoes.ConexaoFactory;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.sql.SQLException;

public class AegisTerminal {
    private final ConexaoFactory conexoes;
    private final UsuarioBO usuarios;
    private final OcorrenciaBO ocorrencias;
    private final VistoriaBO vistorias;
    private final EpidemiologiaBO epidemiologia;
    private final LiaBO lia;
    private Usuario usuarioLogado;

    private AegisTerminal(ConfigurableApplicationContext contexto) {
        conexoes = contexto.getBean(ConexaoFactory.class);
        usuarios = contexto.getBean(UsuarioBO.class);
        ocorrencias = contexto.getBean(OcorrenciaBO.class);
        vistorias = contexto.getBean(VistoriaBO.class);
        epidemiologia = contexto.getBean(EpidemiologiaBO.class);
        lia = contexto.getBean(LiaBO.class);
    }

    public static void main(String[] args) {
        Entrada.executar(() -> {
            System.out.println("=== AEGIS | Terminal ===");
            var propriedades = new CredenciaisTerminal().propriedades();
            try (var contexto = new SpringApplicationBuilder(AegisApplication.class)
                    .properties("server.port=0")
                    .initializers(c -> c.getEnvironment().getPropertySources()
                            .addFirst(new MapPropertySource("credenciaisDoTerminal", propriedades)))
                    .run(args)) {
                String porta = contexto.getEnvironment().getRequiredProperty("local.server.port");
                System.out.println("Spring Boot iniciado. API REST em http://127.0.0.1:" + porta);
                new AegisTerminal(contexto).menu();
            }
        });
    }

    private void menu() {
        while (true) {
            System.out.println("""

                    === AEGIS | Menu principal ===
                    1 - Testar conexão Oracle
                    2 - Cadastrar cidadão ou empresa
                    3 - Entrar na conta AEGIS
                    4 - Ocorrências ambientais
                    5 - Vistorias
                    6 - GeoRadar e Observatório (indicadores do InfoDengue)
                    7 - Previsão epidemiológica
                    8 - Conversar com a LIA
                    9 - Preparar gestor ou agente piloto
                    10 - Sair da conta
                    0 - Encerrar o AEGIS
                    """);
            if (usuarioLogado != null) {
                System.out.println("Conta ativa: " + usuarioLogado.nome() + " (" + usuarioLogado.perfil() + ")");
            }
            String opcao = Entrada.texto("Opção");
            if ("0".equals(opcao) || !Entrada.executar(() -> executar(opcao))) {
                return;
            }
        }
    }

    private void executar(String opcao) throws SQLException, ClassNotFoundException {
        switch (opcao) {
            case "1" -> BancoTerminal.verificar(conexoes);
            case "2" -> CadastroTerminal.cadastrar(usuarios);
            case "3" -> {
                usuarioLogado = Entrada.login(usuarios);
                System.out.println("Login realizado.");
            }
            case "4" -> menuOcorrencias(exigirLogin());
            case "5" -> VistoriasTerminal.executar(vistorias, exigirLogin());
            case "6" -> mostrarIndicadores();
            case "7" -> mostrarPrevisao();
            case "8" -> conversarComLia();
            case "9" -> prepararOperador();
            case "10" -> {
                usuarioLogado = null;
                System.out.println("Conta encerrada no terminal.");
            }
            default -> System.out.println("Escolha uma opção do menu.");
        }
    }

    private Usuario exigirLogin() {
        if (usuarioLogado == null) {
            throw ApiException.naoAutenticado("Use a opção 3 para entrar na sua conta AEGIS.");
        }
        return usuarioLogado;
    }

    private void menuOcorrencias(Usuario usuario) throws SQLException {
        String acao = Entrada.escolher("Ocorrências ambientais", "Registrar", "Listar", "Buscar por ID", "Atualizar", "Excluir");
        switch (acao) {
            case "Registrar" -> {
                var criada = ocorrencias.registrar(Entrada.relato(usuario.perfil()), usuario);
                System.out.println("Ocorrência cadastrada. Guarde o ID " + criada.id() + ".");
                System.out.println(criada);
            }
            case "Listar" -> {
                var lista = ocorrencias.listar(usuario);
                System.out.println("Ocorrências encontradas: " + lista.size());
                lista.forEach(System.out::println);
            }
            case "Buscar por ID" -> System.out.println(ocorrencias.consultar(Entrada.id("ID da ocorrência"), usuario));
            case "Atualizar" -> {
                long id = Entrada.id("ID da ocorrência");
                System.out.println(ocorrencias.atualizar(id, Entrada.relato(usuario.perfil()), usuario));
            }
            case "Excluir" -> {
                long id = Entrada.id("ID da ocorrência");
                System.out.println(ocorrencias.consultar(id, usuario));
                if (Entrada.confirmar("Excluir a ocorrência " + id + "?")) {
                    ocorrencias.excluir(id, usuario);
                    System.out.println("Ocorrência excluída.");
                }
            }
        }
    }

    private void mostrarIndicadores() {
        Periodo periodo = lerPeriodo();
        var serie = epidemiologia.historico(periodo.municipio(), periodo.ano());
        System.out.printf("Fonte: %s | Município IBGE: %d | Ano: %d%n", serie.fonte(), serie.municipio(), serie.ano());
        System.out.println("Semanas encontradas: " + serie.indicadores().size());
        serie.indicadores().forEach(semana -> System.out.printf(
                "SE %d | %s | Notificados: %s | Estimados: %s | Incidência/100 mil: %s | Alerta: %s%n",
                semana.semanaEpidemiologica(), semana.inicioDaSemana(), semana.casosNotificados(),
                semana.casosEstimados(), semana.incidenciaPor100Mil(), semana.nivelDeAlerta()));
    }

    private void mostrarPrevisao() {
        Periodo periodo = lerPeriodo();
        var previsao = epidemiologia.prever(periodo.municipio(), periodo.ano());
        System.out.printf("Semana iniciada em %s | %d casos estimados%n", previsao.inicioDaSemana(),
                previsao.casosEstimados());
        System.out.println(previsao.metodo());
        System.out.println(previsao.limitacoes());
    }

    private void conversarComLia() {
        exigirLogin();
        String pergunta = Entrada.texto("Pergunta para a LIA (sem dados pessoais)");
        Periodo periodo = Entrada.confirmar("Incluir dados de um município na pergunta?")
                ? lerPeriodo() : new Periodo(null, null);
        System.out.println(lia.conversar(new LiaBO.Pergunta(pergunta, periodo.municipio(), periodo.ano())).resposta());
    }

    private void prepararOperador() throws SQLException {
        Perfil perfil = Perfil.valueOf(Entrada.escolher("Conta interna piloto", "GESTOR", "AGENTE"));
        String email = Entrada.texto("E-mail da conta interna");
        String senha = Entrada.senha("Senha com pelo menos 8 caracteres");
        usuarios.prepararOperador(perfil, email, senha, perfil == Perfil.AGENTE ? 1L : null);
        System.out.println("Conta piloto disponível. Use a opção 3 para entrar.");
    }

    private Periodo lerPeriodo() {
        String municipio = Entrada.texto("Código IBGE (Enter para São Paulo)");
        String ano = Entrada.texto("Ano (Enter para o último ano completo)");
        return new Periodo(municipio.isBlank() ? null : Integer.valueOf(municipio),
                ano.isBlank() ? null : Integer.valueOf(ano));
    }

    private record Periodo(Integer municipio, Integer ano) {
    }
}

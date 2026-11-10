package br.com.fiap.main;

import br.com.fiap.AegisApplication;
import br.com.fiap.bo.EpidemiologiaBO;
import br.com.fiap.bo.LiaBO;
import br.com.fiap.bo.OcorrenciaBO;
import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.services.OpenAiService;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.SQLException;

public class AegisTerminal {
    private final UsuarioBO usuarios;
    private final OcorrenciaBO ocorrencias;
    private final VistoriaBO vistorias;
    private final EpidemiologiaBO epidemiologia;
    private final LiaBO lia;
    private boolean encerrado;

    private AegisTerminal(ConfigurableApplicationContext contexto) {
        usuarios = contexto.getBean(UsuarioBO.class);
        ocorrencias = contexto.getBean(OcorrenciaBO.class);
        vistorias = contexto.getBean(VistoriaBO.class);
        epidemiologia = contexto.getBean(EpidemiologiaBO.class);
        lia = contexto.getBean(LiaBO.class);
    }

    public static void main(String[] args) {
        Entrada.executar(() -> {
            System.out.println("=== AEGIS | Terminal ===");
            try (var contexto = new SpringApplicationBuilder(AegisApplication.class)
                    .properties("server.port=0")
                    .run(args)) {
                String porta = contexto.getEnvironment().getRequiredProperty("local.server.port");
                System.out.println("Spring Boot iniciado. API REST em http://127.0.0.1:" + porta);
                System.out.println("OpenAI: " + contexto.getBean(OpenAiService.class).origemChave());
                var terminal = new AegisTerminal(contexto);
                while (!terminal.encerrado && Entrada.executar(terminal::acessar)) {
                    // Volta à escolha de perfil quando o usuário sai da conta.
                }
            }
        });
    }

    private void acessar() throws SQLException {
        Perfil perfil = CadastroTerminal.escolherPerfil();
        if (perfil == null) {
            encerrado = true;
            return;
        }

        String acao = Entrada.escolher("Acesso ao AEGIS", "Entrar", "Criar cadastro", "Voltar");
        if ("Voltar".equals(acao)) {
            return;
        }

        Usuario usuario = "Criar cadastro".equals(acao) ? CadastroTerminal.cadastrar(usuarios, perfil)
                : usuarios.autenticar(Entrada.dadosLogin("E-mail cadastrado no AEGIS", "Senha da sua conta no AEGIS"), perfil);
        menu(usuario);
    }

    private void menu(Usuario usuario) {
        String[] opcoes = switch (usuario.perfil()) {
            case CIDADAO -> new String[]{"Minhas ocorrências", "Indicadores do InfoDengue", "Previsão epidemiológica",
                    "Conversar com a LIA", "Sair da conta", "Encerrar"};
            case EMPRESA -> new String[]{"Minhas ocorrências", "Minhas vistorias", "Indicadores do InfoDengue",
                    "Previsão epidemiológica", "Conversar com a LIA", "Sair da conta", "Encerrar"};
            case GESTOR -> new String[]{"Registros ambientais", "Centro de Operações", "Sair da conta", "Encerrar"};
            case AGENTE -> new String[]{"Atendimentos da equipe", "Sair da conta", "Encerrar"};
        };
        while (true) {
            String acao = Entrada.escolher("\n=== AEGIS | " + usuario.nome() + " (" + usuario.perfil() + ") ===", opcoes);
            if ("Sair da conta".equals(acao)) {
                return;
            }
            if ("Encerrar".equals(acao) || !Entrada.executar(() -> executar(acao, usuario))) {
                encerrado = true;
                return;
            }
        }
    }

    private void executar(String acao, Usuario usuario) throws SQLException {
        switch (acao) {
            case "Minhas ocorrências", "Registros ambientais" -> menuOcorrencias(usuario);
            case "Minhas vistorias", "Centro de Operações", "Atendimentos da equipe" ->
                    VistoriasTerminal.executar(vistorias, usuario);
            case "Indicadores do InfoDengue" -> mostrarIndicadores();
            case "Previsão epidemiológica" -> mostrarPrevisao();
            case "Conversar com a LIA" -> conversarComLia();
        }
    }

    private void menuOcorrencias(Usuario usuario) throws SQLException {
        String[] opcoes = usuario.perfil() == Perfil.GESTOR
                ? new String[]{"Listar", "Buscar por ID", "Voltar"}
                : new String[]{"Registrar", "Listar", "Buscar por ID", "Atualizar", "Excluir", "Voltar"};
        String acao = Entrada.escolher("Ocorrências ambientais", opcoes);
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
        String pergunta = Entrada.texto("Pergunta para a LIA (sem dados pessoais)");
        Periodo periodo = Entrada.confirmar("Incluir dados de um município na pergunta?")
                ? lerPeriodo() : new Periodo(null, null);
        System.out.println(lia.conversar(new LiaBO.Pergunta(pergunta, periodo.municipio(), periodo.ano())).resposta());
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

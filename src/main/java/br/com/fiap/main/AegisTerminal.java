package br.com.fiap.main;

import br.com.fiap.AegisApplication;
import br.com.fiap.bo.EpidemiologiaBO;
import br.com.fiap.bo.LiaBO;
import br.com.fiap.bo.OcorrenciaBO;
import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.entities.Ocorrencia;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Vistoria;
import br.com.fiap.excecoes.ApiException;
import br.com.fiap.entities.Usuario;
import br.com.fiap.services.OpenAiService;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            PainelTerminal.titulo("AEGIS | VIGILÂNCIA DA DENGUE");
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
            case CIDADAO -> new String[]{"Consultar GeoRadar", "Visualizar gráficos", "Consultar previsão",
                    "Conversar com a LIA", "Registrar ocorrência", "Acompanhar ocorrências",
                    "Sair da conta", "Encerrar"};
            case EMPRESA -> new String[]{"Gerenciar estabelecimento", "Descrever situação ambiental",
                    "Consultar classificação", "Solicitar vistoria", "Acompanhar atendimento",
                    "Consultar histórico", "Sair da conta", "Encerrar"};
            case GESTOR -> new String[]{"Analisar registros ambientais", "Centro de Operações", "Sair da conta", "Encerrar"};
            case AGENTE -> new String[]{"Minhas vistorias", "Sair da conta", "Encerrar"};
        };
        while (true) {
            String acao = Entrada.escolher(PainelTerminal.perfil(usuario.perfil()).toUpperCase(java.util.Locale.ROOT)
                    + " | " + usuario.email(), opcoes);
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
            case "Consultar GeoRadar" -> mostrarGeoRadar();
            case "Visualizar gráficos" -> mostrarGrafico();
            case "Consultar previsão" -> mostrarPrevisao();
            case "Conversar com a LIA" -> conversarComLia();
            case "Registrar ocorrência", "Descrever situação ambiental" -> {
                var criada = ocorrencias.registrar(Entrada.relato(usuario.perfil()), usuario);
                PainelTerminal.ocorrencia(criada, null);
            }
            case "Acompanhar ocorrências", "Consultar classificação" -> mostrarOcorrencia(usuario);
            case "Gerenciar estabelecimento" -> gerenciarEstabelecimento(usuario);
            case "Solicitar vistoria" -> {
                var atendimentos = atendimentos(usuario);
                var disponiveis = ocorrencias.listar(usuario).stream().filter(o -> !atendimentos.containsKey(o.id())).toList();
                long id = selecionarOcorrencia(disponiveis, atendimentos);
                PainelTerminal.ocorrencia(ocorrencias.consultar(id, usuario), null);
                if (Entrada.confirmar("Enviar a ocorrência para avaliação?")) {
                    PainelTerminal.vistoria(vistorias.solicitar(id, usuario));
                }
            }
            case "Acompanhar atendimento" -> VistoriasTerminal.acompanhar(vistorias, usuario);
            case "Consultar histórico" -> VistoriasTerminal.historico(vistorias, usuario);
            case "Analisar registros ambientais" -> analisarOcorrencia(usuario);
            case "Centro de Operações", "Minhas vistorias" -> VistoriasTerminal.executar(vistorias, usuario);
        }
    }

    private void mostrarOcorrencia(Usuario usuario) throws SQLException {
        var atendimentos = atendimentos(usuario);
        long id = selecionarOcorrencia(ocorrencias.listar(usuario), atendimentos);
        Vistoria atendimento = atendimentos.get(id);
        PainelTerminal.ocorrencia(ocorrencias.consultar(id, usuario), atendimento);
        if (atendimento != null) {
            PainelTerminal.vistoria(atendimento);
            PainelTerminal.historico(atendimento.id(), vistorias.historico(atendimento.id(), usuario));
        }
    }

    private void analisarOcorrencia(Usuario gestor) throws SQLException {
        var atendimentos = atendimentos(gestor);
        long id = selecionarOcorrencia(ocorrencias.listar(gestor), atendimentos);
        Vistoria atendimento = atendimentos.get(id);
        PainelTerminal.ocorrencia(ocorrencias.consultar(id, gestor), atendimento);
        if (atendimento != null) {
            PainelTerminal.vistoria(atendimento);
            PainelTerminal.historico(atendimento.id(), vistorias.historico(atendimento.id(), gestor));
            return;
        }

        String decisao = Entrada.escolher("Decisão sobre o relato", "Encaminhar para vistoria",
                "Encerrar sem vistoria", "Voltar");
        if (!"Voltar".equals(decisao)) {
            var dados = new VistoriaBO.DecisaoOcorrencia("Encaminhar para vistoria".equals(decisao),
                    Entrada.texto("Justificativa da decisão"));
            var analisada = vistorias.analisarOcorrencia(id, dados, gestor);
            PainelTerminal.vistoria(analisada);
            PainelTerminal.historico(analisada.id(), vistorias.historico(analisada.id(), gestor));
        }
    }

    private Map<Long, Vistoria> atendimentos(Usuario usuario) throws SQLException {
        return vistorias.listar(usuario).stream().collect(Collectors.toMap(Vistoria::ocorrenciaId, Function.identity()));
    }

    private long selecionarOcorrencia(List<Ocorrencia> lista, Map<Long, Vistoria> atendimentos) {
        PainelTerminal.ocorrencias(lista, atendimentos);
        if (lista.isEmpty()) {
            throw ApiException.naoEncontrado("Nenhuma ocorrência disponível para esta ação.");
        }
        long id = Entrada.id("ID da ocorrência");
        if (lista.stream().noneMatch(ocorrencia -> ocorrencia.id() == id)) {
            throw ApiException.invalido("Escolha um ID da lista.");
        }
        return id;
    }

    private void gerenciarEstabelecimento(Usuario usuario) throws SQLException {
        var atual = usuarios.estabelecimento(usuario);
        PainelTerminal.estabelecimento(atual);
        if (!Entrada.confirmar("Editar os dados do estabelecimento?")) {
            return;
        }
        System.out.println("  Pressione Enter para manter o valor atual.");
        var dados = new UsuarioBO.Estabelecimento(
                editarCampo("Razão social", atual.razaoSocial()), editarCampo("Nome fantasia", atual.nomeFantasia()),
                editarCampo("Atividade econômica", atual.atividadeEconomica()), editarCampo("Endereço", atual.endereco()),
                editarCampo("Responsável", atual.responsavel()), editarCampo("Telefone", atual.telefone()),
                editarCampo("Características das instalações", atual.caracteristicasInstalacoes()));
        PainelTerminal.estabelecimento(usuarios.atualizarEstabelecimento(usuario, dados));
        System.out.println("  Dados do estabelecimento atualizados.");
    }

    private String editarCampo(String rotulo, String valorAtual) {
        String valor = Entrada.texto(rotulo + " [" + (valorAtual == null ? "—" : valorAtual) + "]");
        return valor.isBlank() ? valorAtual : valor;
    }

    private void mostrarGeoRadar() {
        Periodo periodo = lerPeriodo();
        var serie = epidemiologia.historico(periodo.municipio(), periodo.ano());
        PainelTerminal.titulo("GEORADAR | ALERTA MUNICIPAL");
        PainelTerminal.campo("Município (IBGE)", serie.municipio() == 3550308 ? "São Paulo (3550308)" : serie.municipio());
        PainelTerminal.campo("Ano da consulta", serie.ano());
        PainelTerminal.campo("Fonte", serie.fonte());
        if (serie.indicadores().isEmpty()) {
            System.out.println("  Nenhum indicador disponível no período.");
        } else {
            var semana = serie.indicadores().getLast();
            PainelTerminal.campo("Semana", semana.semanaEpidemiologica());
            PainelTerminal.campo("Casos notificados", semana.casosNotificados());
            PainelTerminal.campo("Casos estimados", semana.casosEstimados());
            PainelTerminal.campo("Incidência / 100 mil", semana.incidenciaPor100Mil());
            PainelTerminal.campo("Nível de alerta", alerta(semana.nivelDeAlerta()));
        }
        PainelTerminal.campo("Mapa interativo atual", "https://info.dengue.mat.br/alerta/" + serie.municipio() + "/dengue");
    }

    private String alerta(Integer nivel) {
        if (nivel == null) {
            return "Sem informação";
        }
        return switch (nivel) {
            case 1 -> "Verde";
            case 2 -> "Amarelo";
            case 3 -> "Laranja";
            case 4 -> "Vermelho";
            default -> "Sem informação";
        };
    }

    private void mostrarGrafico() {
        Periodo periodo = lerPeriodo();
        var serie = epidemiologia.historico(periodo.municipio(), periodo.ano());
        PainelTerminal.titulo("EVOLUÇÃO DE CASOS NOTIFICADOS");
        PainelTerminal.campo("Município (IBGE)", serie.municipio());
        PainelTerminal.campo("Ano e fonte", serie.ano() + " · " + serie.fonte());
        var semanas = serie.indicadores();
        if (semanas.isEmpty()) {
            System.out.println("  Nenhum indicador disponível no período.");
            return;
        }
        var recentes = semanas.subList(Math.max(0, semanas.size() - 12), semanas.size());
        int maximo = recentes.stream().mapToInt(s -> s.casosNotificados() == null ? 0 : s.casosNotificados())
                .max().orElse(0);
        System.out.println("  Últimas semanas disponíveis · barras proporcionais ao maior número de casos");
        for (var semana : recentes) {
            Integer casos = semana.casosNotificados();
            if (casos == null) {
                System.out.printf("  SE %d | Sem dados%n", semana.semanaEpidemiologica());
            } else {
                int tamanho = casos <= 0 ? 0 : Math.max(1, (int) Math.round(casos * 28.0 / maximo));
                System.out.printf("  SE %d | %-28s %,d%n", semana.semanaEpidemiologica(), "█".repeat(tamanho), casos);
            }
        }
    }

    private void mostrarPrevisao() {
        Periodo periodo = lerPeriodo();
        var previsao = epidemiologia.prever(periodo.municipio(), periodo.ano());
        PainelTerminal.titulo("PREVISÃO EPIDEMIOLÓGICA");
        PainelTerminal.campo("Semana iniciada em", previsao.inicioDaSemana().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        PainelTerminal.campo("Casos estimados", previsao.casosEstimados());
        PainelTerminal.campo("Método", previsao.metodo());
        PainelTerminal.campo("Limitações", previsao.limitacoes());
    }

    private void conversarComLia() {
        String pergunta = Entrada.texto("Pergunta para a LIA (sem dados pessoais)");
        Periodo periodo = Entrada.confirmar("Incluir dados de um município na pergunta?")
                ? lerPeriodo() : new Periodo(null, null);
        PainelTerminal.titulo("LIA | ORIENTAÇÃO E INDICADORES");
        PainelTerminal.campo("Resposta", lia.conversar(new LiaBO.Pergunta(pergunta, periodo.municipio(), periodo.ano())).resposta());
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

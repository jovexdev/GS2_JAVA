package br.com.fiap.main;

import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.entities.Prioridade;
import br.com.fiap.entities.StatusVistoria;
import br.com.fiap.entities.Usuario;
import br.com.fiap.entities.Vistoria;
import br.com.fiap.excecoes.ApiException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

final class VistoriasTerminal {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private VistoriasTerminal() {
    }

    static void executar(VistoriaBO bo, Usuario usuario) throws SQLException {
        String[] opcoes = switch (usuario.perfil()) {
            case EMPRESA, CIDADAO -> new String[]{"Acompanhar atendimento", "Consultar histórico", "Voltar"};
            case GESTOR -> new String[]{"Consultar solicitações", "Revisar prioridade", "Designar agente",
                    "Agendar vistoria", "Acompanhar equipes", "Consultar resultados", "Consultar histórico",
                    "Encerrar solicitação", "Voltar"};
            case AGENTE -> new String[]{"Consultar vistorias atribuídas", "Consultar local da vistoria",
                    "Iniciar vistoria", "Registrar inspeção", "Registrar recomendações", "Concluir vistoria",
                    "Consultar histórico", "Voltar"};
        };
        while (true) {
            String acao = Entrada.escolher(switch (usuario.perfil()) {
                case GESTOR -> "CENTRO DE OPERAÇÕES";
                case AGENTE -> "ATENDIMENTOS DA EQUIPE";
                case CIDADAO, EMPRESA -> "MEUS ATENDIMENTOS";
            }, opcoes);
            if ("Voltar".equals(acao) || !Entrada.executar(() -> executarAcao(acao, bo, usuario))) {
                return;
            }
        }
    }

    private static void executarAcao(String acao, VistoriaBO bo, Usuario usuario) throws SQLException {
        switch (acao) {
            case "Consultar solicitações", "Consultar vistorias atribuídas" ->
                    PainelTerminal.vistorias("SOLICITAÇÕES E VISTORIAS", bo.listar(usuario));
            case "Acompanhar equipes" ->
                    PainelTerminal.vistorias("ATENDIMENTOS POR EQUIPE", bo.listar(usuario));
            case "Acompanhar atendimento" -> acompanhar(bo, usuario);
            case "Consultar local da vistoria" -> mostrarLocal(bo, selecionar(bo.listar(usuario)), usuario);
            case "Revisar prioridade" -> {
                long id = selecionar(comStatus(bo, usuario, StatusVistoria.PENDENTE));
                mostrarLocal(bo, id, usuario);
                var prioridade = switch (Entrada.escolher("Prioridade revisada", "Crítica", "Instável", "OK")) {
                    case "Crítica" -> Prioridade.CRITICA;
                    case "Instável" -> Prioridade.INSTAVEL;
                    default -> Prioridade.OK;
                };
                var dados = new VistoriaBO.Revisao(prioridade, Entrada.texto("Justificativa da decisão"));
                PainelTerminal.vistoria(bo.revisar(id, dados, usuario));
            }
            case "Designar agente" -> {
                long id = selecionar(comStatus(bo, usuario, StatusVistoria.PENDENTE));
                var agentes = bo.agentes(usuario);
                if (agentes.isEmpty()) {
                    throw ApiException.naoEncontrado("Nenhum agente cadastrado para designação.");
                }
                PainelTerminal.titulo("AGENTES CADASTRADOS");
                agentes.forEach(agente -> System.out.printf("  #%-6d %-22s | %s%n",
                        agente.id(), agente.nome(), agente.equipeNome()));
                PainelTerminal.vistoria(bo.designar(id, new VistoriaBO.Designacao(Entrada.id("ID do agente")), usuario));
            }
            case "Agendar vistoria" -> {
                long id = selecionar(comStatus(bo, usuario, StatusVistoria.PENDENTE));
                Vistoria vistoria = bo.consultar(id, usuario);
                if (vistoria.agenteId() == null) {
                    throw ApiException.conflito("Designe o agente antes de agendar.");
                }
                var horario = lerAgendamento();
                PainelTerminal.vistoria(bo.agendar(id, new VistoriaBO.Agendamento(vistoria.equipeId(), horario), usuario));
            }
            case "Consultar resultados" -> {
                var finalizadas = bo.listar(usuario).stream().filter(v ->
                        v.status() == StatusVistoria.CONCLUIDA || v.status() == StatusVistoria.ENCERRADA).toList();
                PainelTerminal.vistoria(bo.consultar(selecionar(finalizadas), usuario));
            }
            case "Iniciar vistoria" -> PainelTerminal.vistoria(
                    bo.iniciar(selecionar(comStatus(bo, usuario, StatusVistoria.AGENDADA)), usuario));
            case "Registrar inspeção" -> {
                long id = selecionar(comStatus(bo, usuario, StatusVistoria.EM_ANDAMENTO));
                var dados = new VistoriaBO.Registro(Entrada.texto("Condições observadas na inspeção"));
                PainelTerminal.vistoria(bo.registrarInspecao(id, dados, usuario));
            }
            case "Registrar recomendações" -> {
                long id = selecionar(comStatus(bo, usuario, StatusVistoria.EM_ANDAMENTO));
                var dados = new VistoriaBO.Registro(Entrada.texto("Orientações fornecidas"));
                PainelTerminal.vistoria(bo.registrarRecomendacoes(id, dados, usuario));
            }
            case "Concluir vistoria" -> {
                long id = selecionar(comStatus(bo, usuario, StatusVistoria.EM_ANDAMENTO));
                var dados = new VistoriaBO.Conclusao(Entrada.texto("Resumo final (Enter para usar a inspeção)"));
                PainelTerminal.vistoria(bo.concluir(id, dados, usuario));
            }
            case "Consultar histórico" -> historico(bo, usuario);
            case "Encerrar solicitação" -> {
                var encerraveis = bo.listar(usuario).stream().filter(v ->
                        v.status() == StatusVistoria.PENDENTE || v.status() == StatusVistoria.CONCLUIDA).toList();
                long id = selecionar(encerraveis);
                var dados = new VistoriaBO.Registro(Entrada.texto("Justificativa do encerramento"));
                PainelTerminal.vistoria(bo.encerrar(id, dados, usuario));
            }
        }
    }

    private static LocalDateTime lerAgendamento() {
        LocalDate data;
        try {
            data = LocalDate.parse(Entrada.texto("Data da vistoria (DD/MM/AAAA)"), FORMATO_DATA);
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("Data inválida. Informe uma data existente no formato DD/MM/AAAA.");
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(Entrada.texto("Hora da vistoria (HH:MM)"), FORMATO_HORA);
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("Hora inválida. Use HH:MM, entre 00:00 e 23:59.");
        }
        return LocalDateTime.of(data, hora);
    }

    static void acompanhar(VistoriaBO bo, Usuario usuario) throws SQLException {
        PainelTerminal.vistoria(bo.consultar(selecionar(bo.listar(usuario)), usuario));
    }

    static void historico(VistoriaBO bo, Usuario usuario) throws SQLException {
        long id = selecionar(bo.listar(usuario));
        PainelTerminal.historico(id, bo.historico(id, usuario));
    }

    private static void mostrarLocal(VistoriaBO bo, long id, Usuario usuario) throws SQLException {
        var local = bo.local(id, usuario);
        if (local.estabelecimento() != null) {
            PainelTerminal.titulo("ESTABELECIMENTO");
            PainelTerminal.campo("Nome", local.estabelecimento());
            PainelTerminal.campo("Endereço", local.endereco());
        }
        PainelTerminal.ocorrencia(local.ocorrencia(), bo.consultar(id, usuario));
    }

    private static List<Vistoria> comStatus(VistoriaBO bo, Usuario usuario, StatusVistoria status) throws SQLException {
        return bo.listar(usuario).stream().filter(v -> v.status() == status).toList();
    }

    private static long selecionar(List<Vistoria> vistorias) {
        PainelTerminal.vistorias("ATENDIMENTOS DISPONÍVEIS", vistorias);
        if (vistorias.isEmpty()) {
            throw ApiException.naoEncontrado("Nenhum atendimento está disponível para esta ação.");
        }
        long id = Entrada.id("ID do atendimento");
        if (vistorias.stream().noneMatch(v -> v.id() == id)) {
            throw ApiException.invalido("Escolha um ID da lista.");
        }
        return id;
    }
}

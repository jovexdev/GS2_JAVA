package br.com.fiap.main;

import br.com.fiap.entities.DadosPerfil;
import br.com.fiap.entities.HistoricoVistoria;
import br.com.fiap.entities.Ocorrencia;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Prioridade;
import br.com.fiap.entities.StatusVistoria;
import br.com.fiap.entities.Vistoria;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

final class PainelTerminal {
    private static final String SEPARADOR = "─".repeat(66);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private PainelTerminal() {
    }

    static void titulo(String titulo) {
        System.out.printf("%n%s%n  %s%n%s%n", SEPARADOR, titulo, SEPARADOR);
    }

    static void campo(String rotulo, Object valor) {
        String texto = valor == null || valor.toString().isBlank() ? "—" : valor.toString();
        var linhas = texto.lines().toList();
        System.out.printf("  %-22s %s%n", rotulo + ":", linhas.getFirst());
        for (int i = 1; i < linhas.size(); i++) {
            System.out.printf("  %-22s %s%n", "", linhas.get(i));
        }
    }

    static void ocorrencias(List<Ocorrencia> ocorrencias, Map<Long, Vistoria> atendimentos) {
        titulo("OCORRÊNCIAS AMBIENTAIS");
        if (ocorrencias.isEmpty()) {
            System.out.println("  Nenhuma ocorrência encontrada.");
            return;
        }
        for (var ocorrencia : ocorrencias) {
            System.out.printf("  #%-5d | %-24s | %s%n", ocorrencia.id(),
                    andamento(atendimentos.get(ocorrencia.id())), resumo(ocorrencia.descricao()));
        }
    }

    static void ocorrencia(Ocorrencia ocorrencia, Vistoria atendimento) {
        titulo("OCORRÊNCIA #" + ocorrencia.id());
        campo("Relato", ocorrencia.descricao());
        campo("Local", ocorrencia.localizacao());
        campo("Registrada em", data(ocorrencia.criadaEm()));
        campo("Andamento", andamento(atendimento));
        if (ocorrencia.prioridadePreliminar() != null) {
            campo("Classificação", prioridade(ocorrencia.prioridadePreliminar()));
            campo("Justificativa", ocorrencia.justificativa());
        }
        if (atendimento != null) {
            campo("Atendimento", "#" + atendimento.id());
        }
    }

    static void vistorias(String titulo, List<Vistoria> vistorias) {
        titulo(titulo);
        if (vistorias.isEmpty()) {
            System.out.println("  Nenhum atendimento encontrado.");
            return;
        }
        System.out.printf("  %-7s %-7s %-15s %-20s %s%n", "ID", "Relato", "Situação", "Equipe", "Agendamento");
        for (var vistoria : vistorias) {
            System.out.printf("  #%-6d #%-6d %-15s %-20s %s%n", vistoria.id(), vistoria.ocorrenciaId(),
                    status(vistoria.status()), limitar(vistoria.equipeNome(), 20), data(vistoria.agendadaPara()));
        }
    }

    static void vistoria(Vistoria vistoria) {
        titulo("ATENDIMENTO #" + vistoria.id());
        campo("Ocorrência", "#" + vistoria.ocorrenciaId());
        campo("Situação", status(vistoria.status()));
        campo("Prioridade revisada", prioridade(vistoria.prioridade()));
        campo("Equipe", vistoria.equipeNome());
        if (vistoria.agenteNome() != null) {
            campo("Agente", vistoria.agenteNome());
        }
        campo("Agendamento", data(vistoria.agendadaPara()));
        if (vistoria.inspecao() != null) {
            campo("Inspeção", vistoria.inspecao());
        }
        if (vistoria.recomendacoes() != null) {
            campo("Recomendações", vistoria.recomendacoes());
        }
        if (vistoria.resultado() != null && !vistoria.resultado().equals(vistoria.inspecao())) {
            campo("Resultado", vistoria.resultado());
        }
    }

    static void historico(long id, List<HistoricoVistoria> historico) {
        titulo("HISTÓRICO DO ATENDIMENTO #" + id);
        for (var evento : historico) {
            System.out.printf("  %s  ·  %s%n", data(evento.registradoEm()), acao(evento.acao()));
            campo("Detalhe", evento.detalhe());
        }
    }

    static void estabelecimento(DadosPerfil.Empresa dados) {
        titulo("MEU ESTABELECIMENTO");
        campo("Razão social", dados.razaoSocial());
        campo("Nome fantasia", dados.nomeFantasia());
        campo("CNPJ", dados.cnpj());
        campo("Atividade", dados.atividadeEconomica());
        campo("Endereço", dados.endereco());
        campo("Responsável", dados.responsavel());
        campo("Telefone", dados.telefone());
        campo("Instalações", dados.caracteristicasInstalacoes());
    }

    static String perfil(Perfil perfil) {
        return switch (perfil) {
            case CIDADAO -> "Cidadão";
            case EMPRESA -> "Representante empresarial";
            case GESTOR -> "Gestor de vistorias";
            case AGENTE -> "Agente de vistoria";
        };
    }

    private static String andamento(Vistoria vistoria) {
        return vistoria == null ? "Aguardando análise" : status(vistoria.status());
    }

    private static String status(StatusVistoria status) {
        return switch (status) {
            case PENDENTE -> "Pendente";
            case AGENDADA -> "Agendada";
            case EM_ANDAMENTO -> "Em andamento";
            case CONCLUIDA -> "Concluída";
            case ENCERRADA -> "Encerrada";
        };
    }

    private static String prioridade(Prioridade prioridade) {
        if (prioridade == null) {
            return "A definir";
        }
        return switch (prioridade) {
            case CRITICA -> "Crítica";
            case INSTAVEL -> "Instável";
            case OK -> "OK";
        };
    }

    private static String acao(String acao) {
        return switch (acao) {
            case "SOLICITADA" -> "Solicitação recebida";
            case "ENCAMINHADA" -> "Ocorrência encaminhada";
            case "REVISADA" -> "Prioridade revisada";
            case "DESIGNADA" -> "Agente designado";
            case "AGENDADA" -> "Vistoria agendada";
            case "INICIADA" -> "Vistoria iniciada";
            case "INSPECAO" -> "Inspeção registrada";
            case "RECOMENDACOES" -> "Recomendações registradas";
            case "CONCLUIDA" -> "Atendimento de campo concluído";
            case "ENCERRADA" -> "Encerramento administrativo";
            default -> acao;
        };
    }

    private static String data(LocalDateTime data) {
        return data == null ? "—" : DATA_HORA.format(data);
    }

    private static String resumo(String texto) {
        return limitar(texto.replaceAll("\\s+", " "), 54);
    }

    private static String limitar(String texto, int limite) {
        if (texto == null) {
            return "—";
        }
        return texto.length() <= limite ? texto : texto.substring(0, limite - 1) + "…";
    }
}

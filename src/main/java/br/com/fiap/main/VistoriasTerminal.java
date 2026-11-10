package br.com.fiap.main;

import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.entities.Prioridade;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;

import java.sql.SQLException;
import java.time.LocalDateTime;

final class VistoriasTerminal {
    private VistoriasTerminal() {
    }

    static void executar(VistoriaBO bo, Usuario usuario) throws SQLException {
        String[] opcoes = switch (usuario.perfil()) {
            case EMPRESA -> new String[]{"Solicitar", "Listar", "Histórico", "Voltar"};
            case GESTOR -> new String[]{"Listar", "Consultar ocorrência", "Revisar", "Agendar", "Histórico", "Voltar"};
            case AGENTE -> new String[]{"Listar", "Consultar ocorrência", "Concluir", "Histórico", "Voltar"};
            case CIDADAO -> throw ApiException.proibido("Use o menu de ocorrências para consultar seus registros.");
        };
        String acao = Entrada.escolher("Vistorias", opcoes);

        switch (acao) {
            case "Solicitar" -> System.out.println(bo.solicitar(Entrada.id("ID da ocorrência"), usuario));
            case "Listar" -> {
                var lista = bo.listar(usuario);
                System.out.println("Vistorias encontradas: " + lista.size());
                lista.forEach(System.out::println);
            }
            case "Consultar ocorrência" ->
                    System.out.println(bo.ocorrencia(Entrada.id("ID da vistoria"), usuario));
            case "Revisar" -> {
                long id = Entrada.id("ID da vistoria");
                var prioridade = Prioridade.valueOf(Entrada.escolher("Prioridade", "CRITICA", "INSTAVEL", "OK"));
                var dados = new VistoriaBO.Revisao(prioridade, Entrada.texto("Justificativa"));
                System.out.println(bo.revisar(id, dados, usuario));
            }
            case "Agendar" -> {
                long id = Entrada.id("ID da vistoria");
                bo.equipes(usuario).forEach(System.out::println);
                long equipe = Entrada.id("ID da equipe piloto (1)");
                var horario = LocalDateTime.parse(Entrada.texto("Data futura no formato AAAA-MM-DDTHH:MM"));
                System.out.println(bo.agendar(id, new VistoriaBO.Agendamento(equipe, horario), usuario));
            }
            case "Concluir" -> {
                long id = Entrada.id("ID da vistoria");
                var dados = new VistoriaBO.Conclusao(Entrada.texto("Resultado da vistoria"));
                System.out.println(bo.concluir(id, dados, usuario));
            }
            case "Histórico" -> bo.historico(Entrada.id("ID da vistoria"), usuario).forEach(System.out::println);
        }
    }
}

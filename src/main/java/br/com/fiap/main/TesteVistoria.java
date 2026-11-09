package br.com.fiap.main;

import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.conexoes.ConexaoFactory;
import br.com.fiap.dao.VistoriaDAO;
import br.com.fiap.entities.Prioridade;

import java.time.LocalDateTime;

public class TesteVistoria {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var usuario = Entrada.login();
            var bo = new VistoriaBO(new VistoriaDAO(new ConexaoFactory()), Entrada.ocorrencias());
            String acao = Entrada.escolher("O que você quer testar?", "Solicitar", "Listar", "Revisar",
                    "Agendar", "Concluir", "Histórico");

            switch (acao) {
                case "Solicitar" -> System.out.println(bo.solicitar(Entrada.id("ID da ocorrência"), usuario));
                case "Listar" -> bo.listar(usuario).forEach(System.out::println);
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
        });
    }
}

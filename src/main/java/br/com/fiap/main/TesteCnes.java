package br.com.fiap.main;

import br.com.fiap.bo.UnidadesSaudeBO;
import br.com.fiap.services.CnesService;

public class TesteCnes {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            String codigo = Entrada.texto("Código IBGE (deixe vazio para Campinas)");
            int municipio = codigo.isBlank() ? 3509502 : Integer.parseInt(codigo);
            var consulta = new UnidadesSaudeBO(new CnesService()).buscar(municipio);
            System.out.println("\nUPAs E PRONTO ATENDIMENTO SUS");
            System.out.println("Fonte: " + consulta.fonte());
            System.out.println("Unidades encontradas: " + consulta.unidades().size());
            for (var unidade : consulta.unidades()) {
                System.out.println("\n" + unidade.nome() + " | " + unidade.tipo());
                System.out.println("Endereço: " + unidade.endereco() + " — " + unidade.bairro());
                System.out.println("Telefone: " + (unidade.telefone() == null ? "Não informado" : unidade.telefone()));
                System.out.println("Horário cadastrado: " + unidade.turnoCadastrado());
            }
            System.out.println("\nO cadastro não informa disponibilidade ou espera em tempo real.");
        });
    }
}

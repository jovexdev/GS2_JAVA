package br.com.fiap.main;

import br.com.fiap.bo.EpidemiologiaBO;
import br.com.fiap.services.InfoDengueService;

public class TesteInfoDengue {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            String municipio = Entrada.texto("Código IBGE (deixe vazio para São Paulo)");
            String ano = Entrada.texto("Ano (deixe vazio para consultar o último ano completo)");
            var bo = new EpidemiologiaBO(new InfoDengueService());
            var serie = bo.historico(municipio.isBlank() ? null : Integer.valueOf(municipio),
                    ano.isBlank() ? null : Integer.valueOf(ano));
            System.out.println("Fonte: " + serie.fonte() + " | Município: " + serie.municipio());
            System.out.println("Semanas encontradas: " + serie.indicadores().size());
            serie.indicadores().forEach(System.out::println);
        });
    }
}

package br.com.fiap.main;

import br.com.fiap.bo.OcorrenciaBO;
import br.com.fiap.entities.Perfil;

import java.util.ArrayList;

public class TesteArrayListOcorrencia {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var ocorrencias = new ArrayList<OcorrenciaBO.Relato>();
            do {
                ocorrencias.add(Entrada.relato(Perfil.CIDADAO));
            } while (Entrada.confirmar("Adicionar outro relato à lista de teste?"));

            for (var ocorrencia : ocorrencias) {
                System.out.println(ocorrencia.descricao() + " — " + ocorrencia.localizacao());
            }
        });
    }
}

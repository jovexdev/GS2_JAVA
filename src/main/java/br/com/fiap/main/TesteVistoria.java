package br.com.fiap.main;

import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.dao.VistoriaDAO;

public class TesteVistoria {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var usuario = Entrada.login();
            var bo = new VistoriaBO(new VistoriaDAO(Entrada.conexoes()), Entrada.ocorrencias());
            VistoriasTerminal.executar(bo, usuario);
        });
    }
}

package br.com.fiap.bo;

import br.com.fiap.excecoes.ApiException;

final class Validacao {
    private Validacao() {
    }

    static String obrigatorio(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {
            throw ApiException.invalido(campo + " é obrigatório e deve ter até " + maximo + " caracteres.");
        }
        return valor.strip();
    }
}

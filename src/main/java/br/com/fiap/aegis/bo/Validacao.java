package br.com.fiap.aegis.bo;

import br.com.fiap.aegis.excecoes.ApiException;

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

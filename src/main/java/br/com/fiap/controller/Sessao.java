package br.com.fiap.controller;

import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;
import jakarta.servlet.http.HttpServletRequest;

final class Sessao {
    private static final String USUARIO = "usuarioAutenticado";

    private Sessao() {
    }

    static void iniciar(HttpServletRequest request, Usuario usuario) {
        encerrar(request);
        request.getSession(true).setAttribute(USUARIO, usuario);
    }

    static void atualizar(HttpServletRequest request, Usuario usuario) {
        var sessao = request.getSession(false);
        if (sessao != null) {
            sessao.setAttribute(USUARIO, usuario);
        }
    }

    static Usuario exigirUsuario(HttpServletRequest request) {
        var sessao = request.getSession(false);
        if (sessao != null && sessao.getAttribute(USUARIO) instanceof Usuario usuario) {
            return usuario;
        }
        throw ApiException.naoAutenticado("Faça login para continuar.");
    }

    static void encerrar(HttpServletRequest request) {
        var sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
    }
}

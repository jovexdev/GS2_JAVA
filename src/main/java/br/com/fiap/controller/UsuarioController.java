package br.com.fiap.controller;

import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.entities.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@RequestMapping("/api")
public class UsuarioController {
    private final UsuarioBO bo;

    public UsuarioController(UsuarioBO bo) {
        this.bo = bo;
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario cadastrar(@RequestBody UsuarioBO.Cadastro cadastro) throws SQLException {
        return bo.cadastrar(cadastro);
    }

    @PostMapping("/sessao")
    public Usuario entrar(@RequestBody UsuarioBO.Login login, HttpServletRequest request) throws SQLException {
        Usuario usuario = bo.autenticar(login);
        Sessao.iniciar(request, usuario);
        return usuario;
    }

    @GetMapping("/sessao")
    public Usuario consultar(HttpServletRequest request) {
        return Sessao.exigirUsuario(request);
    }

    @DeleteMapping("/sessao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(HttpServletRequest request) {
        Sessao.encerrar(request);
    }
}

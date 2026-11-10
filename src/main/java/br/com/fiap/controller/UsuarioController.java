package br.com.fiap.controller;

import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.entities.DadosPerfil;
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

    @GetMapping("/empresa/estabelecimento")
    public DadosPerfil.Empresa estabelecimento(HttpServletRequest request) throws SQLException {
        return bo.estabelecimento(Sessao.exigirUsuario(request));
    }

    @PutMapping("/empresa/estabelecimento")
    public DadosPerfil.Empresa atualizarEstabelecimento(@RequestBody UsuarioBO.Estabelecimento dados,
                                                         HttpServletRequest request) throws SQLException {
        Usuario usuario = Sessao.exigirUsuario(request);
        DadosPerfil.Empresa atualizada = bo.atualizarEstabelecimento(usuario, dados);
        Sessao.atualizar(request, new Usuario(usuario.id(), usuario.perfil(), atualizada.razaoSocial(),
                usuario.documento(), usuario.email(), atualizada.endereco(), atualizada.responsavel(), usuario.equipeId()));
        return atualizada;
    }

    @DeleteMapping("/sessao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(HttpServletRequest request) {
        Sessao.encerrar(request);
    }
}

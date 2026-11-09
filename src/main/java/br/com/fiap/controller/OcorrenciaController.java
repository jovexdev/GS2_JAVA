package br.com.fiap.controller;

import br.com.fiap.bo.OcorrenciaBO;
import br.com.fiap.entities.Ocorrencia;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {
    private final OcorrenciaBO bo;

    public OcorrenciaController(OcorrenciaBO bo) {
        this.bo = bo;
    }

    @GetMapping
    public List<Ocorrencia> listar(HttpServletRequest request) throws SQLException {
        return bo.listar(Sessao.exigirUsuario(request));
    }

    @GetMapping("/{id}")
    public Ocorrencia consultar(@PathVariable long id, HttpServletRequest request) throws SQLException {
        return bo.consultar(id, Sessao.exigirUsuario(request));
    }

    @PostMapping
    public ResponseEntity<Ocorrencia> registrar(@RequestBody OcorrenciaBO.Relato relato,
                                                 HttpServletRequest request) throws SQLException {
        Ocorrencia criada = bo.registrar(relato, Sessao.exigirUsuario(request));
        return ResponseEntity.created(URI.create("/api/ocorrencias/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public Ocorrencia atualizar(@PathVariable long id, @RequestBody OcorrenciaBO.Relato relato,
                                HttpServletRequest request) throws SQLException {
        return bo.atualizar(id, relato, Sessao.exigirUsuario(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable long id, HttpServletRequest request) throws SQLException {
        bo.excluir(id, Sessao.exigirUsuario(request));
    }
}

package br.com.fiap.controller;

import br.com.fiap.bo.OrientacaoCidadaoBO;
import br.com.fiap.bo.UnidadesSaudeBO;
import br.com.fiap.entities.Perfil;
import br.com.fiap.excecoes.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cidadao")
public class CuidadoCidadaoController {
    private final UnidadesSaudeBO unidades;
    private final OrientacaoCidadaoBO orientacao;
    public CuidadoCidadaoController(UnidadesSaudeBO unidades, OrientacaoCidadaoBO orientacao) {
        this.unidades = unidades; this.orientacao = orientacao;
    }
    @GetMapping("/unidades-saude")
    public UnidadesSaudeBO.Consulta unidades(@RequestParam int municipio, HttpServletRequest request) {
        exigirCidadao(request);
        return unidades.buscar(municipio);
    }
    @PostMapping("/orientacao")
    public ResponseEntity<OrientacaoCidadaoBO.Orientacao> orientar(@RequestBody OrientacaoCidadaoBO.Questionario dados,
                                                                HttpServletRequest request) {
        exigirCidadao(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orientacao.orientar(dados));
    }
    private void exigirCidadao(HttpServletRequest request) {
        if (Sessao.exigirUsuario(request).perfil() != Perfil.CIDADAO)
            throw ApiException.proibido("Esta área é exclusiva do cidadão.");
    }
}

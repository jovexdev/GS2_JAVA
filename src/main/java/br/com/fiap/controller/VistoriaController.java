package br.com.fiap.controller;

import br.com.fiap.bo.VistoriaBO;
import br.com.fiap.entities.AgenteVistoria;
import br.com.fiap.entities.Equipe;
import br.com.fiap.entities.HistoricoVistoria;
import br.com.fiap.entities.Ocorrencia;
import br.com.fiap.entities.Vistoria;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VistoriaController {
    private final VistoriaBO bo;

    public VistoriaController(VistoriaBO bo) {
        this.bo = bo;
    }

    @PostMapping("/ocorrencias/{id}/vistoria")
    public ResponseEntity<Vistoria> solicitar(@PathVariable long id, HttpServletRequest request) throws SQLException {
        Vistoria vistoria = bo.solicitar(id, Sessao.exigirUsuario(request));
        return ResponseEntity.created(URI.create("/api/vistorias/" + vistoria.id())).body(vistoria);
    }

    @GetMapping("/vistorias")
    public List<Vistoria> listar(HttpServletRequest request) throws SQLException {
        return bo.listar(Sessao.exigirUsuario(request));
    }

    @GetMapping("/vistorias/{id}")
    public Vistoria consultar(@PathVariable long id, HttpServletRequest request) throws SQLException {
        return bo.consultar(id, Sessao.exigirUsuario(request));
    }

    @GetMapping("/vistorias/{id}/historico")
    public List<HistoricoVistoria> historico(@PathVariable long id, HttpServletRequest request) throws SQLException {
        return bo.historico(id, Sessao.exigirUsuario(request));
    }

    @GetMapping("/vistorias/{id}/ocorrencia")
    public Ocorrencia ocorrencia(@PathVariable long id, HttpServletRequest request) throws SQLException {
        return bo.ocorrencia(id, Sessao.exigirUsuario(request));
    }

    @GetMapping("/equipes")
    public List<Equipe> equipes(HttpServletRequest request) throws SQLException {
        return bo.equipes(Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/prioridade")
    public Vistoria revisar(@PathVariable long id, @RequestBody VistoriaBO.Revisao revisao,
                            HttpServletRequest request) throws SQLException {
        return bo.revisar(id, revisao, Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/agendamento")
    public Vistoria agendar(@PathVariable long id, @RequestBody VistoriaBO.Agendamento dados,
                            HttpServletRequest request) throws SQLException {
        return bo.agendar(id, dados, Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/conclusao")
    public Vistoria concluir(@PathVariable long id, @RequestBody VistoriaBO.Conclusao dados,
                             HttpServletRequest request) throws SQLException {
        return bo.concluir(id, dados, Sessao.exigirUsuario(request));
    }

    @PostMapping("/ocorrencias/{id}/analise")
    public ResponseEntity<Vistoria> analisar(@PathVariable long id, @RequestBody VistoriaBO.DecisaoOcorrencia dados,
                                            HttpServletRequest request) throws SQLException {
        Vistoria vistoria = bo.analisarOcorrencia(id, dados, Sessao.exigirUsuario(request));
        return ResponseEntity.created(URI.create("/api/vistorias/" + vistoria.id())).body(vistoria);
    }

    @GetMapping("/agentes")
    public List<AgenteVistoria> agentes(HttpServletRequest request) throws SQLException {
        return bo.agentes(Sessao.exigirUsuario(request));
    }

    @GetMapping("/vistorias/{id}/local")
    public VistoriaBO.LocalAtendimento local(@PathVariable long id, HttpServletRequest request) throws SQLException {
        return bo.local(id, Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/designacao")
    public Vistoria designar(@PathVariable long id, @RequestBody VistoriaBO.Designacao dados,
                             HttpServletRequest request) throws SQLException {
        return bo.designar(id, dados, Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/inicio")
    public Vistoria iniciar(@PathVariable long id, HttpServletRequest request) throws SQLException {
        return bo.iniciar(id, Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/inspecao")
    public Vistoria inspecao(@PathVariable long id, @RequestBody VistoriaBO.Registro dados,
                             HttpServletRequest request) throws SQLException {
        return bo.registrarInspecao(id, dados, Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/recomendacoes")
    public Vistoria recomendacoes(@PathVariable long id, @RequestBody VistoriaBO.Registro dados,
                                  HttpServletRequest request) throws SQLException {
        return bo.registrarRecomendacoes(id, dados, Sessao.exigirUsuario(request));
    }

    @PutMapping("/vistorias/{id}/encerramento")
    public Vistoria encerrar(@PathVariable long id, @RequestBody VistoriaBO.Registro dados,
                             HttpServletRequest request) throws SQLException {
        return bo.encerrar(id, dados, Sessao.exigirUsuario(request));
    }
}

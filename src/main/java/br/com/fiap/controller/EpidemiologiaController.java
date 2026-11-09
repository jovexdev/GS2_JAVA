package br.com.fiap.controller;

import br.com.fiap.bo.EpidemiologiaBO;
import br.com.fiap.bo.LiaBO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EpidemiologiaController {
    private final EpidemiologiaBO epidemiologia;
    private final LiaBO lia;

    public EpidemiologiaController(EpidemiologiaBO epidemiologia, LiaBO lia) {
        this.epidemiologia = epidemiologia;
        this.lia = lia;
    }

    @GetMapping("/epidemiologia/historico")
    public EpidemiologiaBO.Serie historico(@RequestParam(required = false) Integer municipio,
                                            @RequestParam(required = false) Integer ano) {
        return epidemiologia.historico(municipio, ano);
    }

    @GetMapping("/epidemiologia/previsao")
    public EpidemiologiaBO.Previsao previsao(@RequestParam(required = false) Integer municipio,
                                             @RequestParam(required = false) Integer ano) {
        return epidemiologia.prever(municipio, ano);
    }

    @PostMapping("/lia")
    public LiaBO.Resposta conversar(@RequestBody LiaBO.Pergunta pergunta, HttpServletRequest request) {
        Sessao.exigirUsuario(request);
        return lia.conversar(pergunta);
    }
}

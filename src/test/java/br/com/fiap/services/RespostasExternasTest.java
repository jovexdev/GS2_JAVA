package br.com.fiap.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RespostasExternasTest {
    @Test
    void leIndicadoresOrdenadosComDatasIsoOuEpoch() {
        var resultado = new InfoDengueService().lerIndicadores("""
                [
                  {"SE":201702,"data_iniSE":"2017-01-08","casos":7,"Localidade_id":0},
                  {"SE":201701,"data_iniSE":1483228800000,"casos":5,"casos_est":6.2,"nivel":2},
                  {"SE":201701,"data_iniSE":1483228800000,"casos":2,"Localidade_id":3}
                ]
                """);
        assertThat(resultado).hasSize(2);
        assertThat(resultado.getFirst().inicioDaSemana()).isEqualTo(LocalDate.of(2017, 1, 1));
        assertThat(resultado.getFirst().casosEstimados()).isEqualTo(6.2);
        assertThat(resultado.getLast().casosNotificados()).isEqualTo(7);
    }

    @Test
    void agregaTextosDaOpenAiMesmoQuandoHaItemDeRaciocinio() {
        String resposta = new OpenAiService("", "").lerResposta("""
                {"status":"completed","output":[
                  {"type":"reasoning"},
                  {"type":"message","content":[{"type":"output_text","text":"Primeiro."}]},
                  {"type":"message","content":[{"type":"output_text","text":"Segundo."}]}
                ]}
                """);
        assertThat(resposta).isEqualTo("Primeiro.\nSegundo.");
    }
}

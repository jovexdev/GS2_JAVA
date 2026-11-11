package br.com.fiap.bo;

import br.com.fiap.api.RespostaCnes;
import br.com.fiap.excecoes.ApiException;
import br.com.fiap.services.CnesService;
import br.com.fiap.services.OpenAiService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CuidadoCidadaoTest {
    @Test
    void converteCodigoFiltraNaoSusEDesabilitadasEPreservaUnidadeSemCoordenadas() {
        var servico = mock(CnesService.class);
        String json = """
                {"estabelecimentos":[
                {"codigo_cnes":1,"nome_fantasia":"UPA DEMO","atende":true,"estabelecimento_faz_atendimento_ambulatorial_sus":"SIM","latitude_estabelecimento_decimo_grau":-23.5,"longitude_estabelecimento_decimo_grau":-46.6},
                {"codigo_cnes":2,"nome_fantasia":"PRIVADO","estabelecimento_faz_atendimento_ambulatorial_sus":"NAO"},
                {"codigo_cnes":3,"nome_fantasia":"DESABILITADA","estabelecimento_faz_atendimento_ambulatorial_sus":"SIM","codigo_motivo_desabilitacao_estabelecimento":"10"},
                {"codigo_cnes":4,"nome_fantasia":"PA SEM COORDENADAS","estabelecimento_faz_atendimento_ambulatorial_sus":"SIM","latitude_estabelecimento_decimo_grau":0,"longitude_estabelecimento_decimo_grau":0}
                ]}
                """;
        when(servico.buscar(355030)).thenReturn(new Gson().fromJson(json, RespostaCnes.class).estabelecimentos());
        var resultado = new UnidadesSaudeBO(servico).buscar(3550308);
        assertThat(resultado.unidades()).hasSize(2);
        assertThat(resultado.unidades()).anySatisfy(u -> { assertThat(u.tipo()).isEqualTo("UPA");assertThat(u.latitude()).isEqualTo(-23.5); });
        assertThat(resultado.unidades()).anySatisfy(u -> { assertThat(u.codigoCnes()).isEqualTo(4);assertThat(u.latitude()).isNull(); });
        assertThatThrownBy(() -> new UnidadesSaudeBO(servico).buscar(123)).isInstanceOf(ApiException.class);
        verify(servico).buscar(355030);
    }

    private OrientacaoCidadaoBO.Questionario relato(Boolean consentimento) {
        return new OrientacaoCidadaoBO.Questionario(OrientacaoCidadaoBO.Sinais.NAO,
                "Febre e dor no corpo.", "Começou ontem.", "Continuo indisposto.", consentimento);
    }
    @Test
    void sinaisDeAlarmeNaoDependemDaOpenAiNemDeConsentimentoParaMostrarUrgencia() {
        var ai = mock(OpenAiService.class);var bo = new OrientacaoCidadaoBO(ai);
        for (var sinal : new OrientacaoCidadaoBO.Sinais[]{OrientacaoCidadaoBO.Sinais.SIM, OrientacaoCidadaoBO.Sinais.NAO_SEI}) {
            var r = bo.orientar(new OrientacaoCidadaoBO.Questionario(sinal,null,null,null,false));
            assertThat(r.urgente()).isTrue();assertThat(r.iaUtilizada()).isFalse();
        }
        verifyNoInteractions(ai);
    }
    @Test
    void exigeConsentimentoAntesDeEnviarRelato() {
        var ai = mock(OpenAiService.class);
        assertThatThrownBy(() -> new OrientacaoCidadaoBO(ai).orientar(relato(false))).isInstanceOf(ApiException.class);
        verifyNoInteractions(ai);
    }
    @Test
    void saidaLimitadaUsaTrechosExatosENaoConfirmaDiagnostico() {
        var ai = mock(OpenAiService.class);
        when(ai.responder(anyString(),anyString())).thenReturn("{\"situacao\":\"SINTOMAS_COMPATIVEIS\",\"trechos\":[\"Febre e dor no corpo.\",\"Diagnóstico inventado\"]}");
        var r = new OrientacaoCidadaoBO(ai).orientar(relato(true));
        assertThat(r.urgente()).isFalse();assertThat(r.iaUtilizada()).isTrue();
        assertThat(r.trechosDoRelato()).containsExactly("Febre e dor no corpo.");
        assertThat(r.limite()).contains("não confirma nem descarta");
    }
    @Test
    void indisponibilidadeOuRespostaInvalidaMantemOrientacaoDeReferencia() {
        var ai=mock(OpenAiService.class);var bo=new OrientacaoCidadaoBO(ai);
        when(ai.responder(anyString(),anyString())).thenThrow(ApiException.falhaExterna("Falha"));
        assertThat(bo.orientar(relato(true)).iaUtilizada()).isFalse();
        doReturn("{\"situacao\":\"DENGUE_CONFIRMADA\"}").when(ai).responder(anyString(),anyString());
        var r=bo.orientar(relato(true));
        assertThat(r.iaUtilizada()).isFalse();assertThat(r.limite()).contains("não confirma nem descarta");
    }
    @Test
    void relatoLivrePodeElevarUrgenciaMasNaoReduzirAlarmeJaInformado() {
        var ai=mock(OpenAiService.class);
        when(ai.responder(anyString(),anyString())).thenReturn("{\"situacao\":\"ALARME_RELATADO\"}");
        assertThat(new OrientacaoCidadaoBO(ai).orientar(relato(true)).urgente()).isTrue();
    }
}

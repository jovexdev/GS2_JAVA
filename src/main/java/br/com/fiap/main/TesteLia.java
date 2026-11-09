package br.com.fiap.main;

import br.com.fiap.bo.EpidemiologiaBO;
import br.com.fiap.bo.LiaBO;
import br.com.fiap.config.ConfiguracaoLocal;
import br.com.fiap.services.InfoDengueService;
import br.com.fiap.services.OpenAiService;

public class TesteLia {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var configuracao = ConfiguracaoLocal.carregar();
            var openAi = new OpenAiService(configuracao.valor("aegis.openai.api-key", "OPENAI_API_KEY"),
                    configuracao.valor("aegis.openai.model", "OPENAI_MODEL"));
            var lia = new LiaBO(new EpidemiologiaBO(new InfoDengueService()), openAi);
            String pergunta = Entrada.texto("Pergunta para a LIA (sem dados pessoais)");
            System.out.println(lia.conversar(new LiaBO.Pergunta(pergunta, null, null)).resposta());
        });
    }
}

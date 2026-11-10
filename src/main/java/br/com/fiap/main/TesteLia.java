package br.com.fiap.main;

import br.com.fiap.bo.EpidemiologiaBO;
import br.com.fiap.bo.LiaBO;
import br.com.fiap.services.InfoDengueService;
import br.com.fiap.services.OpenAiService;

public class TesteLia {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var credenciais = new CredenciaisTerminal();
            var openAi = new OpenAiService(credenciais.chaveOpenAi(), credenciais.modeloOpenAi());
            var lia = new LiaBO(new EpidemiologiaBO(new InfoDengueService()), openAi);
            String pergunta = Entrada.texto("Pergunta para a LIA (sem dados pessoais)");
            System.out.println(lia.conversar(new LiaBO.Pergunta(pergunta, null, null)).resposta());
        });
    }
}

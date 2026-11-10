package br.com.fiap.services;

import br.com.fiap.api.RespostaOpenAi;
import br.com.fiap.config.ConfiguracaoLocal;
import br.com.fiap.excecoes.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OpenAiService {
    private static final String MODELO_PADRAO = "gpt-5.6-terra";
    private static final String URL = "https://api.openai.com/v1/responses";
    private static final RequestConfig LIMITES = RequestConfig.custom()
            .setConnectTimeout(5000).setSocketTimeout(45000).build();
    private final Gson gson = new Gson();
    private final String chave;
    private final String modelo;
    private final String origemChave;

    public OpenAiService(@Value("${aegis.openai.api-key}") String chave,
                         @Value("${aegis.openai.model}") String modelo) {
        String chaveJava = ConfiguracaoLocal.segredoJava("OPENAI_API_KEY");
        this.chave = chaveJava.isBlank() ? chave : chaveJava;
        this.origemChave = !chaveJava.isBlank() ? "CredenciaisLocais.java"
                : chave.isBlank() ? "chave não configurada" : "arquivo de configuração ou variável OPENAI_API_KEY";
        this.modelo = modelo.isBlank() ? MODELO_PADRAO : modelo;
    }

    public String origemChave() {
        return origemChave;
    }

    public String responder(String instrucoes, String entrada) {
        if (chave.isBlank()) {
            throw ApiException.indisponivel("Configure OPENAI_API_KEY em CredenciaisLocais.java.");
        }
        var requisicao = new HttpPost(URL);
        requisicao.setHeader("Authorization", "Bearer " + chave);
        String json = gson.toJson(Map.of("model", modelo, "instructions", instrucoes, "input", entrada,
                "max_output_tokens", 1000, "store", false));
        requisicao.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var cliente = HttpClientBuilder.create().disableRedirectHandling().setDefaultRequestConfig(LIMITES).build();
             var resposta = cliente.execute(requisicao)) {
            HttpEntity entidade = resposta.getEntity();
            if (resposta.getStatusLine().getStatusCode() != 200 || entidade == null) {
                throw ApiException.falhaExterna("A OpenAI não concluiu a solicitação. Confira a chave e o modelo.");
            }
            String texto = lerResposta(EntityUtils.toString(entidade, StandardCharsets.UTF_8));
            if (texto.isBlank()) {
                throw ApiException.falhaExterna("A OpenAI retornou uma resposta sem texto.");
            }
            return texto;
        } catch (IOException | JsonParseException e) {
            throw ApiException.falhaExterna("Falha ao ler a resposta da OpenAI.");
        }
    }

    String lerResposta(String json) {
        RespostaOpenAi resposta = gson.fromJson(json, RespostaOpenAi.class);
        if (resposta == null || !"completed".equals(resposta.status()) || resposta.itens() == null) {
            return "";
        }
        return resposta.itens().stream()
                .filter(item -> "message".equals(item.tipo()) && item.conteudo() != null)
                .flatMap(item -> item.conteudo().stream())
                .filter(parte -> "output_text".equals(parte.tipo()))
                .map(RespostaOpenAi.Parte::texto)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }
}

package br.com.fiap.services;

import br.com.fiap.excecoes.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    public OpenAiService(@Value("${aegis.openai.api-key}") String chave,
                         @Value("${aegis.openai.model}") String modelo) {
        this.chave = chave;
        this.modelo = modelo.isBlank() ? MODELO_PADRAO : modelo;
    }

    public String responder(String instrucoes, String entrada) {
        if (chave.isBlank()) {
            throw ApiException.indisponivel("Preencha aegis.openai.api-key em aegis.local.properties.");
        }
        var requisicao = new HttpPost(URL);
        requisicao.setHeader("Authorization", "Bearer " + chave);
        String json = gson.toJson(Map.of("model", modelo, "instructions", instrucoes, "input", entrada,
                "max_output_tokens", 1000, "store", false));
        requisicao.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var cliente = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(LIMITES).build();
             var resposta = cliente.execute(requisicao)) {
            if (resposta.getStatusLine().getStatusCode() != 200 || resposta.getEntity() == null) {
                throw ApiException.falhaExterna("A OpenAI não concluiu a solicitação. Confira a chave e o modelo.");
            }
            String texto = lerResposta(EntityUtils.toString(resposta.getEntity(), StandardCharsets.UTF_8));
            if (texto.isBlank()) {
                throw ApiException.falhaExterna("A OpenAI retornou uma resposta sem texto.");
            }
            return texto;
        } catch (IOException | JsonParseException e) {
            throw ApiException.falhaExterna("Falha ao ler a resposta da OpenAI.");
        }
    }

    String lerResposta(String json) {
        Resposta resposta = gson.fromJson(json, Resposta.class);
        if (resposta == null || !"completed".equals(resposta.status()) || resposta.itens() == null) {
            return "";
        }
        return resposta.itens().stream()
                .filter(item -> "message".equals(item.tipo()) && item.conteudo() != null)
                .flatMap(item -> item.conteudo().stream())
                .filter(parte -> "output_text".equals(parte.tipo()))
                .map(Parte::texto)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    private record Resposta(String status, @SerializedName("output") List<Item> itens) {
    }

    private record Item(@SerializedName("type") String tipo, @SerializedName("content") List<Parte> conteudo) {
    }

    private record Parte(@SerializedName("type") String tipo, @SerializedName("text") String texto) {
    }
}

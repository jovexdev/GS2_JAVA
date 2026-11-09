package br.com.fiap.main;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class ConsultaApiAula {
    private ConsultaApiAula() {
    }

    static <T> T buscar(String url, Class<T> tipo) throws IOException {
        var limites = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(15000).build();
        var requisicao = new HttpGet(url);
        requisicao.setHeader("Accept", "application/json");
        try (var cliente = HttpClients.custom().setDefaultRequestConfig(limites).build();
             var resposta = cliente.execute(requisicao)) {
            int status = resposta.getStatusLine().getStatusCode();
            if (status != 200 || resposta.getEntity() == null) {
                throw new IOException("A API de exemplo retornou HTTP " + status + ".");
            }
            T resultado = new Gson().fromJson(EntityUtils.toString(resposta.getEntity(), StandardCharsets.UTF_8), tipo);
            if (resultado == null) {
                throw new IOException("A API de exemplo retornou um JSON vazio.");
            }
            return resultado;
        } catch (JsonParseException e) {
            throw new IOException("A API de exemplo retornou um JSON inválido.", e);
        }
    }
}

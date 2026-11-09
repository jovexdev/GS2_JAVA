package br.com.fiap.services;

import br.com.fiap.entities.IndicadorEpidemiologico;
import br.com.fiap.excecoes.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class InfoDengueService {
    private static final String URL = "https://info.dengue.mat.br/api/alertcity";
    private static final RequestConfig LIMITES = RequestConfig.custom()
            .setConnectTimeout(5000).setSocketTimeout(15000).build();
    private final Gson gson = new Gson();

    public List<IndicadorEpidemiologico> buscar(int municipio, int ano) {
        String parametros = "?geocode=" + municipio + "&disease=dengue&format=json&ew_start=1&ew_end=53"
                + "&ey_start=" + ano + "&ey_end=" + ano;
        HttpGet requisicao = new HttpGet(URL + parametros);
        requisicao.setHeader("Accept", "application/json");
        try (var cliente = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(LIMITES).build();
             var resposta = cliente.execute(requisicao)) {
            if (resposta.getStatusLine().getStatusCode() != 200 || resposta.getEntity() == null) {
                throw ApiException.falhaExterna("O InfoDengue não retornou os indicadores solicitados.");
            }
            String json = EntityUtils.toString(resposta.getEntity(), StandardCharsets.UTF_8);
            return lerIndicadores(json);
        } catch (IOException | JsonParseException | DateTimeException | NumberFormatException e) {
            throw ApiException.falhaExterna("Falha ao ler a resposta do InfoDengue.");
        }
    }

    List<IndicadorEpidemiologico> lerIndicadores(String json) {
        Semana[] semanas = gson.fromJson(json, Semana[].class);
        if (semanas == null) {
            throw new JsonParseException("Resposta sem uma lista de semanas.");
        }

        return Arrays.stream(semanas)
                .filter(s -> s != null && s.semana() != null && s.inicio() != null)
                .filter(s -> s.localidade() == null || s.localidade() == 0)
                .map(s -> new IndicadorEpidemiologico(s.semana(), data(s.inicio()), s.casos(),
                        s.estimativa(), s.incidencia(), s.nivel()))
                .sorted(Comparator.comparing(IndicadorEpidemiologico::inicioDaSemana))
                .toList();
    }

    private LocalDate data(String valor) {
        if (valor.matches("[0-9]+")) {
            return Instant.ofEpochMilli(Long.parseLong(valor)).atOffset(ZoneOffset.UTC).toLocalDate();
        }
        return LocalDate.parse(valor.length() > 10 ? valor.substring(0, 10) : valor);
    }

    private record Semana(
            @SerializedName(value = "SE", alternate = "EW") Integer semana,
            @SerializedName(value = "data_iniSE", alternate = "data_ini_SE") String inicio,
            @SerializedName(value = "casos", alternate = "cases") Integer casos,
            @SerializedName("casos_est") Double estimativa,
            @SerializedName("p_inc100k") Double incidencia,
            @SerializedName(value = "nivel", alternate = "level") Integer nivel,
            @SerializedName("Localidade_id") Integer localidade) {
    }
}

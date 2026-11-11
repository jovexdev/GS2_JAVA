package br.com.fiap.services;

import br.com.fiap.api.EstabelecimentoCnes;
import br.com.fiap.api.RespostaCnes;
import br.com.fiap.excecoes.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CnesService {
    public static final String FONTE = "https://apidadosabertos.saude.gov.br/cnes/estabelecimentos";
    private static final int TAMANHO_PAGINA = 100;
    private static final RequestConfig LIMITES = RequestConfig.custom()
            .setConnectTimeout(5000).setConnectionRequestTimeout(5000).setSocketTimeout(15000).build();
    private final Gson gson = new Gson();
    private final String url;
    private final ConcurrentHashMap<Integer, Cache> cache = new ConcurrentHashMap<>();

    public CnesService() { this(FONTE); }
    CnesService(String url) { this.url = url; }

    public List<EstabelecimentoCnes> buscar(int municipioCnes) {
        Cache anterior = cache.get(municipioCnes);
        if (anterior != null && anterior.expira().isAfter(Instant.now())) return anterior.unidades();
        List<EstabelecimentoCnes> unidades = new ArrayList<>();
        try (var cliente = HttpClientBuilder.create().disableRedirectHandling().setDefaultRequestConfig(LIMITES).build()) {
            for (int pagina = 0; pagina < 10; pagina++) {
                var requisicao = new HttpGet(url + "?codigo_municipio=" + municipioCnes
                        + "&codigo_tipo_unidade=73&limit=" + TAMANHO_PAGINA + "&offset=" + pagina * TAMANHO_PAGINA);
                requisicao.setHeader("Accept", "application/json");
                try (var resposta = cliente.execute(requisicao)) {
                    if (resposta.getStatusLine().getStatusCode() != 200 || resposta.getEntity() == null)
                        throw ApiException.falhaExterna("O CNES não retornou as unidades. Tente novamente em instantes.");
                    List<EstabelecimentoCnes> lote = lerResposta(EntityUtils.toString(resposta.getEntity(), StandardCharsets.UTF_8));
                    if (lote.stream().anyMatch(e -> e == null || e.municipio() == null || e.municipio() != municipioCnes
                            || e.tipo() == null || e.tipo() != 73))
                        throw ApiException.falhaExterna("O CNES retornou unidades fora do município solicitado.");
                    unidades.addAll(lote);
                    if (lote.size() < TAMANHO_PAGINA) {
                        List<EstabelecimentoCnes> resultado = List.copyOf(unidades);
                        if (cache.size() >= 50) cache.clear();
                        cache.put(municipioCnes, new Cache(resultado, Instant.now().plusSeconds(1800)));
                        return resultado;
                    }
                }
            }
            throw ApiException.falhaExterna("A consulta ao CNES excedeu o limite de páginas. Tente novamente.");
        } catch (IOException | JsonParseException e) {
            throw ApiException.falhaExterna("Não foi possível consultar o CNES. Confira a conexão e tente novamente.");
        }
    }

    List<EstabelecimentoCnes> lerResposta(String json) {
        RespostaCnes resposta = gson.fromJson(json, RespostaCnes.class);
        if (resposta == null || resposta.estabelecimentos() == null)
            throw new JsonParseException("Resposta do CNES sem lista de estabelecimentos.");
        return resposta.estabelecimentos();
    }

    private record Cache(List<EstabelecimentoCnes> unidades, Instant expira) { }
}

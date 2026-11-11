package br.com.fiap.services;

import br.com.fiap.excecoes.ApiException;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class CnesServiceTest {
    private static final String UNIDADE = "{\"codigo_cnes\":2000001,\"nome_fantasia\":\"UPA DEMO\",\"codigo_municipio\":355030,\"codigo_tipo_unidade\":73}";
    @Test
    void consultaSemChavePaginaResultadosEReutilizaCache() throws Exception {
        var chamadas = new AtomicInteger();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/cnes", request -> {
            chamadas.incrementAndGet();
            String query = request.getRequestURI().getQuery();
            boolean parametros = query.contains("codigo_municipio=355030") && query.contains("codigo_tipo_unidade=73")
                    && query.contains("limit=100") && request.getRequestHeaders().getFirst("Authorization") == null;
            String corpo = "{\"estabelecimentos\":[" + (query.contains("offset=0")
                    ? String.join(",", java.util.Collections.nCopies(100, UNIDADE)) : UNIDADE) + "]}";
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(parametros ? 200 : 400, bytes.length);
            request.getResponseBody().write(bytes); request.close();
        });
        servidor.start();
        try {
            var servico = new CnesService("http://127.0.0.1:" + servidor.getAddress().getPort() + "/cnes");
            assertThat(servico.buscar(355030)).hasSize(101);
            assertThat(servico.buscar(355030)).hasSize(101);
            assertThat(chamadas).hasValue(2);
        } finally { servidor.stop(0); }
    }
    @Test
    void naoTransformaRespostaInvalidaEmListaVazia() {
        var servico = new CnesService();
        assertThatThrownBy(() -> servico.lerResposta("{}")).isInstanceOf(JsonParseException.class);
        assertThat(servico.lerResposta("{\"estabelecimentos\":[]}")).isEmpty();
    }
    @Test
    void rejeitaRespostaDeOutroMunicipio() throws Exception {
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        servidor.createContext("/cnes", request -> {
            byte[] bytes = ("{\"estabelecimentos\":[" + UNIDADE + "]}").getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(200,bytes.length);request.getResponseBody().write(bytes);request.close();
        });
        servidor.start();
        try {
            var servico = new CnesService("http://127.0.0.1:" + servidor.getAddress().getPort()+"/cnes");
            assertThatThrownBy(() -> servico.buscar(350950)).isInstanceOf(ApiException.class);
        } finally { servidor.stop(0); }
    }
}

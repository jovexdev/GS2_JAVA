package br.com.fiap.bo;

import br.com.fiap.api.EstabelecimentoCnes;
import br.com.fiap.entities.UnidadeSaude;
import br.com.fiap.excecoes.ApiException;
import br.com.fiap.services.CnesService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class UnidadesSaudeBO {
    private final CnesService cnes;
    public UnidadesSaudeBO(CnesService cnes) { this.cnes = cnes; }

    public Consulta buscar(int municipio) {
        if (municipio < 1_000_000 || municipio > 9_999_999)
            throw ApiException.invalido("Selecione um município válido.");
        // A API de localidades usa 7 dígitos; o CNES recebe os 6 primeiros, sem o dígito verificador.
        List<UnidadeSaude> unidades = cnes.buscar(municipio / 10).stream()
                .filter(e -> e.codigo() != null && e.nome() != null && !e.nome().isBlank())
                .filter(e -> "SIM".equalsIgnoreCase(e.atendeSus()))
                .filter(e -> e.desabilitacao() == null || e.desabilitacao().isBlank() || "0".equals(e.desabilitacao()))
                .map(this::mapear)
                .distinct()
                .sorted(Comparator.comparing(UnidadeSaude::nome))
                .toList();
        return new Consulta("CNES / Ministério da Saúde", CnesService.FONTE, Instant.now().toString(), unidades);
    }

    private UnidadeSaude mapear(EstabelecimentoCnes e) {
        boolean coordenadas = e.latitude() != null && e.longitude() != null
                && Double.isFinite(e.latitude()) && Double.isFinite(e.longitude())
                && Math.abs(e.latitude()) <= 90 && Math.abs(e.longitude()) <= 180
                && e.latitude() != 0 && e.longitude() != 0;
        String nome = e.nome().toUpperCase(Locale.ROOT);
        String tipo = nome.matches(".*\\bUPA\\b.*") || nome.contains("UNIDADE DE PRONTO ATENDIMENTO")
                ? "UPA" : "Pronto atendimento SUS";
        String endereco = e.logradouro() == null ? "" : e.logradouro().strip();
        if (e.numero() != null && !e.numero().isBlank()) endereco += ", " + e.numero().strip();
        return new UnidadeSaude(e.codigo(), e.nome(), tipo, endereco, e.bairro(), e.cep(), e.telefone(),
                coordenadas ? e.latitude() : null, coordenadas ? e.longitude() : null, e.turno(), e.atualizacao());
    }

    public record Consulta(String fonte, String urlFonte, String consultadoEm, List<UnidadeSaude> unidades) { }
}

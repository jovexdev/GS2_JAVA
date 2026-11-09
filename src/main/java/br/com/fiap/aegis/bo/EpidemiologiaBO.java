package br.com.fiap.aegis.bo;

import br.com.fiap.aegis.entities.IndicadorEpidemiologico;
import br.com.fiap.aegis.excecoes.ApiException;
import br.com.fiap.aegis.services.InfoDengueService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
public class EpidemiologiaBO {
    private static final int MUNICIPIO_PILOTO = 3550308;
    private static final int JANELA = 4;
    private final InfoDengueService infoDengue;

    public EpidemiologiaBO(InfoDengueService infoDengue) {
        this.infoDengue = infoDengue;
    }

    public Serie historico(Integer municipio, Integer ano) {
        int codigo = municipio == null ? MUNICIPIO_PILOTO : municipio;
        int anoAtual = Year.now().getValue();
        int anoConsulta = ano == null ? anoAtual - 1 : ano;
        if (codigo < 1_000_000 || codigo > 9_999_999 || anoConsulta < 2010 || anoConsulta > anoAtual) {
            throw ApiException.invalido("Informe um código IBGE de sete dígitos e um ano válido a partir de 2010.");
        }
        return new Serie(codigo, anoConsulta, "InfoDengue", infoDengue.buscar(codigo, anoConsulta));
    }

    public Previsao prever(Integer municipio, Integer ano) {
        Serie serie = historico(municipio, ano);
        List<IndicadorEpidemiologico> dados = serie.indicadores();
        if (dados.size() < JANELA) {
            throw ApiException.naoEncontrado("São necessárias quatro semanas de dados para estimar a tendência.");
        }
        var recentes = dados.subList(dados.size() - JANELA, dados.size());
        LocalDate inicio = recentes.getFirst().inicioDaSemana();
        for (int i = 0; i < JANELA; i++) {
            var semana = recentes.get(i);
            if (semana.casosNotificados() == null || !semana.inicioDaSemana().equals(inicio.plusWeeks(i))) {
                throw ApiException.naoEncontrado("A estimativa exige quatro semanas consecutivas com casos notificados.");
            }
        }
        long estimativa = Math.round(recentes.stream().mapToInt(IndicadorEpidemiologico::casosNotificados)
                .average().orElseThrow());
        return new Previsao(serie.municipio(), recentes.getLast().inicioDaSemana().plusWeeks(1), estimativa,
                "Média móvel das quatro últimas semanas do período selecionado",
                "Estimativa acadêmica a partir das notificações. Atrasos nos registros e sazonalidade afetam o resultado.");
    }

    public record Serie(int municipio, int ano, String fonte, List<IndicadorEpidemiologico> indicadores) {
    }

    public record Previsao(int municipio, LocalDate inicioDaSemana, long casosEstimados,
                           String metodo, String limitacoes) {
    }
}

package br.com.fiap.entities;

import java.time.LocalDateTime;

public record Vistoria(long id, long ocorrenciaId, long solicitanteId, Long equipeId, StatusVistoria status,
                       Prioridade prioridade, LocalDateTime agendadaPara, String resultado, int versao,
                       Long agenteId, String inspecao, String recomendacoes, String equipeNome, String agenteNome) {
    public Vistoria(long id, long ocorrenciaId, long solicitanteId, Long equipeId, StatusVistoria status,
                    Prioridade prioridade, LocalDateTime agendadaPara, String resultado, int versao) {
        this(id, ocorrenciaId, solicitanteId, equipeId, status, prioridade, agendadaPara, resultado, versao,
                null, null, null, null, null);
    }
}

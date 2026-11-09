package br.com.fiap.entities;

import java.time.LocalDateTime;

public record Vistoria(long id, long ocorrenciaId, long solicitanteId, Long equipeId, StatusVistoria status,
                       Prioridade prioridade, LocalDateTime agendadaPara, String resultado, int versao) {
}

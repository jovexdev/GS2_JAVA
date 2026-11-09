package br.com.fiap.aegis.entities;

import java.time.LocalDateTime;

public record Ocorrencia(long id, long autorId, String descricao, String localizacao, Boolean aguaParada,
                         Boolean reservatorioAberto, Boolean materiaisExpostos, Prioridade prioridadePreliminar,
                         String justificativa, LocalDateTime criadaEm) {
}

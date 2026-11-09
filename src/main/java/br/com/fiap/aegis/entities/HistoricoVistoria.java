package br.com.fiap.aegis.entities;

import java.time.LocalDateTime;

public record HistoricoVistoria(long id, long vistoriaId, long autorId, String acao, String detalhe,
                                LocalDateTime registradoEm) {
}

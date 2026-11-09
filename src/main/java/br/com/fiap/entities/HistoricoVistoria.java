package br.com.fiap.entities;

import java.time.LocalDateTime;

public record HistoricoVistoria(long id, long vistoriaId, long autorId, String acao, String detalhe,
                                LocalDateTime registradoEm) {
}

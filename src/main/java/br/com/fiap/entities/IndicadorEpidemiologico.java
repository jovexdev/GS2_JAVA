package br.com.fiap.entities;

import java.time.LocalDate;

public record IndicadorEpidemiologico(int semanaEpidemiologica, LocalDate inicioDaSemana,
                                     Integer casosNotificados, Double casosEstimados,
                                     Double incidenciaPor100Mil, Integer nivelDeAlerta) {
}

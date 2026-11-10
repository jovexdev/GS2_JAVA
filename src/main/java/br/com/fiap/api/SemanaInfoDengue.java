package br.com.fiap.api;

import com.google.gson.annotations.SerializedName;

public record SemanaInfoDengue(
        @SerializedName(value = "SE", alternate = "EW") Integer semana,
        @SerializedName(value = "data_iniSE", alternate = "data_ini_SE") String inicio,
        @SerializedName(value = "casos", alternate = "cases") Integer casos,
        @SerializedName("casos_est") Double estimativa,
        @SerializedName("p_inc100k") Double incidencia,
        @SerializedName(value = "nivel", alternate = "level") Integer nivel,
        @SerializedName("Localidade_id") Integer localidade) {
}

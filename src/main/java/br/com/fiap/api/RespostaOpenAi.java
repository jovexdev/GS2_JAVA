package br.com.fiap.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record RespostaOpenAi(String status, @SerializedName("output") List<Item> itens) {
    public record Item(@SerializedName("type") String tipo, @SerializedName("content") List<Parte> conteudo) {
    }

    public record Parte(@SerializedName("type") String tipo, @SerializedName("text") String texto) {
    }
}

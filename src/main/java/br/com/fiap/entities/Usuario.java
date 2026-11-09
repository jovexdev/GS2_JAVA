package br.com.fiap.entities;

public record Usuario(long id, Perfil perfil, String nome, String documento, String email,
                      String endereco, String responsavel, Long equipeId) {
}

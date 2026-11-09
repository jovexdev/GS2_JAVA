package br.com.fiap.aegis.entities;

public record Usuario(long id, Perfil perfil, String nome, String documento, String email,
                      String endereco, String responsavel, Long equipeId) {
}

package br.com.fiap.entities;

public record UnidadeSaude(long codigoCnes, String nome, String tipo, String endereco, String bairro,
                           String cep, String telefone, Double latitude, Double longitude,
                           String turnoCadastrado, String atualizadoEm) {
}

package br.com.fiap.api;

import com.google.gson.annotations.SerializedName;

// Campos públicos utilizados do CNES; dados pessoais de profissionais não são importados.
public record EstabelecimentoCnes(
        @SerializedName("codigo_cnes") Long codigo,
        @SerializedName("nome_fantasia") String nome,
        @SerializedName("codigo_tipo_unidade") Integer tipo,
        @SerializedName("codigo_municipio") Integer municipio,
        @SerializedName("endereco_estabelecimento") String logradouro,
        @SerializedName("numero_estabelecimento") String numero,
        @SerializedName("bairro_estabelecimento") String bairro,
        @SerializedName("codigo_cep_estabelecimento") String cep,
        @SerializedName("numero_telefone_estabelecimento") String telefone,
        @SerializedName("latitude_estabelecimento_decimo_grau") Double latitude,
        @SerializedName("longitude_estabelecimento_decimo_grau") Double longitude,
        @SerializedName("estabelecimento_faz_atendimento_ambulatorial_sus") String atendeSus,
        @SerializedName("codigo_motivo_desabilitacao_estabelecimento") String desabilitacao,
        @SerializedName("descricao_turno_atendimento") String turno,
        @SerializedName("data_atualizacao") String atualizacao) {
}

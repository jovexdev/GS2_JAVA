package br.com.fiap.entities;

public sealed interface DadosPerfil {
    default Perfil perfil() {
        return switch (this) {
            case Cidadao ignorado -> Perfil.CIDADAO;
            case Empresa ignorado -> Perfil.EMPRESA;
            case Gestor ignorado -> Perfil.GESTOR;
            case Agente ignorado -> Perfil.AGENTE;
        };
    }

    record Cidadao(String nome, String cpf, String endereco, String ocupacao, String telefone) implements DadosPerfil {
    }

    record Empresa(String razaoSocial, String nomeFantasia, String cnpj, String atividadeEconomica, String endereco,
                   String responsavel, String telefone, String caracteristicasInstalacoes) implements DadosPerfil {
    }

    record Gestor(String nome, String identificacaoFuncional, String setor) implements DadosPerfil {
    }

    record Agente(String nome, String identificacaoFuncional, long equipeId) implements DadosPerfil {
    }
}

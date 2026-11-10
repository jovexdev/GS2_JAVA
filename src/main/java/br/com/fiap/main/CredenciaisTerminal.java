package br.com.fiap.main;

import br.com.fiap.config.ConfiguracaoLocal;

import java.util.Map;

final class CredenciaisTerminal {
    private final ConfiguracaoLocal configuracao = ConfiguracaoLocal.carregar();

    Banco banco() {
        String url = configuracao.valor("aegis.db.url", "DB_URL");
        String usuario = configuracao.valor("aegis.db.usuario", "DB_USUARIO");
        String senha = configuracao.valor("aegis.db.senha", "DB_SENHA");
        if (usuario.isBlank()) {
            usuario = Entrada.texto("Usuário Oracle FIAP (Enter para pular o banco)");
        }
        if (!usuario.isBlank() && senha.isBlank()) {
            senha = Entrada.senha("Senha Oracle FIAP");
        }
        return new Banco(url, usuario, senha);
    }

    String chaveOpenAi() {
        String chave = configuracao.valor("aegis.openai.api-key", "OPENAI_API_KEY");
        return chave.isBlank() ? Entrada.senha("Chave OpenAI (Enter para pular a LIA)").strip() : chave;
    }

    String modeloOpenAi() {
        return configuracao.valor("aegis.openai.model", "OPENAI_MODEL");
    }

    Map<String, Object> propriedades() {
        Banco banco = banco();
        return Map.of("aegis.db.url", banco.url(), "aegis.db.usuario", banco.usuario(),
                "aegis.db.senha", banco.senha(), "aegis.openai.api-key", chaveOpenAi(),
                "aegis.openai.model", modeloOpenAi());
    }

    record Banco(String url, String usuario, String senha) {
    }
}

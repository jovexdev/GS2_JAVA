package br.com.fiap.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfiguracaoLocal {
    private final Properties propriedades;

    private ConfiguracaoLocal(Properties propriedades) {
        this.propriedades = propriedades;
    }

    public static ConfiguracaoLocal carregar() {
        Path arquivo = Path.of("aegis.local.properties");
        Properties propriedades = new Properties();
        if (Files.exists(arquivo)) {
            try (var leitor = Files.newBufferedReader(arquivo)) {
                propriedades.load(leitor);
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível ler aegis.local.properties.", e);
            }
        }
        return new ConfiguracaoLocal(propriedades);
    }

    public static String segredoJava(String campo) {
        try {
            Class<?> fonte = Class.forName("br.com.fiap.config.CredenciaisLocais");
            String valor = (String) fonte.getField(campo).get(null);
            return valor == null ? "" : valor;
        } catch (ClassNotFoundException e) {
            return "";
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("Confira o campo " + campo + " em CredenciaisLocais.java.", e);
        }
    }

    public String valor(String chave, String variavel) {
        String valor = propriedades.getProperty(chave, "");
        return valor.isBlank() ? System.getenv().getOrDefault(variavel, "") : valor;
    }
}

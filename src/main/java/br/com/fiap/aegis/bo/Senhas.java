package br.com.fiap.aegis.bo;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

final class Senhas {
    private static final int ITERACOES = 600_000;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senhas() {
    }

    static String gerarHash(String senha) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        var encoder = Base64.getEncoder();
        return encoder.encodeToString(sal) + ":" + encoder.encodeToString(derivar(senha, sal));
    }

    static boolean confere(String senha, String hash) {
        String[] partes = hash.split(":", 2);
        var decoder = Base64.getDecoder();
        return MessageDigest.isEqual(decoder.decode(partes[1]), derivar(senha, decoder.decode(partes[0])));
    }

    private static byte[] derivar(String senha, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, ITERACOES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível processar a senha.", e);
        } finally {
            spec.clearPassword();
        }
    }
}

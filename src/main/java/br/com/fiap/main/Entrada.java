package br.com.fiap.main;

import br.com.fiap.bo.OcorrenciaBO;
import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.conexoes.ConexaoFactory;
import br.com.fiap.dao.OcorrenciaDAO;
import br.com.fiap.dao.UsuarioDAO;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;

import java.io.Console;
import java.io.IOException;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CancellationException;

final class Entrada {
    private static final Console CONSOLE = System.console();
    private static final Scanner LEITOR = CONSOLE == null ? new Scanner(System.in) : null;

    private Entrada() {
    }

    static boolean executar(Teste teste) {
        try {
            teste.rodar();
        } catch (CancellationException e) {
            return false;
        } catch (SQLException e) {
            System.err.println("Falha Oracle (código " + e.getErrorCode() + "): " + e.getMessage());
            if (e.getErrorCode() == 942) {
                System.err.println("Se já existe um esquema AEGIS, execute database/02_migrar_perfis.sql no seu usuário Oracle.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Driver Oracle indisponível. Recarregue o projeto Maven no IntelliJ.");
        } catch (ApiException | IllegalArgumentException | DateTimeException | IOException e) {
            System.err.println(e.getMessage());
        }
        return true;
    }

    static String texto(String pergunta) {
        return linha(pergunta).strip();
    }

    static String senha(String pergunta) {
        if (CONSOLE == null) {
            return linha(pergunta + " (entrada visível neste console)");
        }
        char[] valor = CONSOLE.readPassword("%s: ", pergunta);
        if (valor == null) {
            throw new CancellationException();
        }
        try {
            return new String(valor);
        } finally {
            Arrays.fill(valor, '\0');
        }
    }

    private static String linha(String pergunta) {
        if (CONSOLE != null) {
            String valor = CONSOLE.readLine("%s: ", pergunta);
            if (valor == null) {
                throw new CancellationException();
            }
            return valor;
        }
        System.out.print(pergunta + ": ");
        System.out.flush();
        if (!LEITOR.hasNextLine()) {
            throw new CancellationException();
        }
        return LEITOR.nextLine();
    }

    static long id(String pergunta) {
        long valor = Long.parseLong(texto(pergunta));
        if (valor <= 0) {
            throw new IllegalArgumentException("Informe um número inteiro positivo.");
        }
        return valor;
    }

    static boolean confirmar(String pergunta) {
        return switch (texto(pergunta + " [s/n]").toLowerCase(java.util.Locale.ROOT)) {
            case "s", "sim" -> true;
            case "n", "nao", "não" -> false;
            default -> throw new IllegalArgumentException("Responda com s ou n.");
        };
    }

    static String escolher(String pergunta, String... opcoes) {
        System.out.println(pergunta);
        for (int i = 0; i < opcoes.length; i++) {
            System.out.println((i + 1) + " - " + opcoes[i]);
        }
        long opcao = id("Opção");
        if (opcao > opcoes.length) {
            throw new IllegalArgumentException("Escolha uma opção da lista.");
        }
        return opcoes[(int) opcao - 1];
    }

    static ConexaoFactory conexoes() {
        return new ConexaoFactory();
    }

    static UsuarioBO usuarios() {
        return new UsuarioBO(new UsuarioDAO(conexoes()));
    }

    static OcorrenciaBO ocorrencias() {
        return new OcorrenciaBO(new OcorrenciaDAO(conexoes()));
    }

    static Usuario login(UsuarioBO usuarios) throws SQLException {
        return usuarios.autenticar(dadosLogin("E-mail cadastrado no AEGIS", "Senha da sua conta no AEGIS"));
    }

    static UsuarioBO.Login dadosLogin(String perguntaEmail, String perguntaSenha) {
        return new UsuarioBO.Login(texto(perguntaEmail), senha(perguntaSenha));
    }

    static Usuario login() throws SQLException {
        return login(usuarios());
    }

    static OcorrenciaBO.Relato relato(Perfil perfil) {
        String descricao = texto("Descrição da ocorrência");
        String local = texto("Localização da ocorrência");
        if (perfil == Perfil.EMPRESA) {
            return new OcorrenciaBO.Relato(descricao, local, confirmar("Há água parada?"),
                    confirmar("Há reservatório aberto?"), confirmar("Há materiais expostos?"));
        }
        return new OcorrenciaBO.Relato(descricao, local, null, null, null);
    }

    @FunctionalInterface
    interface Teste {
        void rodar() throws SQLException, ClassNotFoundException, IOException;
    }
}

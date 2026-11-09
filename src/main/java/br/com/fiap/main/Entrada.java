package br.com.fiap.main;

import br.com.fiap.bo.OcorrenciaBO;
import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.conexoes.ConexaoFactory;
import br.com.fiap.dao.OcorrenciaDAO;
import br.com.fiap.dao.UsuarioDAO;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import java.io.IOException;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

final class Entrada {
    private Entrada() {
    }

    static void executar(Teste teste) {
        try {
            teste.rodar();
        } catch (CancellationException e) {
            System.out.println("Teste cancelado.");
        } catch (SQLException e) {
            System.err.println("Falha Oracle (código " + e.getErrorCode() + "): " + e.getMessage());
            if (e.getErrorCode() == 942) {
                System.err.println("Execute database/01_schema.sql no esquema do seu usuário Oracle.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Driver Oracle indisponível. Recarregue o projeto Maven no IntelliJ.");
        } catch (ApiException | IllegalArgumentException | DateTimeException | IOException e) {
            System.err.println(e.getMessage());
        }
    }

    static String texto(String pergunta) {
        String resposta = JOptionPane.showInputDialog(pergunta);
        if (resposta == null) {
            throw new CancellationException();
        }
        return resposta.strip();
    }

    static String senha(String pergunta) {
        JPasswordField campo = new JPasswordField(20);
        int opcao = JOptionPane.showConfirmDialog(null, new Object[]{pergunta, campo},
                "AEGIS", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) {
            throw new CancellationException();
        }
        char[] valor = campo.getPassword();
        try {
            return new String(valor);
        } finally {
            Arrays.fill(valor, '\0');
            campo.setText("");
        }
    }

    static long id(String pergunta) {
        long valor = Long.parseLong(texto(pergunta));
        if (valor <= 0) {
            throw new IllegalArgumentException("Informe um número inteiro positivo.");
        }
        return valor;
    }

    static boolean confirmar(String pergunta) {
        int opcao = JOptionPane.showConfirmDialog(null, pergunta, "AEGIS", JOptionPane.YES_NO_CANCEL_OPTION);
        if (opcao != JOptionPane.YES_OPTION && opcao != JOptionPane.NO_OPTION) {
            throw new CancellationException();
        }
        return opcao == JOptionPane.YES_OPTION;
    }

    static String escolher(String pergunta, String... opcoes) {
        var escolha = JOptionPane.showInputDialog(null, pergunta, "AEGIS", JOptionPane.QUESTION_MESSAGE,
                null, opcoes, opcoes[0]);
        if (escolha == null) {
            throw new CancellationException();
        }
        return escolha.toString();
    }

    static UsuarioBO usuarios() {
        return new UsuarioBO(new UsuarioDAO(new ConexaoFactory()));
    }

    static OcorrenciaBO ocorrencias() {
        return new OcorrenciaBO(new OcorrenciaDAO(new ConexaoFactory()));
    }

    static Usuario login() throws SQLException {
        String email = texto("E-mail cadastrado no AEGIS");
        String senha = senha("Senha da sua conta no AEGIS");
        return usuarios().autenticar(new UsuarioBO.Login(email, senha));
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

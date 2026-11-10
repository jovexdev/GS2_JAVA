package br.com.fiap.main;

import br.com.fiap.conexoes.ConexaoFactory;

import java.sql.SQLException;

final class BancoTerminal {
    private BancoTerminal() {
    }

    static void verificar(ConexaoFactory conexoes) throws ClassNotFoundException, SQLException {
        String sql = "SELECT id, nome FROM AEGIS_EQUIPE WHERE id = 1";
        try (var conexao = conexoes.conexao();
             var stmt = conexao.prepareStatement(sql);
             var equipes = stmt.executeQuery()) {
            System.out.println("Conectado ao Oracle da FIAP.");
            if (equipes.next()) {
                System.out.println("Equipe piloto encontrada: " + equipes.getLong("id") + " - " + equipes.getString("nome"));
            } else {
                System.out.println("A tabela existe. Confira o cadastro da equipe piloto no script SQL.");
            }
        }
    }
}

package br.com.fiap.aegis.dao;

import br.com.fiap.aegis.conexoes.ConexaoFactory;
import br.com.fiap.aegis.entities.Perfil;
import br.com.fiap.aegis.entities.Usuario;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

@Repository
public class UsuarioDAO {
    private final ConexaoFactory conexoes;

    public UsuarioDAO(ConexaoFactory conexoes) {
        this.conexoes = conexoes;
    }

    public Optional<Conta> buscarPorEmail(String email) throws SQLException {
        String sql = """
                SELECT id, perfil, nome, documento, email, endereco, responsavel, senha_hash, equipe_id
                FROM AEGIS_USUARIO WHERE email = ?
                """;
        try (var conexao = conexoes.conexao(); var stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Usuario usuario = new Usuario(rs.getLong("id"), Perfil.valueOf(rs.getString("perfil")),
                        rs.getString("nome"), rs.getString("documento"), rs.getString("email"),
                        rs.getString("endereco"), rs.getString("responsavel"), rs.getObject("equipe_id", Long.class));
                return Optional.of(new Conta(usuario, rs.getString("senha_hash")));
            }
        }
    }

    public Usuario inserir(Usuario usuario, String senhaHash) throws SQLException {
        String sql = """
                INSERT INTO AEGIS_USUARIO (perfil, nome, documento, email, endereco, responsavel, senha_hash, equipe_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var conexao = conexoes.conexao();
             PreparedStatement stmt = conexao.prepareStatement(sql, new String[]{"ID"})) {
            stmt.setString(1, usuario.perfil().name());
            stmt.setString(2, usuario.nome());
            stmt.setString(3, usuario.documento());
            stmt.setString(4, usuario.email());
            stmt.setString(5, usuario.endereco());
            stmt.setString(6, usuario.responsavel());
            stmt.setString(7, senhaHash);
            stmt.setObject(8, usuario.equipeId(), Types.NUMERIC);
            stmt.executeUpdate();
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("O cadastro não retornou o ID.");
                }
                return new Usuario(chaves.getLong(1), usuario.perfil(), usuario.nome(), usuario.documento(),
                        usuario.email(), usuario.endereco(), usuario.responsavel(), usuario.equipeId());
            }
        }
    }

    public record Conta(Usuario usuario, String senhaHash) {
    }
}

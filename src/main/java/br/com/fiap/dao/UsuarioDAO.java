package br.com.fiap.dao;

import br.com.fiap.conexoes.ConexaoFactory;
import br.com.fiap.entities.DadosPerfil;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class UsuarioDAO {
    private final ConexaoFactory conexoes;

    public UsuarioDAO(ConexaoFactory conexoes) {
        this.conexoes = conexoes;
    }

    public Optional<Conta> buscarPorEmail(String email) throws SQLException {
        try (var conexao = conexoes.abrir()) {
            return buscarPorEmail(conexao, email);
        }
    }

    private Optional<Conta> buscarPorEmail(Connection conexao, String email) throws SQLException {
        String sql = """
                SELECT u.id, u.perfil, u.email, u.senha_hash,
                       COALESCE(c.nome, e.razao_social, g.nome, a.nome) AS nome,
                       COALESCE(c.cpf, e.cnpj) AS documento,
                       COALESCE(c.endereco, e.endereco) AS endereco,
                       e.responsavel, a.equipe_id
                FROM AEGIS_USUARIO u
                LEFT JOIN AEGIS_CIDADAO c ON c.usuario_id = u.id AND u.perfil = 'CIDADAO'
                LEFT JOIN AEGIS_EMPRESA e ON e.usuario_id = u.id AND u.perfil = 'EMPRESA'
                LEFT JOIN AEGIS_GESTOR g ON g.usuario_id = u.id AND u.perfil = 'GESTOR'
                LEFT JOIN AEGIS_AGENTE a ON a.usuario_id = u.id AND u.perfil = 'AGENTE'
                WHERE u.email = ?
                """;
        try (var stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String nome = rs.getString("nome");
                if (nome == null) {
                    throw new SQLException("Conta sem registro de perfil.");
                }
                Usuario usuario = new Usuario(rs.getLong("id"), Perfil.valueOf(rs.getString("perfil")),
                        nome, rs.getString("documento"), rs.getString("email"), rs.getString("endereco"),
                        rs.getString("responsavel"), rs.getObject("equipe_id", Long.class));
                return Optional.of(new Conta(usuario, rs.getString("senha_hash")));
            }
        }
    }

    public Optional<DadosPerfil.Empresa> buscarEmpresa(long usuarioId) throws SQLException {
        String sql = """
                SELECT razao_social, nome_fantasia, cnpj, atividade_economica, endereco, responsavel, telefone,
                       caracteristicas_instalacoes
                FROM AEGIS_EMPRESA WHERE usuario_id = ?
                """;
        try (var conexao = conexoes.abrir(); var stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, usuarioId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(new DadosPerfil.Empresa(rs.getString(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getString(8))) : Optional.empty();
            }
        }
    }

    public DadosPerfil.Empresa atualizarEmpresa(long usuarioId, DadosPerfil.Empresa empresa) throws SQLException {
        String sql = """
                UPDATE AEGIS_EMPRESA
                SET razao_social = ?, nome_fantasia = ?, atividade_economica = ?, endereco = ?, responsavel = ?,
                    telefone = ?, caracteristicas_instalacoes = ?
                WHERE usuario_id = ?
                """;
        try (var conexao = conexoes.abrir(); var stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, empresa.razaoSocial());
            stmt.setString(2, empresa.nomeFantasia());
            stmt.setString(3, empresa.atividadeEconomica());
            stmt.setString(4, empresa.endereco());
            stmt.setString(5, empresa.responsavel());
            stmt.setString(6, empresa.telefone());
            stmt.setString(7, empresa.caracteristicasInstalacoes());
            stmt.setLong(8, usuarioId);
            if (stmt.executeUpdate() != 1) {
                throw ApiException.naoEncontrado("Estabelecimento não encontrado.");
            }
            return empresa;
        }
    }

    public boolean temGestor() throws SQLException {
        try (var conexao = conexoes.abrir()) {
            return temGestor(conexao);
        }
    }

    private boolean temGestor(Connection conexao) throws SQLException {
        try (var stmt = conexao.prepareStatement("SELECT COUNT(*) FROM AEGIS_GESTOR");
             var rs = stmt.executeQuery()) {
            rs.next();
            return rs.getLong(1) > 0;
        }
    }

    public Usuario inserir(String email, String senhaHash, DadosPerfil dados) throws SQLException {
        return inserir(email, senhaHash, dados, false);
    }

    public Usuario inserirPrimeiroGestor(String email, String senhaHash, DadosPerfil.Gestor dados) throws SQLException {
        return inserir(email, senhaHash, dados, true);
    }

    private Usuario inserir(String email, String senhaHash, DadosPerfil dados, boolean primeiroGestor)
            throws SQLException {
        try (var conexao = conexoes.abrir()) {
            conexao.setAutoCommit(false);
            try {
                if (dados.perfil() == Perfil.GESTOR) {
                    travarCadastroDeGestor(conexao);
                    if (primeiroGestor && temGestor(conexao)) {
                        throw ApiException.conflito("Já existe um gestor. Solicite a autorização dele.");
                    }
                }
                long id = inserirConta(conexao, email, senhaHash, dados.perfil());
                inserirPerfil(conexao, id, dados);
                Usuario criado = buscarPorEmail(conexao, email).orElseThrow().usuario();
                conexao.commit();
                return criado;
            } catch (SQLException | RuntimeException e) {
                conexao.rollback();
                throw e;
            }
        }
    }

    private void travarCadastroDeGestor(Connection conexao) throws SQLException {
        try (var stmt = conexao.prepareStatement("SELECT id FROM AEGIS_EQUIPE WHERE id = 1 FOR UPDATE");
             var rs = stmt.executeQuery()) {
            if (!rs.next()) {
                throw ApiException.indisponivel("A equipe piloto precisa existir antes do cadastro de gestor.");
            }
        }
    }

    private long inserirConta(Connection conexao, String email, String senhaHash, Perfil perfil) throws SQLException {
        String sql = "INSERT INTO AEGIS_USUARIO (perfil, email, senha_hash) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, new String[]{"ID"})) {
            stmt.setString(1, perfil.name());
            stmt.setString(2, email);
            stmt.setString(3, senhaHash);
            stmt.executeUpdate();
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("O cadastro não retornou o ID.");
                }
                return chaves.getLong(1);
            }
        }
    }

    private void inserirPerfil(Connection conexao, long id, DadosPerfil dados) throws SQLException {
        switch (dados) {
            case DadosPerfil.Cidadao c -> gravarPerfil(conexao, id, """
                    INSERT INTO AEGIS_CIDADAO (usuario_id, nome, cpf, endereco, ocupacao, telefone)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, c.nome(), c.cpf(), c.endereco(), c.ocupacao(), c.telefone());
            case DadosPerfil.Empresa e -> gravarPerfil(conexao, id, """
                    INSERT INTO AEGIS_EMPRESA (usuario_id, razao_social, nome_fantasia, cnpj, atividade_economica,
                                               endereco, responsavel, telefone, caracteristicas_instalacoes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, e.razaoSocial(), e.nomeFantasia(), e.cnpj(), e.atividadeEconomica(), e.endereco(),
                    e.responsavel(), e.telefone(), e.caracteristicasInstalacoes());
            case DadosPerfil.Gestor g -> gravarPerfil(conexao, id, """
                    INSERT INTO AEGIS_GESTOR (usuario_id, nome, identificacao_funcional, setor)
                    VALUES (?, ?, ?, ?)
                    """, g.nome(), g.identificacaoFuncional(), g.setor());
            case DadosPerfil.Agente a -> gravarPerfil(conexao, id, """
                    INSERT INTO AEGIS_AGENTE (usuario_id, nome, identificacao_funcional, equipe_id)
                    VALUES (?, ?, ?, ?)
                    """, a.nome(), a.identificacaoFuncional(), a.equipeId());
        }
    }

    private void gravarPerfil(Connection conexao, long id, String sql, Object... campos) throws SQLException {
        try (var stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, id);
            for (int i = 0; i < campos.length; i++) {
                if (campos[i] instanceof Long numero) {
                    stmt.setLong(i + 2, numero);
                } else {
                    stmt.setString(i + 2, (String) campos[i]);
                }
            }
            stmt.executeUpdate();
        }
    }

    public record Conta(Usuario usuario, String senhaHash) {
    }
}

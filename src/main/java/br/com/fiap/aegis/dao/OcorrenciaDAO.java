package br.com.fiap.aegis.dao;

import br.com.fiap.aegis.conexoes.ConexaoFactory;
import br.com.fiap.aegis.entities.Ocorrencia;
import br.com.fiap.aegis.entities.Prioridade;
import br.com.fiap.aegis.excecoes.ApiException;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OcorrenciaDAO {
    private static final String BUSCA = """
            SELECT id, autor_id, descricao, localizacao, agua_parada, reservatorio_aberto, materiais_expostos,
                   prioridade_preliminar, justificativa, criada_em FROM AEGIS_OCORRENCIA
            """;
    private final ConexaoFactory conexoes;

    public OcorrenciaDAO(ConexaoFactory conexoes) {
        this.conexoes = conexoes;
    }

    public List<Ocorrencia> listar(Long autorId) throws SQLException {
        String sql = BUSCA + (autorId == null ? "" : " WHERE autor_id = ?") + " ORDER BY id DESC";
        try (var conexao = conexoes.conexao(); var stmt = conexao.prepareStatement(sql)) {
            if (autorId != null) {
                stmt.setLong(1, autorId);
            }
            try (var rs = stmt.executeQuery()) {
                List<Ocorrencia> ocorrencias = new ArrayList<>();
                while (rs.next()) {
                    ocorrencias.add(mapear(rs));
                }
                return ocorrencias;
            }
        }
    }

    public Optional<Ocorrencia> buscar(long id) throws SQLException {
        try (var conexao = conexoes.conexao()) {
            return buscar(conexao, id);
        }
    }

    private Optional<Ocorrencia> buscar(Connection conexao, long id) throws SQLException {
        try (var stmt = conexao.prepareStatement(BUSCA + " WHERE id = ?")) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public Ocorrencia inserir(Ocorrencia ocorrencia) throws SQLException {
        String sql = """
                INSERT INTO AEGIS_OCORRENCIA
                    (descricao, localizacao, agua_parada, reservatorio_aberto, materiais_expostos,
                     prioridade_preliminar, justificativa, autor_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var conexao = conexoes.conexao(); var stmt = conexao.prepareStatement(sql, new String[]{"ID"})) {
            preencher(stmt, ocorrencia);
            stmt.setLong(8, ocorrencia.autorId());
            stmt.executeUpdate();
            try (var chaves = stmt.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("A ocorrência não retornou o ID.");
                }
                return buscar(conexao, chaves.getLong(1)).orElseThrow();
            }
        }
    }

    public Ocorrencia atualizar(Ocorrencia ocorrencia) throws SQLException {
        String sql = """
                UPDATE AEGIS_OCORRENCIA
                SET descricao = ?, localizacao = ?, agua_parada = ?, reservatorio_aberto = ?, materiais_expostos = ?,
                    prioridade_preliminar = ?, justificativa = ?
                WHERE id = ? AND autor_id = ?
                  AND NOT EXISTS (SELECT 1 FROM AEGIS_VISTORIA WHERE ocorrencia_id = AEGIS_OCORRENCIA.id)
                """;
        String trava = "SELECT id FROM AEGIS_OCORRENCIA WHERE id = ? AND autor_id = ? FOR UPDATE";
        try (var conexao = conexoes.conexao()) {
            conexao.setAutoCommit(false);
            try (var registro = conexao.prepareStatement(trava);
                 var stmt = conexao.prepareStatement(sql)) {
                registro.setLong(1, ocorrencia.id());
                registro.setLong(2, ocorrencia.autorId());
                try (var rs = registro.executeQuery()) {
                    if (!rs.next()) {
                        throw ApiException.naoEncontrado("Ocorrência não encontrada.");
                    }
                }
                preencher(stmt, ocorrencia);
                stmt.setLong(8, ocorrencia.id());
                stmt.setLong(9, ocorrencia.autorId());
                exigirAlteracao(stmt.executeUpdate());
                Ocorrencia atualizada = buscar(conexao, ocorrencia.id()).orElseThrow();
                conexao.commit();
                return atualizada;
            } catch (SQLException | RuntimeException e) {
                conexao.rollback();
                throw e;
            }
        }
    }

    public void excluir(long id, long autorId) throws SQLException {
        String sql = """
                DELETE FROM AEGIS_OCORRENCIA WHERE id = ? AND autor_id = ?
                  AND NOT EXISTS (SELECT 1 FROM AEGIS_VISTORIA WHERE ocorrencia_id = AEGIS_OCORRENCIA.id)
                """;
        try (var conexao = conexoes.conexao(); var stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, autorId);
            exigirAlteracao(stmt.executeUpdate());
        }
    }

    private void exigirAlteracao(int linhas) {
        if (linhas == 0) {
            throw ApiException.conflito("A ocorrência foi removida ou tem uma vistoria vinculada.");
        }
    }

    private void preencher(PreparedStatement stmt, Ocorrencia ocorrencia) throws SQLException {
        stmt.setString(1, ocorrencia.descricao());
        stmt.setString(2, ocorrencia.localizacao());
        stmt.setObject(3, numero(ocorrencia.aguaParada()), Types.NUMERIC);
        stmt.setObject(4, numero(ocorrencia.reservatorioAberto()), Types.NUMERIC);
        stmt.setObject(5, numero(ocorrencia.materiaisExpostos()), Types.NUMERIC);
        stmt.setString(6, ocorrencia.prioridadePreliminar() == null ? null : ocorrencia.prioridadePreliminar().name());
        stmt.setString(7, ocorrencia.justificativa());
    }

    private Integer numero(Boolean valor) {
        if (valor == null) {
            return null;
        }
        return valor ? 1 : 0;
    }

    private Boolean booleano(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor != 0;
    }

    private Ocorrencia mapear(ResultSet rs) throws SQLException {
        String prioridade = rs.getString("prioridade_preliminar");
        return new Ocorrencia(rs.getLong("id"), rs.getLong("autor_id"), rs.getString("descricao"),
                rs.getString("localizacao"), booleano(rs, "agua_parada"), booleano(rs, "reservatorio_aberto"),
                booleano(rs, "materiais_expostos"), prioridade == null ? null : Prioridade.valueOf(prioridade),
                rs.getString("justificativa"), rs.getTimestamp("criada_em").toLocalDateTime());
    }
}

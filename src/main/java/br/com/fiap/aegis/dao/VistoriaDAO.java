package br.com.fiap.aegis.dao;

import br.com.fiap.aegis.conexoes.ConexaoFactory;
import br.com.fiap.aegis.entities.Equipe;
import br.com.fiap.aegis.entities.HistoricoVistoria;
import br.com.fiap.aegis.entities.Prioridade;
import br.com.fiap.aegis.entities.StatusVistoria;
import br.com.fiap.aegis.entities.Usuario;
import br.com.fiap.aegis.entities.Vistoria;
import br.com.fiap.aegis.excecoes.ApiException;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class VistoriaDAO {
    private static final String BUSCA = """
            SELECT v.id, v.ocorrencia_id, o.autor_id, v.equipe_id, v.status, v.prioridade, v.agendada_para,
                   v.resultado, v.versao
            FROM AEGIS_VISTORIA v JOIN AEGIS_OCORRENCIA o ON o.id = v.ocorrencia_id
            """;
    private final ConexaoFactory conexoes;

    public VistoriaDAO(ConexaoFactory conexoes) {
        this.conexoes = conexoes;
    }

    public Optional<Vistoria> buscar(long id) throws SQLException {
        try (var conexao = conexoes.conexao(); var stmt = conexao.prepareStatement(BUSCA + " WHERE v.id = ?")) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Vistoria> listar(Usuario usuario) throws SQLException {
        String filtro = switch (usuario.perfil()) {
            case GESTOR -> "";
            case AGENTE -> " WHERE v.equipe_id = ?";
            case EMPRESA, CIDADAO -> " WHERE o.autor_id = ?";
        };
        try (var conexao = conexoes.conexao();
             var stmt = conexao.prepareStatement(BUSCA + filtro + " ORDER BY v.id DESC")) {
            if (!filtro.isEmpty()) {
                long id = usuario.equipeId() == null ? usuario.id() : usuario.equipeId();
                stmt.setLong(1, id);
            }
            try (var rs = stmt.executeQuery()) {
                List<Vistoria> vistorias = new ArrayList<>();
                while (rs.next()) {
                    vistorias.add(mapear(rs));
                }
                return vistorias;
            }
        }
    }

    public List<Equipe> listarEquipes() throws SQLException {
        try (var conexao = conexoes.conexao();
             var stmt = conexao.prepareStatement("SELECT id, nome FROM AEGIS_EQUIPE ORDER BY id");
             var rs = stmt.executeQuery()) {
            List<Equipe> equipes = new ArrayList<>();
            while (rs.next()) {
                equipes.add(new Equipe(rs.getLong("id"), rs.getString("nome")));
            }
            return equipes;
        }
    }

    public List<HistoricoVistoria> historico(long id) throws SQLException {
        String sql = """
                SELECT id, vistoria_id, autor_id, acao, detalhe, registrado_em
                FROM AEGIS_HISTORICO WHERE vistoria_id = ? ORDER BY id
                """;
        try (var conexao = conexoes.conexao(); var stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                List<HistoricoVistoria> eventos = new ArrayList<>();
                while (rs.next()) {
                    eventos.add(new HistoricoVistoria(rs.getLong("id"), rs.getLong("vistoria_id"),
                            rs.getLong("autor_id"), rs.getString("acao"), rs.getString("detalhe"),
                            rs.getTimestamp("registrado_em").toLocalDateTime()));
                }
                return eventos;
            }
        }
    }

    public Vistoria inserir(long ocorrenciaId, long autorId) throws SQLException {
        String trava = "SELECT id FROM AEGIS_OCORRENCIA WHERE id = ? AND autor_id = ? FOR UPDATE";
        String sql = "INSERT INTO AEGIS_VISTORIA (ocorrencia_id, status) VALUES (?, 'PENDENTE')";
        try (var conexao = conexoes.conexao()) {
            conexao.setAutoCommit(false);
            try (var ocorrencia = conexao.prepareStatement(trava);
                 var stmt = conexao.prepareStatement(sql, new String[]{"ID"})) {
                ocorrencia.setLong(1, ocorrenciaId);
                ocorrencia.setLong(2, autorId);
                try (var rs = ocorrencia.executeQuery()) {
                    if (!rs.next()) {
                        throw ApiException.naoEncontrado("Ocorrência não encontrada.");
                    }
                }
                stmt.setLong(1, ocorrenciaId);
                stmt.executeUpdate();
                try (var chaves = stmt.getGeneratedKeys()) {
                    if (!chaves.next()) {
                        throw new SQLException("A solicitação não retornou o ID.");
                    }
                    long id = chaves.getLong(1);
                    gravarHistorico(conexao, id, autorId, "SOLICITADA", "Vistoria solicitada.");
                    conexao.commit();
                    return new Vistoria(id, ocorrenciaId, autorId, null, StatusVistoria.PENDENTE,
                            null, null, null, 0);
                }
            } catch (SQLException | RuntimeException e) {
                conexao.rollback();
                throw e;
            }
        }
    }

    public Vistoria atualizar(Vistoria vistoria, long autorId, String acao, String detalhe) throws SQLException {
        String sql = """
                UPDATE AEGIS_VISTORIA
                SET equipe_id = ?, status = ?, prioridade = ?, agendada_para = ?, resultado = ?, versao = versao + 1
                WHERE id = ? AND versao = ?
                """;
        try (var conexao = conexoes.conexao()) {
            conexao.setAutoCommit(false);
            try (var stmt = conexao.prepareStatement(sql)) {
                stmt.setObject(1, vistoria.equipeId(), Types.NUMERIC);
                stmt.setString(2, vistoria.status().name());
                stmt.setString(3, vistoria.prioridade().name());
                stmt.setTimestamp(4, vistoria.agendadaPara() == null ? null : Timestamp.valueOf(vistoria.agendadaPara()));
                stmt.setString(5, vistoria.resultado());
                stmt.setLong(6, vistoria.id());
                stmt.setInt(7, vistoria.versao());
                if (stmt.executeUpdate() != 1) {
                    throw ApiException.conflito("A vistoria foi alterada. Consulte o estado atual e tente novamente.");
                }

                gravarHistorico(conexao, vistoria.id(), autorId, acao, detalhe);
                conexao.commit();
                return new Vistoria(vistoria.id(), vistoria.ocorrenciaId(), vistoria.solicitanteId(), vistoria.equipeId(),
                        vistoria.status(), vistoria.prioridade(), vistoria.agendadaPara(), vistoria.resultado(),
                        vistoria.versao() + 1);
            } catch (SQLException | RuntimeException e) {
                conexao.rollback();
                throw e;
            }
        }
    }

    private void gravarHistorico(Connection conexao, long vistoriaId, long autorId,
                                 String acao, String detalhe) throws SQLException {
        String sql = "INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe) VALUES (?, ?, ?, ?)";
        try (var stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, vistoriaId);
            stmt.setLong(2, autorId);
            stmt.setString(3, acao);
            stmt.setString(4, detalhe);
            stmt.executeUpdate();
        }
    }

    private Vistoria mapear(ResultSet rs) throws SQLException {
        String prioridade = rs.getString("prioridade");
        Timestamp agendada = rs.getTimestamp("agendada_para");
        return new Vistoria(rs.getLong("id"), rs.getLong("ocorrencia_id"), rs.getLong("autor_id"),
                rs.getObject("equipe_id", Long.class), StatusVistoria.valueOf(rs.getString("status")),
                prioridade == null ? null : Prioridade.valueOf(prioridade),
                agendada == null ? null : agendada.toLocalDateTime(), rs.getString("resultado"), rs.getInt("versao"));
    }
}

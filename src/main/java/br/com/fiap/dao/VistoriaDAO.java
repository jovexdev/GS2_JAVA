package br.com.fiap.dao;

import br.com.fiap.conexoes.ConexaoFactory;
import br.com.fiap.entities.AgenteVistoria;
import br.com.fiap.entities.Equipe;
import br.com.fiap.entities.HistoricoVistoria;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Prioridade;
import br.com.fiap.entities.StatusVistoria;
import br.com.fiap.entities.Usuario;
import br.com.fiap.entities.Vistoria;
import br.com.fiap.excecoes.ApiException;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class VistoriaDAO {
    private static final String BUSCA = """
            SELECT v.id, v.ocorrencia_id, o.autor_id, v.equipe_id, v.status, v.prioridade, v.agendada_para,
                   v.resultado, v.versao, v.agente_id, v.inspecao, v.recomendacoes,
                   e.nome AS equipe_nome, a.nome AS agente_nome
            FROM AEGIS_VISTORIA v JOIN AEGIS_OCORRENCIA o ON o.id = v.ocorrencia_id
            LEFT JOIN AEGIS_EQUIPE e ON e.id = v.equipe_id
            LEFT JOIN AEGIS_AGENTE a ON a.usuario_id = v.agente_id
            """;
    private final ConexaoFactory conexoes;

    public VistoriaDAO(ConexaoFactory conexoes) {
        this.conexoes = conexoes;
    }

    public Optional<Vistoria> buscar(long id) throws SQLException {
        try (var conexao = conexoes.abrir()) {
            return buscar(conexao, id);
        }
    }

    private Optional<Vistoria> buscar(Connection conexao, long id) throws SQLException {
        try (var stmt = conexao.prepareStatement(BUSCA + " WHERE v.id = ?")) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Vistoria> listar(Usuario usuario) throws SQLException {
        String filtro = switch (usuario.perfil()) {
            case GESTOR -> "";
            case AGENTE -> " WHERE v.agendada_para IS NOT NULL AND (v.agente_id = ? OR (v.agente_id IS NULL AND v.equipe_id = ?))";
            case EMPRESA, CIDADAO -> " WHERE o.autor_id = ?";
        };
        try (var conexao = conexoes.abrir();
             var stmt = conexao.prepareStatement(BUSCA + filtro + " ORDER BY v.id DESC")) {
            if (!filtro.isEmpty()) {
                stmt.setLong(1, usuario.id());
            }
            if (usuario.perfil() == Perfil.AGENTE) {
                stmt.setObject(2, usuario.equipeId(), Types.NUMERIC);
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
        try (var conexao = conexoes.abrir();
             var stmt = conexao.prepareStatement("SELECT id, nome FROM AEGIS_EQUIPE ORDER BY id");
             var rs = stmt.executeQuery()) {
            List<Equipe> equipes = new ArrayList<>();
            while (rs.next()) {
                equipes.add(new Equipe(rs.getLong("id"), rs.getString("nome")));
            }
            return equipes;
        }
    }

    public boolean equipeDisponivel(long equipeId, LocalDateTime horario) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AEGIS_VISTORIA WHERE equipe_id = ? AND agendada_para = ?";
        try (var conexao = conexoes.abrir(); var stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, equipeId);
            stmt.setTimestamp(2, Timestamp.valueOf(horario));
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) == 0;
            }
        }
    }

    public Optional<LocalEmpresa> buscarLocalEmpresa(long usuarioId) throws SQLException {
        String sql = "SELECT razao_social, endereco FROM AEGIS_EMPRESA WHERE usuario_id = ?";
        try (var conexao = conexoes.abrir(); var stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, usuarioId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(new LocalEmpresa(rs.getString(1), rs.getString(2))) : Optional.empty();
            }
        }
    }

    public record LocalEmpresa(String nome, String endereco) {
    }

    public List<AgenteVistoria> listarAgentes() throws SQLException {
        String sql = """
                SELECT a.usuario_id, a.nome, a.equipe_id, e.nome AS equipe_nome
                FROM AEGIS_AGENTE a JOIN AEGIS_EQUIPE e ON e.id = a.equipe_id
                ORDER BY a.nome, a.usuario_id
                """;
        try (var conexao = conexoes.abrir(); var stmt = conexao.prepareStatement(sql); var rs = stmt.executeQuery()) {
            List<AgenteVistoria> agentes = new ArrayList<>();
            while (rs.next()) {
                agentes.add(new AgenteVistoria(rs.getLong("usuario_id"), rs.getString("nome"),
                        rs.getLong("equipe_id"), rs.getString("equipe_nome")));
            }
            return agentes;
        }
    }

    public List<HistoricoVistoria> historico(long id) throws SQLException {
        String sql = """
                SELECT id, vistoria_id, autor_id, acao, detalhe, registrado_em
                FROM AEGIS_HISTORICO WHERE vistoria_id = ? ORDER BY id
                """;
        try (var conexao = conexoes.abrir(); var stmt = conexao.prepareStatement(sql)) {
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

    public Vistoria inserir(long ocorrenciaId, long autorId, long atorId, StatusVistoria status,
                            String acao, String detalhe) throws SQLException {
        String trava = "SELECT id FROM AEGIS_OCORRENCIA WHERE id = ? AND autor_id = ? FOR UPDATE";
        String sql = "INSERT INTO AEGIS_VISTORIA (ocorrencia_id, status) VALUES (?, ?)";
        try (var conexao = conexoes.abrir()) {
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
                stmt.setString(2, status.name());
                stmt.executeUpdate();
                try (var chaves = stmt.getGeneratedKeys()) {
                    if (!chaves.next()) {
                        throw new SQLException("A solicitação não retornou o ID.");
                    }
                    long id = chaves.getLong(1);
                    gravarHistorico(conexao, id, atorId, acao, detalhe);
                    conexao.commit();
                    return new Vistoria(id, ocorrenciaId, autorId, null, status,
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
                SET equipe_id = ?, agente_id = ?, status = ?, prioridade = ?, agendada_para = ?, resultado = ?,
                    inspecao = ?, recomendacoes = ?, versao = versao + 1
                WHERE id = ? AND versao = ?
                """;
        try (var conexao = conexoes.abrir()) {
            conexao.setAutoCommit(false);
            try (var stmt = conexao.prepareStatement(sql)) {
                stmt.setObject(1, vistoria.equipeId(), Types.NUMERIC);
                stmt.setObject(2, vistoria.agenteId(), Types.NUMERIC);
                stmt.setString(3, vistoria.status().name());
                stmt.setString(4, vistoria.prioridade() == null ? null : vistoria.prioridade().name());
                stmt.setTimestamp(5, vistoria.agendadaPara() == null ? null : Timestamp.valueOf(vistoria.agendadaPara()));
                stmt.setString(6, vistoria.resultado());
                stmt.setString(7, vistoria.inspecao());
                stmt.setString(8, vistoria.recomendacoes());
                stmt.setLong(9, vistoria.id());
                stmt.setInt(10, vistoria.versao());
                if (stmt.executeUpdate() != 1) {
                    throw ApiException.conflito("A vistoria foi alterada. Consulte o estado atual e tente novamente.");
                }

                gravarHistorico(conexao, vistoria.id(), autorId, acao, detalhe);
                Vistoria atualizada = buscar(conexao, vistoria.id()).orElseThrow();
                conexao.commit();
                return atualizada;
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
                agendada == null ? null : agendada.toLocalDateTime(), rs.getString("resultado"), rs.getInt("versao"),
                rs.getObject("agente_id", Long.class), rs.getString("inspecao"), rs.getString("recomendacoes"),
                rs.getString("equipe_nome"), rs.getString("agente_nome"));
    }
}

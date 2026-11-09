package br.com.fiap.aegis.bo;

import br.com.fiap.aegis.dao.VistoriaDAO;
import br.com.fiap.aegis.entities.Equipe;
import br.com.fiap.aegis.entities.HistoricoVistoria;
import br.com.fiap.aegis.entities.Ocorrencia;
import br.com.fiap.aegis.entities.Perfil;
import br.com.fiap.aegis.entities.Prioridade;
import br.com.fiap.aegis.entities.StatusVistoria;
import br.com.fiap.aegis.entities.Usuario;
import br.com.fiap.aegis.entities.Vistoria;
import br.com.fiap.aegis.excecoes.ApiException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class VistoriaBO {
    private static final ZoneId FUSO_PILOTO = ZoneId.of("America/Sao_Paulo");
    private final VistoriaDAO dao;
    private final OcorrenciaBO ocorrencias;

    public VistoriaBO(VistoriaDAO dao, OcorrenciaBO ocorrencias) {
        this.dao = dao;
        this.ocorrencias = ocorrencias;
    }

    public Vistoria solicitar(long ocorrenciaId, Usuario usuario) throws SQLException {
        exigirPerfil(usuario, Perfil.EMPRESA);
        ocorrencias.consultarPropria(ocorrenciaId, usuario);
        return dao.inserir(ocorrenciaId, usuario.id());
    }

    public List<Vistoria> listar(Usuario usuario) throws SQLException {
        return dao.listar(usuario);
    }

    public Vistoria consultar(long id, Usuario usuario) throws SQLException {
        Vistoria vistoria = dao.buscar(id).orElseThrow(() -> ApiException.naoEncontrado("Vistoria não encontrada."));
        boolean podeConsultar = switch (usuario.perfil()) {
            case GESTOR -> true;
            case AGENTE -> usuario.equipeId() != null && Objects.equals(usuario.equipeId(), vistoria.equipeId());
            case EMPRESA, CIDADAO -> usuario.id() == vistoria.solicitanteId();
        };
        if (!podeConsultar) {
            throw ApiException.naoEncontrado("Vistoria não encontrada.");
        }
        return vistoria;
    }

    public List<HistoricoVistoria> historico(long id, Usuario usuario) throws SQLException {
        consultar(id, usuario);
        return dao.historico(id);
    }

    public Ocorrencia ocorrencia(long id, Usuario usuario) throws SQLException {
        return ocorrencias.buscar(consultar(id, usuario).ocorrenciaId());
    }

    public List<Equipe> equipes(Usuario usuario) throws SQLException {
        exigirPerfil(usuario, Perfil.GESTOR);
        return dao.listarEquipes();
    }

    public Vistoria revisar(long id, Revisao revisao, Usuario gestor) throws SQLException {
        exigirPerfil(gestor, Perfil.GESTOR);
        Vistoria atual = consultar(id, gestor);
        exigirStatus(atual, StatusVistoria.PENDENTE);
        if (revisao.prioridade() == null) {
            throw ApiException.invalido("Informe a prioridade revisada.");
        }
        String justificativa = Validacao.obrigatorio(revisao.justificativa(), "Justificativa", 950);
        Vistoria revisada = new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), null,
                atual.status(), revisao.prioridade(), null, null, atual.versao());
        return dao.atualizar(revisada, gestor.id(), "REVISADA", revisao.prioridade().name() + ": " + justificativa);
    }

    public Vistoria agendar(long id, Agendamento dados, Usuario gestor) throws SQLException {
        exigirPerfil(gestor, Perfil.GESTOR);
        Vistoria atual = consultar(id, gestor);
        exigirStatus(atual, StatusVistoria.PENDENTE);
        if (atual.prioridade() == null) {
            throw ApiException.conflito("Revise a prioridade antes de agendar.");
        }
        if (dados.agendadaPara() == null || dados.agendadaPara().getSecond() != 0
                || dados.agendadaPara().getNano() != 0 || !dados.agendadaPara().isAfter(LocalDateTime.now(FUSO_PILOTO))) {
            throw ApiException.invalido("Informe uma data futura com precisão de minuto no horário de São Paulo.");
        }
        Equipe equipe = dao.listarEquipes().stream().filter(e -> e.id() == dados.equipeId())
                .findFirst().orElseThrow(() -> ApiException.naoEncontrado("Equipe não encontrada."));
        Vistoria agendada = new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), equipe.id(),
                StatusVistoria.AGENDADA, atual.prioridade(), dados.agendadaPara(), null, atual.versao());
        String detalhe = equipe.nome() + " designada para " + dados.agendadaPara() + ".";
        return dao.atualizar(agendada, gestor.id(), "AGENDADA", detalhe);
    }

    public Vistoria concluir(long id, Conclusao dados, Usuario agente) throws SQLException {
        exigirPerfil(agente, Perfil.AGENTE);
        Vistoria atual = consultar(id, agente);
        exigirStatus(atual, StatusVistoria.AGENDADA);
        String resultado = Validacao.obrigatorio(dados.resultado(), "Resultado", 1000);
        Vistoria concluida = new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), atual.equipeId(),
                StatusVistoria.CONCLUIDA, atual.prioridade(), atual.agendadaPara(), resultado, atual.versao());
        return dao.atualizar(concluida, agente.id(), "CONCLUIDA", resultado);
    }

    private void exigirPerfil(Usuario usuario, Perfil perfil) {
        if (usuario.perfil() != perfil) {
            throw ApiException.proibido("Seu perfil não pode executar esta operação.");
        }
    }

    private void exigirStatus(Vistoria vistoria, StatusVistoria status) {
        if (vistoria.status() != status) {
            throw ApiException.conflito("A operação exige uma vistoria com status " + status.name() + ".");
        }
    }

    public record Revisao(Prioridade prioridade, String justificativa) {
    }

    public record Agendamento(long equipeId, LocalDateTime agendadaPara) {
    }

    public record Conclusao(String resultado) {
    }
}

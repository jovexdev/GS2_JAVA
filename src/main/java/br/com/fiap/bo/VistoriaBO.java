package br.com.fiap.bo;

import br.com.fiap.dao.VistoriaDAO;
import br.com.fiap.entities.AgenteVistoria;
import br.com.fiap.entities.Equipe;
import br.com.fiap.entities.HistoricoVistoria;
import br.com.fiap.entities.Ocorrencia;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Prioridade;
import br.com.fiap.entities.StatusVistoria;
import br.com.fiap.entities.Usuario;
import br.com.fiap.entities.Vistoria;
import br.com.fiap.excecoes.ApiException;
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
        return dao.inserir(ocorrenciaId, usuario.id(), usuario.id(), StatusVistoria.PENDENTE,
                "SOLICITADA", "Vistoria solicitada pelo estabelecimento.");
    }

    public Vistoria analisarOcorrencia(long ocorrenciaId, DecisaoOcorrencia dados, Usuario gestor) throws SQLException {
        exigirPerfil(gestor, Perfil.GESTOR);
        Ocorrencia ocorrencia = ocorrencias.consultar(ocorrenciaId, gestor);
        String justificativa = Validacao.obrigatorio(dados.justificativa(), "Justificativa", 1000);
        if (dados.realizarVistoria() == null) {
            throw ApiException.invalido("Informe se a ocorrência exige vistoria.");
        }
        if (dados.realizarVistoria()) {
            return dao.inserir(ocorrenciaId, ocorrencia.autorId(), gestor.id(), StatusVistoria.PENDENTE,
                    "ENCAMINHADA", justificativa);
        }
        return dao.inserir(ocorrenciaId, ocorrencia.autorId(), gestor.id(), StatusVistoria.ENCERRADA,
                "ENCERRADA", justificativa);
    }

    public List<Vistoria> listar(Usuario usuario) throws SQLException {
        return dao.listar(usuario).stream().map(vistoria -> visao(vistoria, usuario)).toList();
    }

    public Vistoria consultar(long id, Usuario usuario) throws SQLException {
        Vistoria vistoria = dao.buscar(id).orElseThrow(() -> ApiException.naoEncontrado("Vistoria não encontrada."));
        boolean podeConsultar = switch (usuario.perfil()) {
            case GESTOR -> true;
            case AGENTE -> vistoria.agendadaPara() != null && (vistoria.agenteId() != null
                    ? Objects.equals(vistoria.agenteId(), usuario.id())
                    : usuario.equipeId() != null && Objects.equals(usuario.equipeId(), vistoria.equipeId()));
            case EMPRESA, CIDADAO -> usuario.id() == vistoria.solicitanteId();
        };
        if (!podeConsultar) {
            throw ApiException.naoEncontrado("Vistoria não encontrada.");
        }
        return visao(vistoria, usuario);
    }

    public List<HistoricoVistoria> historico(long id, Usuario usuario) throws SQLException {
        Vistoria vistoria = consultar(id, usuario);
        List<HistoricoVistoria> historico = dao.historico(id);
        if (solicitante(usuario) && !finalizada(vistoria)) {
            return historico.stream().filter(evento -> !evento.acao().equals("INSPECAO")
                    && !evento.acao().equals("RECOMENDACOES")).toList();
        }
        return historico;
    }

    public Ocorrencia ocorrencia(long id, Usuario usuario) throws SQLException {
        return ocorrencias.buscar(consultar(id, usuario).ocorrenciaId());
    }

    public LocalAtendimento local(long id, Usuario usuario) throws SQLException {
        Ocorrencia ocorrencia = ocorrencia(id, usuario);
        var empresa = dao.buscarLocalEmpresa(ocorrencia.autorId());
        return new LocalAtendimento(ocorrencia, empresa.map(VistoriaDAO.LocalEmpresa::nome).orElse(null),
                empresa.map(VistoriaDAO.LocalEmpresa::endereco).orElse(null));
    }

    public List<Equipe> equipes(Usuario usuario) throws SQLException {
        exigirPerfil(usuario, Perfil.GESTOR);
        return dao.listarEquipes();
    }

    public List<AgenteVistoria> agentes(Usuario usuario) throws SQLException {
        exigirPerfil(usuario, Perfil.GESTOR);
        return dao.listarAgentes();
    }

    public Vistoria revisar(long id, Revisao revisao, Usuario gestor) throws SQLException {
        exigirPerfil(gestor, Perfil.GESTOR);
        Vistoria atual = consultar(id, gestor);
        exigirStatus(atual, StatusVistoria.PENDENTE);
        if (revisao.prioridade() == null) {
            throw ApiException.invalido("Informe a prioridade revisada.");
        }
        String justificativa = Validacao.obrigatorio(revisao.justificativa(), "Justificativa", 950);
        Vistoria revisada = new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), atual.equipeId(),
                atual.status(), revisao.prioridade(), null, null, atual.versao(), atual.agenteId(),
                null, null, atual.equipeNome(), atual.agenteNome());
        return dao.atualizar(revisada, gestor.id(), "REVISADA", revisao.prioridade().name() + ": " + justificativa);
    }

    public Vistoria designar(long id, Designacao dados, Usuario gestor) throws SQLException {
        exigirPerfil(gestor, Perfil.GESTOR);
        Vistoria atual = consultar(id, gestor);
        exigirStatus(atual, StatusVistoria.PENDENTE);
        if (atual.prioridade() == null) {
            throw ApiException.conflito("Revise a prioridade antes de designar o agente.");
        }
        AgenteVistoria agente = dao.listarAgentes().stream().filter(a -> a.id() == dados.agenteId())
                .findFirst().orElseThrow(() -> ApiException.naoEncontrado("Agente não encontrado."));
        Vistoria designada = new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), agente.equipeId(),
                atual.status(), atual.prioridade(), null, null, atual.versao(), agente.id(),
                null, null, agente.equipeNome(), agente.nome());
        return dao.atualizar(designada, gestor.id(), "DESIGNADA", "Agente designado na equipe " + agente.equipeNome() + ".");
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
        if (atual.agenteId() != null && !Objects.equals(atual.equipeId(), dados.equipeId())) {
            throw ApiException.conflito("Use a equipe do agente designado para agendar.");
        }
        Equipe equipe = dao.listarEquipes().stream().filter(e -> e.id() == dados.equipeId())
                .findFirst().orElseThrow(() -> ApiException.naoEncontrado("Equipe não encontrada."));
        if (!dao.equipeDisponivel(equipe.id(), dados.agendadaPara())) {
            throw ApiException.conflito("A equipe já tem uma vistoria nesse horário.");
        }
        Vistoria agendada = new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), equipe.id(),
                StatusVistoria.AGENDADA, atual.prioridade(), dados.agendadaPara(), null, atual.versao(),
                atual.agenteId(), null, null, equipe.nome(), atual.agenteNome());
        String detalhe = equipe.nome() + " designada para " + dados.agendadaPara() + ".";
        return dao.atualizar(agendada, gestor.id(), "AGENDADA", detalhe);
    }

    public Vistoria iniciar(long id, Usuario agente) throws SQLException {
        exigirPerfil(agente, Perfil.AGENTE);
        Vistoria atual = consultar(id, agente);
        exigirStatus(atual, StatusVistoria.AGENDADA);
        Vistoria iniciada = new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), atual.equipeId(),
                StatusVistoria.EM_ANDAMENTO, atual.prioridade(), atual.agendadaPara(), null, atual.versao(),
                agente.id(), null, null, atual.equipeNome(), agente.nome());
        return dao.atualizar(iniciada, agente.id(), "INICIADA", "Atendimento de campo iniciado.");
    }

    public Vistoria registrarInspecao(long id, Registro dados, Usuario agente) throws SQLException {
        Vistoria atual = emAtendimento(id, agente);
        String texto = Validacao.obrigatorio(dados.texto(), "Inspeção", 1000);
        return dao.atualizar(estado(atual, atual.status(), null, texto, atual.recomendacoes()),
                agente.id(), "INSPECAO", texto);
    }

    public Vistoria registrarRecomendacoes(long id, Registro dados, Usuario agente) throws SQLException {
        Vistoria atual = emAtendimento(id, agente);
        String texto = Validacao.obrigatorio(dados.texto(), "Recomendações", 1000);
        return dao.atualizar(estado(atual, atual.status(), null, atual.inspecao(), texto),
                agente.id(), "RECOMENDACOES", texto);
    }

    public Vistoria concluir(long id, Conclusao dados, Usuario agente) throws SQLException {
        exigirPerfil(agente, Perfil.AGENTE);
        Vistoria atual = consultar(id, agente);
        if (atual.status() == StatusVistoria.AGENDADA && atual.agenteId() == null) {
            String resultado = Validacao.obrigatorio(dados.resultado(), "Resultado", 1000);
            return dao.atualizar(estado(atual, StatusVistoria.CONCLUIDA, resultado, null, null),
                    agente.id(), "CONCLUIDA", resultado);
        }

        exigirStatus(atual, StatusVistoria.EM_ANDAMENTO);
        if (atual.inspecao() == null) {
            throw ApiException.conflito("Registre a inspeção antes de concluir.");
        }
        String resultado = dados.resultado() == null || dados.resultado().isBlank()
                ? atual.inspecao() : Validacao.obrigatorio(dados.resultado(), "Resultado", 1000);
        return dao.atualizar(estado(atual, StatusVistoria.CONCLUIDA, resultado, atual.inspecao(), atual.recomendacoes()),
                agente.id(), "CONCLUIDA", resultado);
    }

    public Vistoria encerrar(long id, Registro dados, Usuario gestor) throws SQLException {
        exigirPerfil(gestor, Perfil.GESTOR);
        Vistoria atual = consultar(id, gestor);
        if (atual.status() != StatusVistoria.PENDENTE && atual.status() != StatusVistoria.CONCLUIDA) {
            throw ApiException.conflito("O encerramento exige uma solicitação pendente ou uma vistoria concluída.");
        }
        String motivo = Validacao.obrigatorio(dados.texto(), "Justificativa de encerramento", 1000);
        return dao.atualizar(estado(atual, StatusVistoria.ENCERRADA, atual.resultado(), atual.inspecao(),
                atual.recomendacoes()), gestor.id(), "ENCERRADA", motivo);
    }

    private Vistoria emAtendimento(long id, Usuario agente) throws SQLException {
        exigirPerfil(agente, Perfil.AGENTE);
        Vistoria vistoria = consultar(id, agente);
        exigirStatus(vistoria, StatusVistoria.EM_ANDAMENTO);
        return vistoria;
    }

    private Vistoria estado(Vistoria atual, StatusVistoria status, String resultado, String inspecao, String recomendacoes) {
        return new Vistoria(atual.id(), atual.ocorrenciaId(), atual.solicitanteId(), atual.equipeId(), status,
                atual.prioridade(), atual.agendadaPara(), resultado, atual.versao(), atual.agenteId(), inspecao,
                recomendacoes, atual.equipeNome(), atual.agenteNome());
    }

    private Vistoria visao(Vistoria vistoria, Usuario usuario) {
        if (!solicitante(usuario)) {
            return vistoria;
        }
        boolean finalizada = finalizada(vistoria);
        return new Vistoria(vistoria.id(), vistoria.ocorrenciaId(), vistoria.solicitanteId(), vistoria.equipeId(),
                vistoria.status(), vistoria.prioridade(), vistoria.agendadaPara(),
                finalizada ? vistoria.resultado() : null, vistoria.versao(), null,
                finalizada ? vistoria.inspecao() : null, finalizada ? vistoria.recomendacoes() : null,
                vistoria.equipeNome(), null);
    }

    private boolean solicitante(Usuario usuario) {
        return usuario.perfil() == Perfil.CIDADAO || usuario.perfil() == Perfil.EMPRESA;
    }

    private boolean finalizada(Vistoria vistoria) {
        return vistoria.status() == StatusVistoria.CONCLUIDA || vistoria.status() == StatusVistoria.ENCERRADA;
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

    public record Designacao(long agenteId) {
    }

    public record Agendamento(long equipeId, LocalDateTime agendadaPara) {
    }

    public record Conclusao(String resultado) {
    }

    public record Registro(String texto) {
    }

    public record DecisaoOcorrencia(Boolean realizarVistoria, String justificativa) {
    }

    public record LocalAtendimento(Ocorrencia ocorrencia, String estabelecimento, String endereco) {
    }
}

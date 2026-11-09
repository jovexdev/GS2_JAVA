package br.com.fiap.aegis.bo;

import br.com.fiap.aegis.dao.OcorrenciaDAO;
import br.com.fiap.aegis.entities.Ocorrencia;
import br.com.fiap.aegis.entities.Perfil;
import br.com.fiap.aegis.entities.Prioridade;
import br.com.fiap.aegis.entities.Usuario;
import br.com.fiap.aegis.excecoes.ApiException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcorrenciaBO {
    private final OcorrenciaDAO dao;

    public OcorrenciaBO(OcorrenciaDAO dao) {
        this.dao = dao;
    }

    public List<Ocorrencia> listar(Usuario usuario) throws SQLException {
        if (usuario.perfil() == Perfil.GESTOR) {
            return dao.listar(null);
        }
        exigirSolicitante(usuario);
        return dao.listar(usuario.id());
    }

    public Ocorrencia consultar(long id, Usuario usuario) throws SQLException {
        if (usuario.perfil() == Perfil.GESTOR) {
            return buscar(id);
        }
        exigirSolicitante(usuario);
        return consultarPropria(id, usuario);
    }

    public Ocorrencia registrar(Relato relato, Usuario usuario) throws SQLException {
        exigirSolicitante(usuario);
        return dao.inserir(montar(0, relato, usuario));
    }

    public Ocorrencia atualizar(long id, Relato relato, Usuario usuario) throws SQLException {
        exigirSolicitante(usuario);
        consultarPropria(id, usuario);
        return dao.atualizar(montar(id, relato, usuario));
    }

    public void excluir(long id, Usuario usuario) throws SQLException {
        exigirSolicitante(usuario);
        consultarPropria(id, usuario);
        dao.excluir(id, usuario.id());
    }

    Ocorrencia buscar(long id) throws SQLException {
        return dao.buscar(id).orElseThrow(() -> ApiException.naoEncontrado("Ocorrência não encontrada."));
    }

    Ocorrencia consultarPropria(long id, Usuario usuario) throws SQLException {
        Ocorrencia ocorrencia = buscar(id);
        if (ocorrencia.autorId() != usuario.id()) {
            throw ApiException.naoEncontrado("Ocorrência não encontrada.");
        }
        return ocorrencia;
    }

    private void exigirSolicitante(Usuario usuario) {
        if (usuario.perfil() != Perfil.EMPRESA && usuario.perfil() != Perfil.CIDADAO) {
            throw ApiException.proibido("A operação está disponível para cidadãos e empresas.");
        }
    }

    private Ocorrencia montar(long id, Relato relato, Usuario usuario) {
        String descricao = Validacao.obrigatorio(relato.descricao(), "Descrição", 1000);
        String localizacao = Validacao.obrigatorio(relato.localizacao(), "Localização", 250);

        Prioridade prioridade = null;
        String justificativa = null;
        if (usuario.perfil() == Perfil.EMPRESA) {
            if (relato.aguaParada() == null || relato.reservatorioAberto() == null
                    || relato.materiaisExpostos() == null) {
                throw ApiException.invalido("Preencha os três fatores ambientais do estabelecimento.");
            }
            List<String> fatores = new ArrayList<>();
            if (relato.aguaParada()) {
                fatores.add("água parada");
            }
            if (relato.reservatorioAberto()) {
                fatores.add("reservatório aberto");
            }
            if (relato.materiaisExpostos()) {
                fatores.add("materiais expostos");
            }
            prioridade = switch (fatores.size()) {
                case 0 -> Prioridade.OK;
                case 1 -> Prioridade.INSTAVEL;
                default -> Prioridade.CRITICA;
            };
            justificativa = fatores.isEmpty() ? "Sem fatores de prioridade declarados."
                    : "Fatores declarados: " + String.join(", ", fatores) + ".";
        }
        return new Ocorrencia(id, usuario.id(), descricao, localizacao, relato.aguaParada(),
                relato.reservatorioAberto(), relato.materiaisExpostos(), prioridade, justificativa, null);
    }

    public record Relato(String descricao, String localizacao, Boolean aguaParada,
                         Boolean reservatorioAberto, Boolean materiaisExpostos) {
    }
}

package br.com.fiap.bo;

import br.com.fiap.dao.UsuarioDAO;
import br.com.fiap.entities.DadosPerfil;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Locale;

@Service
public class UsuarioBO {
    private final UsuarioDAO dao;

    public UsuarioBO(UsuarioDAO dao) {
        this.dao = dao;
    }

    public Usuario cadastrar(Cadastro cadastro) throws SQLException {
        Perfil perfil = cadastro.perfil();
        if (perfil != Perfil.CIDADAO && perfil != Perfil.EMPRESA) {
            throw ApiException.invalido("O cadastro público aceita CIDADAO ou EMPRESA.");
        }

        String documento = Validacao.obrigatorio(cadastro.documento(), "Documento", 24)
                .replaceAll("[./\\-\\s]", "").toUpperCase(Locale.ROOT);
        if (perfil == Perfil.CIDADAO && !documento.matches("[0-9]{11}")) {
            throw ApiException.invalido("Informe um CPF com 11 dígitos.");
        }
        if (perfil == Perfil.EMPRESA && !documento.matches("[A-Z0-9]{12}[0-9]{2}")) {
            throw ApiException.invalido("Informe um CNPJ com 14 caracteres e dois dígitos finais.");
        }

        String nome = Validacao.obrigatorio(cadastro.nome(), "Nome", 120);
        String endereco = Validacao.obrigatorio(cadastro.endereco(), "Endereço", 250);
        String telefone = opcional(cadastro.telefone(), "Telefone", 30);
        DadosPerfil dados = perfil == Perfil.CIDADAO
                ? new DadosPerfil.Cidadao(nome, documento, endereco,
                        opcional(cadastro.ocupacao(), "Ocupação", 100), telefone)
                : new DadosPerfil.Empresa(nome, opcional(cadastro.nomeFantasia(), "Nome fantasia", 120),
                        documento, opcional(cadastro.atividadeEconomica(), "Atividade econômica", 150), endereco,
                        Validacao.obrigatorio(cadastro.responsavel(), "Responsável", 120), telefone,
                        opcional(cadastro.caracteristicasInstalacoes(), "Características das instalações", 500));
        return dao.inserir(email(cadastro.email()), hash(cadastro.senha()), dados);
    }

    public DadosPerfil.Empresa estabelecimento(Usuario usuario) throws SQLException {
        if (usuario.perfil() != Perfil.EMPRESA) {
            throw ApiException.proibido("O gerenciamento do estabelecimento é exclusivo do perfil empresa.");
        }
        return dao.buscarEmpresa(usuario.id())
                .orElseThrow(() -> ApiException.naoEncontrado("Estabelecimento não encontrado."));
    }

    public DadosPerfil.Empresa atualizarEstabelecimento(Usuario usuario, Estabelecimento dados) throws SQLException {
        var atual = estabelecimento(usuario);
        var empresa = new DadosPerfil.Empresa(
                Validacao.obrigatorio(dados.razaoSocial(), "Razão social", 120),
                opcional(dados.nomeFantasia(), "Nome fantasia", 120), atual.cnpj(),
                opcional(dados.atividadeEconomica(), "Atividade econômica", 150),
                Validacao.obrigatorio(dados.endereco(), "Endereço", 250),
                Validacao.obrigatorio(dados.responsavel(), "Responsável", 120),
                opcional(dados.telefone(), "Telefone", 30),
                opcional(dados.caracteristicasInstalacoes(), "Características das instalações", 500));
        return dao.atualizarEmpresa(usuario.id(), empresa);
    }

    public record Estabelecimento(String razaoSocial, String nomeFantasia, String atividadeEconomica,
                                  String endereco, String responsavel, String telefone,
                                  String caracteristicasInstalacoes) {
    }

    public Usuario autenticar(Login login) throws SQLException {
        String email = email(login.email());
        String senha = login.senha();
        if (senha == null || senha.isBlank() || senha.length() > 128) {
            throw ApiException.naoAutenticado("E-mail ou senha inválidos.");
        }
        return dao.buscarPorEmail(email).filter(c -> Senhas.confere(senha, c.senhaHash()))
                .orElseThrow(() -> ApiException.naoAutenticado("E-mail ou senha inválidos.")).usuario();
    }

    public Usuario autenticar(Login login, Perfil perfilEscolhido) throws SQLException {
        Usuario usuario = autenticar(login);
        if (usuario.perfil() != perfilEscolhido) {
            throw ApiException.proibido("A conta não pertence ao perfil selecionado.");
        }
        return usuario;
    }

    public boolean temGestor() throws SQLException {
        return dao.temGestor();
    }

    public Usuario cadastrarOperador(CadastroOperador cadastro, Login autorizacao) throws SQLException {
        Perfil perfil = cadastro.perfil();
        if (perfil != Perfil.GESTOR && perfil != Perfil.AGENTE) {
            throw ApiException.invalido("Escolha GESTOR ou AGENTE para o cadastro interno.");
        }
        if (autorizacao != null) {
            autenticar(autorizacao, Perfil.GESTOR);
        } else if (perfil == Perfil.AGENTE) {
            throw ApiException.proibido("O cadastro de agente exige autorização de um gestor.");
        }

        String nome = Validacao.obrigatorio(cadastro.nome(), "Nome", 120);
        String identificacao = Validacao.obrigatorio(cadastro.identificacaoFuncional(), "Identificação funcional", 40);
        String email = email(cadastro.email());
        String senhaHash = hash(cadastro.senha());
        if (perfil == Perfil.GESTOR) {
            var dados = new DadosPerfil.Gestor(nome, identificacao, opcional(cadastro.setor(), "Setor", 120));
            return autorizacao == null ? dao.inserirPrimeiroGestor(email, senhaHash, dados)
                    : dao.inserir(email, senhaHash, dados);
        }
        if (cadastro.equipeId() == null || cadastro.equipeId() <= 0) {
            throw ApiException.invalido("Informe o ID da equipe do agente.");
        }
        return dao.inserir(email, senhaHash, new DadosPerfil.Agente(nome, identificacao, cadastro.equipeId()));
    }

    public void prepararOperador(Perfil perfil, String email, String senha, Long equipeId) throws SQLException {
        if (email.isBlank() && senha.isBlank()) {
            return;
        }
        String emailNormalizado = email(email);
        var existente = dao.buscarPorEmail(emailNormalizado);
        if (existente.isPresent()) {
            if (existente.get().usuario().perfil() != perfil) {
                throw ApiException.conflito("O e-mail do operador pertence a outro perfil.");
            }
            return;
        }

        DadosPerfil dados = switch (perfil) {
            case GESTOR -> new DadosPerfil.Gestor("Gestor piloto", null, null);
            case AGENTE -> new DadosPerfil.Agente("Agente piloto", null, equipeId);
            default -> throw ApiException.invalido("Perfil inválido para uma conta piloto.");
        };
        dao.inserir(emailNormalizado, hash(senha), dados);
    }

    private String hash(String senha) {
        if (senha == null || senha.isBlank() || senha.length() < 8 || senha.length() > 128) {
            throw ApiException.invalido("A senha deve ter de 8 a 128 caracteres.");
        }
        return Senhas.gerarHash(senha);
    }

    private String email(String valor) {
        String email = Validacao.obrigatorio(valor, "E-mail", 254).toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw ApiException.invalido("Informe um e-mail válido.");
        }
        return email;
    }

    private String opcional(String valor, String campo, int maximo) {
        return valor == null || valor.isBlank() ? null : Validacao.obrigatorio(valor, campo, maximo);
    }

    public record Cadastro(Perfil perfil, String nome, String documento, String email, String endereco,
                           String responsavel, String senha, String ocupacao, String telefone, String nomeFantasia,
                           String atividadeEconomica, String caracteristicasInstalacoes) {
    }

    public record CadastroOperador(Perfil perfil, String nome, String email, String senha,
                                   String identificacaoFuncional, String setor, Long equipeId) {
    }

    public record Login(String email, String senha) {
    }
}

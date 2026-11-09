package br.com.fiap.bo;

import br.com.fiap.dao.UsuarioDAO;
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

        String responsavel = perfil == Perfil.EMPRESA
                ? Validacao.obrigatorio(cadastro.responsavel(), "Responsável", 120) : null;
        Usuario usuario = new Usuario(0, perfil, Validacao.obrigatorio(cadastro.nome(), "Nome", 120),
                documento, email(cadastro.email()), Validacao.obrigatorio(cadastro.endereco(), "Endereço", 250),
                responsavel, null);
        return salvar(usuario, cadastro.senha());
    }

    public Usuario autenticar(Login login) throws SQLException {
        String email = email(login.email());
        String senha = login.senha();
        if (senha == null || senha.isBlank() || senha.length() > 128) {
            throw ApiException.naoAutenticado("E-mail ou senha inválidos.");
        }
        UsuarioDAO.Conta conta = dao.buscarPorEmail(email)
                .filter(c -> Senhas.confere(senha, c.senhaHash()))
                .orElseThrow(() -> ApiException.naoAutenticado("E-mail ou senha inválidos."));
        return conta.usuario();
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
        String nome = perfil == Perfil.GESTOR ? "Gestor piloto" : "Agente piloto";
        salvar(new Usuario(0, perfil, nome, null, emailNormalizado, null, null, equipeId), senha);
    }

    private Usuario salvar(Usuario usuario, String senha) throws SQLException {
        if (senha == null || senha.isBlank() || senha.length() < 8 || senha.length() > 128) {
            throw ApiException.invalido("A senha deve ter de 8 a 128 caracteres.");
        }
        return dao.inserir(usuario, Senhas.gerarHash(senha));
    }

    private String email(String valor) {
        String email = Validacao.obrigatorio(valor, "E-mail", 254).toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw ApiException.invalido("Informe um e-mail válido.");
        }
        return email;
    }

    public record Cadastro(Perfil perfil, String nome, String documento, String email,
                           String endereco, String responsavel, String senha) {
    }

    public record Login(String email, String senha) {
    }
}

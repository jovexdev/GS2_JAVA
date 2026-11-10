package br.com.fiap.main;

import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;

import java.sql.SQLException;

final class CadastroTerminal {
    private CadastroTerminal() {
    }

    static Perfil escolherPerfil() {
        return switch (Entrada.escolher("Escolha seu perfil", "Gestor", "Agente", "Cidadão (CPF)",
                "Empresa (CNPJ)", "Encerrar")) {
            case "Gestor" -> Perfil.GESTOR;
            case "Agente" -> Perfil.AGENTE;
            case "Cidadão (CPF)" -> Perfil.CIDADAO;
            case "Empresa (CNPJ)" -> Perfil.EMPRESA;
            default -> null;
        };
    }

    static Usuario cadastrar(UsuarioBO usuarios, Perfil perfil) throws SQLException {
        Usuario usuario = switch (perfil) {
            case CIDADAO -> cadastrarCidadao(usuarios);
            case EMPRESA -> cadastrarEmpresa(usuarios);
            case GESTOR, AGENTE -> cadastrarOperador(usuarios, perfil);
        };
        System.out.println("Conta cadastrada com ID " + usuario.id() + ".");
        return usuario;
    }

    private static Usuario cadastrarCidadao(UsuarioBO usuarios) throws SQLException {
        String nome = Entrada.texto("Nome completo");
        String cpf = Entrada.texto("CPF fictício");
        String email = Entrada.texto("E-mail de teste para login");
        String endereco = Entrada.texto("Endereço fictício");
        String ocupacao = Entrada.texto("Ocupação (opcional)");
        String telefone = Entrada.texto("Telefone (opcional)");
        String senha = Entrada.senha("Crie uma senha com pelo menos 8 caracteres");
        return usuarios.cadastrar(new UsuarioBO.Cadastro(Perfil.CIDADAO, nome, cpf, email, endereco, null,
                senha, ocupacao, telefone, null, null, null));
    }

    private static Usuario cadastrarEmpresa(UsuarioBO usuarios) throws SQLException {
        String razaoSocial = Entrada.texto("Razão social");
        String cnpj = Entrada.texto("CNPJ fictício");
        String email = Entrada.texto("E-mail do responsável para login");
        String endereco = Entrada.texto("Endereço do estabelecimento");
        String responsavel = Entrada.texto("Nome do responsável");
        String telefone = Entrada.texto("Telefone do responsável (opcional)");
        String nomeFantasia = Entrada.texto("Nome fantasia (opcional)");
        String atividade = Entrada.texto("Atividade econômica (opcional)");
        String instalacoes = Entrada.texto("Características das instalações (opcional)");
        String senha = Entrada.senha("Crie uma senha com pelo menos 8 caracteres");
        return usuarios.cadastrar(new UsuarioBO.Cadastro(Perfil.EMPRESA, razaoSocial, cnpj, email, endereco,
                responsavel, senha, null, telefone, nomeFantasia, atividade, instalacoes));
    }

    private static Usuario cadastrarOperador(UsuarioBO usuarios, Perfil perfil) throws SQLException {
        boolean temGestor = usuarios.temGestor();
        if (!temGestor && perfil == Perfil.AGENTE) {
            throw ApiException.proibido("Cadastre primeiro um gestor para autorizar o agente.");
        }

        UsuarioBO.Login autorizacao = null;
        if (temGestor) {
            System.out.println("Um gestor precisa autorizar este cadastro.");
            autorizacao = Entrada.dadosLogin("E-mail do gestor autorizador", "Senha do gestor autorizador");
        }

        String nome = Entrada.texto("Nome completo");
        String identificacao = Entrada.texto("Identificação funcional fictícia");
        String email = Entrada.texto("E-mail de teste para login");
        String senha = Entrada.senha("Crie uma senha com pelo menos 8 caracteres");
        String setor = perfil == Perfil.GESTOR ? Entrada.texto("Setor de atuação (opcional)") : null;
        Long equipe = perfil == Perfil.AGENTE ? Entrada.id("ID da equipe (1 para a equipe piloto)") : null;
        var cadastro = new UsuarioBO.CadastroOperador(perfil, nome, email, senha, identificacao, setor, equipe);
        return usuarios.cadastrarOperador(cadastro, autorizacao);
    }
}

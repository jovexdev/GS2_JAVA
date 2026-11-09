package br.com.fiap.main;

import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.entities.Perfil;

public class TesteCadastrarUsuario {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            Perfil perfil = Perfil.valueOf(Entrada.escolher("Perfil para cadastro", "CIDADAO", "EMPRESA"));
            String nome = Entrada.texto(perfil == Perfil.EMPRESA ? "Nome da empresa" : "Nome do cidadão");
            String documento = Entrada.texto(perfil == Perfil.EMPRESA ? "CNPJ fictício" : "CPF fictício");
            String email = Entrada.texto("E-mail de teste para login");
            String endereco = Entrada.texto("Endereço fictício");
            String responsavel = perfil == Perfil.EMPRESA ? Entrada.texto("Nome do responsável") : null;
            String senha = Entrada.senha("Crie uma senha com pelo menos 8 caracteres");
            var cadastro = new UsuarioBO.Cadastro(perfil, nome, documento, email, endereco, responsavel, senha);
            var usuario = Entrada.usuarios().cadastrar(cadastro);
            System.out.println("Conta cadastrada com ID " + usuario.id() + " e e-mail " + usuario.email() + ".");
        });
    }
}

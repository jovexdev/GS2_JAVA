package br.com.fiap.main;

public class TesteCadastrarUsuario {
    public static void main(String[] args) {
        Entrada.executar(() -> CadastroTerminal.cadastrar(Entrada.usuarios()));
    }
}

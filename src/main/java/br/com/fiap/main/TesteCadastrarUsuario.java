package br.com.fiap.main;

public class TesteCadastrarUsuario {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var perfil = CadastroTerminal.escolherPerfil();
            if (perfil != null) {
                CadastroTerminal.cadastrar(Entrada.usuarios(), perfil);
            }
        });
    }
}

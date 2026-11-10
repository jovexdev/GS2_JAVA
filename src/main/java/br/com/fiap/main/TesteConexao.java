package br.com.fiap.main;

public class TesteConexao {
    public static void main(String[] args) {
        Entrada.executar(() -> BancoTerminal.verificar(Entrada.conexoes()));
    }
}

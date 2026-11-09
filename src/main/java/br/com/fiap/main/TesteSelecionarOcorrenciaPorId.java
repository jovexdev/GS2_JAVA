package br.com.fiap.main;

public class TesteSelecionarOcorrenciaPorId {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var usuario = Entrada.login();
            long id = Entrada.id("ID da ocorrência para consultar");
            System.out.println(Entrada.ocorrencias().consultar(id, usuario));
        });
    }
}

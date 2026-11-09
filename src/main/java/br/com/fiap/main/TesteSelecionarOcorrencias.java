package br.com.fiap.main;

public class TesteSelecionarOcorrencias {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var usuario = Entrada.login();
            var ocorrencias = Entrada.ocorrencias().listar(usuario);
            System.out.println("Ocorrências encontradas: " + ocorrencias.size());
            ocorrencias.forEach(System.out::println);
        });
    }
}

package br.com.fiap.main;

public class TesteInserirOcorrencia {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var usuario = Entrada.login();
            var ocorrencia = Entrada.ocorrencias().registrar(Entrada.relato(usuario.perfil()), usuario);
            System.out.println("Ocorrência cadastrada. Guarde o ID " + ocorrencia.id() + ".");
            System.out.println(ocorrencia);
        });
    }
}

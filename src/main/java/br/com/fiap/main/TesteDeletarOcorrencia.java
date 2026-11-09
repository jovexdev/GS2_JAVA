package br.com.fiap.main;

public class TesteDeletarOcorrencia {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var usuario = Entrada.login();
            long id = Entrada.id("ID da ocorrência de teste para excluir");
            var bo = Entrada.ocorrencias();
            System.out.println(bo.consultar(id, usuario));
            if (Entrada.confirmar("Excluir a ocorrência de teste " + id + "?")) {
                bo.excluir(id, usuario);
                System.out.println("Ocorrência excluída.");
            }
        });
    }
}

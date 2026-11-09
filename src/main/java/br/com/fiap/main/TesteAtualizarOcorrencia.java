package br.com.fiap.main;

public class TesteAtualizarOcorrencia {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            var usuario = Entrada.login();
            long id = Entrada.id("ID da ocorrência para editar");
            var bo = Entrada.ocorrencias();
            System.out.println("Antes: " + bo.consultar(id, usuario));
            var atualizada = bo.atualizar(id, Entrada.relato(usuario.perfil()), usuario);
            System.out.println("Ocorrência atualizada: " + atualizada);
        });
    }
}

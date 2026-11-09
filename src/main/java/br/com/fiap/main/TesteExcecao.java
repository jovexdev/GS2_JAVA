package br.com.fiap.main;

public class TesteExcecao {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            try {
                int numero = Integer.parseInt(Entrada.texto("Digite letras para testar NumberFormatException"));
                System.out.println("Número digitado: " + numero);
            } catch (NumberFormatException e) {
                System.out.println("Exceção tratada: o valor digitado não é um número inteiro.");
            } finally {
                System.out.println("Fim do teste de exceção.");
            }
        });
    }
}

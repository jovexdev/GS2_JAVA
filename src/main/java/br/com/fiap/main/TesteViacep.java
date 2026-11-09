package br.com.fiap.main;

public class TesteViacep {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            String cep = Entrada.texto("CEP para consulta").replace("-", "");
            if (!cep.matches("[0-9]{8}")) {
                throw new IllegalArgumentException("Informe um CEP de oito dígitos.");
            }
            Endereco endereco = ConsultaApiAula.buscar("https://viacep.com.br/ws/" + cep + "/json/", Endereco.class);
            if (endereco.erro()) {
                System.out.println("CEP não encontrado.");
            } else {
                System.out.println(endereco);
            }
        });
    }

    private record Endereco(String cep, String logradouro, String bairro, String localidade, String uf, boolean erro) {
    }
}

package br.com.fiap.main;

public class TesteSwPlaneta {
    public static void main(String[] args) {
        Entrada.executar(() -> {
            long id = Entrada.id("ID do planeta para consulta (exemplo: 1)");
            Planeta planeta = ConsultaApiAula.buscar("https://swapi.dev/api/planets/" + id + "/", Planeta.class);
            System.out.println(planeta);
        });
    }

    private record Planeta(String name, String climate, String gravity, String terrain) {
    }
}

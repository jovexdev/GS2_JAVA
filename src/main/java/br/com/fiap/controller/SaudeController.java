package br.com.fiap.controller;

import br.com.fiap.conexoes.ConexaoFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/saude")
public class SaudeController {
    private final ConexaoFactory conexoes;

    public SaudeController(ConexaoFactory conexoes) {
        this.conexoes = conexoes;
    }

    @GetMapping
    public Map<String, String> saude() {
        return Map.of("aplicacao", "AEGIS", "status", "OK");
    }

    @GetMapping("/oracle")
    public Map<String, String> oracle() throws SQLException {
        try (var conexao = conexoes.abrir();
             var stmt = conexao.prepareStatement("SELECT COUNT(*) FROM AEGIS_EQUIPE");
             var resultado = stmt.executeQuery()) {
            resultado.next();
            return Map.of("banco", "Oracle", "status", "OK");
        }
    }
}

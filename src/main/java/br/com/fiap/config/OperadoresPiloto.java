package br.com.fiap.config;

import br.com.fiap.bo.UsuarioBO;
import br.com.fiap.entities.Perfil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OperadoresPiloto implements ApplicationRunner {
    private final UsuarioBO usuarios;
    private final Environment ambiente;

    public OperadoresPiloto(UsuarioBO usuarios, Environment ambiente) {
        this.usuarios = usuarios;
        this.ambiente = ambiente;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        usuarios.prepararOperador(Perfil.GESTOR, ambiente.getRequiredProperty("aegis.gestor.email"),
                ambiente.getRequiredProperty("aegis.gestor.senha"), null);
        usuarios.prepararOperador(Perfil.AGENTE, ambiente.getRequiredProperty("aegis.agente.email"),
                ambiente.getRequiredProperty("aegis.agente.senha"), 1L);
    }
}

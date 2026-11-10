package br.com.fiap.conexoes;

import br.com.fiap.config.ConfiguracaoLocal;
import br.com.fiap.excecoes.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class ConexaoFactory {
    private static final String URL = "jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl";
    private static final String USUARIO = "Seu usuario Oracle";
    private static final String SENHA = "Sua senha Oracle";
    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver";

    private final String url;
    private final String usuario;
    private final String senha;
    private final String driver;

    public ConexaoFactory() {
        this(ConfiguracaoLocal.carregar());
    }

    private ConexaoFactory(ConfiguracaoLocal configuracao) {
        this(configuracao.valor("aegis.db.url", "DB_URL"),
                configuracao.valor("aegis.db.usuario", "DB_USUARIO"),
                configuracao.valor("aegis.db.senha", "DB_SENHA"), DRIVER);
    }

    @Autowired
    public ConexaoFactory(@Value("${aegis.db.url}") String url,
                          @Value("${aegis.db.usuario}") String usuario,
                          @Value("${aegis.db.senha}") String senha,
                          @Value("${aegis.db.driver}") String driver) {
        this.url = url.isBlank() ? URL : url;
        this.usuario = usuario.isBlank() ? USUARIO : usuario;
        this.senha = senha.isBlank() ? SENHA : senha;
        this.driver = driver;
    }

    public Connection conexao() throws ClassNotFoundException, SQLException {
        if (USUARIO.equals(usuario) || SENHA.equals(senha)) {
            throw ApiException.indisponivel("Informe o usuário e a senha Oracle ao iniciar AegisTerminal ou em aegis.local.properties.");
        }

        Class.forName(driver);
        return DriverManager.getConnection(url, usuario, senha);
    }

    public Connection abrir() throws SQLException {
        try {
            return conexao();
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC indisponível.", e);
        }
    }
}

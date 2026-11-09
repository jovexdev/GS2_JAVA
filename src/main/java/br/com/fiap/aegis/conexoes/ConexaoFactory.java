package br.com.fiap.aegis.conexoes;

import br.com.fiap.aegis.excecoes.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Component
public class ConexaoFactory {
    private final String url;
    private final String usuario;
    private final String senha;
    private final String driver;

    public ConexaoFactory(@Value("${aegis.db.url}") String url,
                          @Value("${aegis.db.usuario}") String usuario,
                          @Value("${aegis.db.senha}") String senha,
                          @Value("${aegis.db.driver}") String driver) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
        this.driver = driver;
    }

    public Connection conexao() throws SQLException {
        if (url.isBlank() || usuario.isBlank() || senha.isBlank()) {
            throw ApiException.indisponivel("Configure DB_USUARIO e DB_SENHA para acessar o banco.");
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC indisponível.", e);
        }

        Properties propriedades = new Properties();
        propriedades.setProperty("user", usuario);
        propriedades.setProperty("password", senha);
        propriedades.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        propriedades.setProperty("oracle.jdbc.ReadTimeout", "10000");
        return DriverManager.getConnection(url, propriedades);
    }
}

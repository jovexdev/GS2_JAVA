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
    private static final String USUARIO = "";
    private static final String SENHA = "";
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
        String usuarioJava = DRIVER.equals(driver) ? ConfiguracaoLocal.segredoJava("ORACLE_USUARIO") : "";
        String senhaJava = DRIVER.equals(driver) ? ConfiguracaoLocal.segredoJava("ORACLE_SENHA") : "";
        this.usuario = valorConfigurado(usuarioJava, usuario, USUARIO);
        this.senha = valorConfigurado(senhaJava, senha, SENHA);
        this.driver = driver;
    }

    private String valorConfigurado(String codigoLocal, String configuracao, String padrao) {
        if (!codigoLocal.isBlank()) {
            return codigoLocal;
        }
        return configuracao.isBlank() ? padrao : configuracao;
    }

    public Connection conexao() throws ClassNotFoundException, SQLException {
        if (usuario.isBlank() || senha.isBlank()
                || "Seu usuario Oracle".equals(usuario) || "Sua senha Oracle".equals(senha)) {
            throw ApiException.indisponivel("Configure ORACLE_USUARIO e ORACLE_SENHA em CredenciaisLocais.java.");
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

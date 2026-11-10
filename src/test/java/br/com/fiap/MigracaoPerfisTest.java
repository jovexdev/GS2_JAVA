package br.com.fiap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MigracaoPerfisTest {
    @Test
    void copiaPerfisLegadosSemAlterarIdsOuDuplicarDados() throws Exception {
        String roteiro = Files.readString(Path.of("database/02_migrar_perfis.sql"));
        List<String> criacoes = extrair(roteiro, "criar_tabela\\(q");
        List<String> copias = extrair(roteiro, "EXECUTE IMMEDIATE q");
        assertThat(criacoes).hasSize(4);
        assertThat(copias).hasSize(4);

        try (var conexao = DriverManager.getConnection("jdbc:h2:mem:migracao;MODE=Oracle");
             var stmt = conexao.createStatement()) {
            stmt.execute("CREATE TABLE AEGIS_EQUIPE (id NUMBER(19) PRIMARY KEY)");
            stmt.execute("""
                    CREATE TABLE AEGIS_USUARIO (
                        id NUMBER(19) PRIMARY KEY, perfil VARCHAR2(12), nome VARCHAR2(120),
                        documento VARCHAR2(14), endereco VARCHAR2(250), responsavel VARCHAR2(120), equipe_id NUMBER(19)
                    )
                    """);
            stmt.execute("CREATE TABLE AEGIS_OCORRENCIA (autor_id NUMBER(19) REFERENCES AEGIS_USUARIO(id))");
            stmt.execute("INSERT INTO AEGIS_EQUIPE VALUES (1)");
            stmt.execute("INSERT INTO AEGIS_USUARIO VALUES (11, 'CIDADAO', 'Cidadã', '12345678909', 'Rua A', NULL, NULL)");
            stmt.execute("INSERT INTO AEGIS_USUARIO VALUES (12, 'EMPRESA', 'Empresa', '11222333000181', 'Rua B', 'Pessoa', NULL)");
            stmt.execute("INSERT INTO AEGIS_USUARIO VALUES (13, 'GESTOR', 'Gestora', NULL, NULL, NULL, NULL)");
            stmt.execute("INSERT INTO AEGIS_USUARIO VALUES (14, 'AGENTE', 'Agente', NULL, NULL, NULL, 1)");
            stmt.execute("INSERT INTO AEGIS_OCORRENCIA VALUES (11)");

            for (String sql : criacoes) {
                stmt.execute(sql);
            }
            for (String sql : copias) {
                assertThat(stmt.executeUpdate(sql)).isEqualTo(1);
                assertThat(stmt.executeUpdate(sql)).isZero();
            }
            conferir(stmt, "SELECT c.cpf FROM AEGIS_CIDADAO c JOIN AEGIS_OCORRENCIA o ON o.autor_id = c.usuario_id",
                    "12345678909");
            conferir(stmt, "SELECT responsavel FROM AEGIS_EMPRESA WHERE usuario_id = 12", "Pessoa");
            conferir(stmt, "SELECT nome FROM AEGIS_GESTOR WHERE usuario_id = 13", "Gestora");
            conferir(stmt, "SELECT equipe_id FROM AEGIS_AGENTE WHERE usuario_id = 14", "1");
        }
    }

    private List<String> extrair(String roteiro, String comando) {
        var partes = Pattern.compile(comando + "'\\[\\s*(.*?)\\s*\\]'", Pattern.DOTALL).matcher(roteiro);
        List<String> sql = new ArrayList<>();
        while (partes.find()) {
            sql.add(partes.group(1));
        }
        return sql;
    }

    private void conferir(Statement stmt, String sql, String valorEsperado) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo(valorEsperado);
        }
    }
}

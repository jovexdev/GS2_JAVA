package br.com.fiap.excecoes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;

@RestControllerAdvice
public class TratadorExcecoes extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TratadorExcecoes.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail tratarApi(ApiException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(SQLException.class)
    public ProblemDetail tratarSql(SQLException e) {
        int codigo = e.getErrorCode();
        if (codigo == 1 || codigo == 2291 || codigo == 2292 || codigo == 23505 || codigo == 23503
                || codigo == 23506) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                    "A operação conflita com um registro existente ou vinculado.");
        }

        LOG.error("Falha no banco. Código JDBC: {}. SQLState: {}.", codigo, e.getSQLState());
        if (codigo == 904) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                    "Confira a atualização da tabela AEGIS_VISTORIA indicada no README.");
        }
        if (codigo == 942) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                    "Confira se executou database/01_schema.sql no mesmo usuário Oracle.");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Falha ao acessar o banco de dados. Confira a conexão Oracle.");
    }
}

-- Execute com F5 no SQL Developer se já criou o esquema com o 01_schema.sql antigo.
-- As contas e os identificadores usados por ocorrências, vistorias e histórico são preservados.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    PROCEDURE criar_tabela(sequencia_sql VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE sequencia_sql;
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> -955 THEN
                RAISE;
            END IF;
    END;
BEGIN
    criar_tabela(q'[
        CREATE TABLE AEGIS_CIDADAO (
            usuario_id NUMBER(19) PRIMARY KEY REFERENCES AEGIS_USUARIO(id),
            nome VARCHAR2(120 CHAR) NOT NULL,
            cpf VARCHAR2(11 CHAR) NOT NULL UNIQUE,
            endereco VARCHAR2(250 CHAR) NOT NULL,
            ocupacao VARCHAR2(100 CHAR),
            telefone VARCHAR2(30 CHAR)
        )
    ]');

    criar_tabela(q'[
        CREATE TABLE AEGIS_EMPRESA (
            usuario_id NUMBER(19) PRIMARY KEY REFERENCES AEGIS_USUARIO(id),
            razao_social VARCHAR2(120 CHAR) NOT NULL,
            nome_fantasia VARCHAR2(120 CHAR),
            cnpj VARCHAR2(14 CHAR) NOT NULL UNIQUE,
            atividade_economica VARCHAR2(150 CHAR),
            endereco VARCHAR2(250 CHAR) NOT NULL,
            responsavel VARCHAR2(120 CHAR) NOT NULL,
            telefone VARCHAR2(30 CHAR),
            caracteristicas_instalacoes VARCHAR2(500 CHAR)
        )
    ]');

    criar_tabela(q'[
        CREATE TABLE AEGIS_GESTOR (
            usuario_id NUMBER(19) PRIMARY KEY REFERENCES AEGIS_USUARIO(id),
            nome VARCHAR2(120 CHAR) NOT NULL,
            identificacao_funcional VARCHAR2(40 CHAR) UNIQUE,
            setor VARCHAR2(120 CHAR)
        )
    ]');

    criar_tabela(q'[
        CREATE TABLE AEGIS_AGENTE (
            usuario_id NUMBER(19) PRIMARY KEY REFERENCES AEGIS_USUARIO(id),
            nome VARCHAR2(120 CHAR) NOT NULL,
            identificacao_funcional VARCHAR2(40 CHAR) UNIQUE,
            equipe_id NUMBER(19) NOT NULL REFERENCES AEGIS_EQUIPE(id)
        )
    ]');
END;
/

DECLARE
    colunas_legadas NUMBER;
    contas_sem_perfil NUMBER;
BEGIN
    SELECT COUNT(*) INTO colunas_legadas
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'AEGIS_USUARIO' AND COLUMN_NAME = 'NOME';

    IF colunas_legadas > 0 THEN
        LOCK TABLE AEGIS_USUARIO IN EXCLUSIVE MODE;
        EXECUTE IMMEDIATE q'[
            INSERT INTO AEGIS_CIDADAO (usuario_id, nome, cpf, endereco)
            SELECT id, nome, documento, endereco
            FROM AEGIS_USUARIO u
            WHERE u.perfil = 'CIDADAO'
              AND NOT EXISTS (SELECT 1 FROM AEGIS_CIDADAO p WHERE p.usuario_id = u.id)
        ]';

        EXECUTE IMMEDIATE q'[
            INSERT INTO AEGIS_EMPRESA (usuario_id, razao_social, cnpj, endereco, responsavel)
            SELECT id, nome, documento, endereco, responsavel
            FROM AEGIS_USUARIO u
            WHERE u.perfil = 'EMPRESA'
              AND NOT EXISTS (SELECT 1 FROM AEGIS_EMPRESA p WHERE p.usuario_id = u.id)
        ]';

        EXECUTE IMMEDIATE q'[
            INSERT INTO AEGIS_GESTOR (usuario_id, nome)
            SELECT id, nome
            FROM AEGIS_USUARIO u
            WHERE u.perfil = 'GESTOR'
              AND NOT EXISTS (SELECT 1 FROM AEGIS_GESTOR p WHERE p.usuario_id = u.id)
        ]';

        EXECUTE IMMEDIATE q'[
            INSERT INTO AEGIS_AGENTE (usuario_id, nome, equipe_id)
            SELECT id, nome, equipe_id
            FROM AEGIS_USUARIO u
            WHERE u.perfil = 'AGENTE'
              AND NOT EXISTS (SELECT 1 FROM AEGIS_AGENTE p WHERE p.usuario_id = u.id)
        ]';

        SELECT COUNT(*) INTO contas_sem_perfil
        FROM AEGIS_USUARIO u
        LEFT JOIN AEGIS_CIDADAO c ON c.usuario_id = u.id AND u.perfil = 'CIDADAO'
        LEFT JOIN AEGIS_EMPRESA e ON e.usuario_id = u.id AND u.perfil = 'EMPRESA'
        LEFT JOIN AEGIS_GESTOR g ON g.usuario_id = u.id AND u.perfil = 'GESTOR'
        LEFT JOIN AEGIS_AGENTE a ON a.usuario_id = u.id AND u.perfil = 'AGENTE'
        WHERE COALESCE(c.usuario_id, e.usuario_id, g.usuario_id, a.usuario_id) IS NULL;

        IF contas_sem_perfil > 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Migração incompleta: há contas sem cadastro de perfil.');
        END IF;

        COMMIT;
        EXECUTE IMMEDIATE
            'ALTER TABLE AEGIS_USUARIO DROP (nome, documento, endereco, responsavel, equipe_id) CASCADE CONSTRAINTS';
    END IF;
END;
/

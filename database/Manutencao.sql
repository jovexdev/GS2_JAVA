-- AEGIS | MANUTENÇÃO OPCIONAL DO BANCO ORACLE
-- Pare a aplicação e use o mesmo usuário Oracle das tabelas AEGIS.
-- Altere somente v_acao abaixo e execute este arquivo com F5 no SQL Developer.
--
-- NENHUMA         = mostra as opções, sem alterar o banco (padrão).
-- APAGAR_DADOS    = apaga os registros, mantendo tabelas, chaves e índices.
--                  Recria somente a Equipe piloto (ID 1), necessária ao AEGIS.
--                  Remove também contas e equipes de demonstração.
--                  Os contadores dos IDs não são reiniciados.
-- APAGAR_TABELAS  = exclui as nove tabelas AEGIS e seus dados.
--                  Depois, execute 01_schema.sql uma vez para reinstalar.
--
-- A exclusão de tabelas usa DDL e não pode ser desfeita com ROLLBACK.
-- As duas operações são alternativas: somente a ação escolhida será executada.

SET SERVEROUTPUT ON;

DECLARE
    v_acao CONSTANT VARCHAR2(20) := 'NENHUMA';

    PROCEDURE apagar_tabela(p_nome VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'DROP TABLE ' || p_nome;
        DBMS_OUTPUT.PUT_LINE('Tabela excluída: ' || p_nome);
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -942 THEN
                DBMS_OUTPUT.PUT_LINE('Tabela já ausente: ' || p_nome);
            ELSE
                RAISE;
            END IF;
    END;
BEGIN
    IF v_acao = 'APAGAR_DADOS' THEN
        SAVEPOINT antes_da_limpeza_aegis;
        BEGIN
            -- Primeiro os registros dependentes, depois os cadastros e as equipes.
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_HISTORICO';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_VISTORIA';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_OCORRENCIA';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_AGENTE';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_GESTOR';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_EMPRESA';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_CIDADAO';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_USUARIO';
            EXECUTE IMMEDIATE 'DELETE FROM AEGIS_EQUIPE';

            EXECUTE IMMEDIATE 'INSERT INTO AEGIS_EQUIPE (id, nome) VALUES (1, ''Equipe piloto'')';
            COMMIT;
            DBMS_OUTPUT.PUT_LINE('Dados apagados. Estrutura mantida e Equipe piloto recriada.');
        EXCEPTION
            WHEN OTHERS THEN
                ROLLBACK TO antes_da_limpeza_aegis;
                RAISE;
        END;

    ELSIF v_acao = 'APAGAR_TABELAS' THEN
        -- A ordem respeita as chaves estrangeiras, inclusive o agente da vistoria.
        -- Uma tabela que já não existe é ignorada; outros erros interrompem a ação.
        apagar_tabela('AEGIS_HISTORICO');
        apagar_tabela('AEGIS_VISTORIA');
        apagar_tabela('AEGIS_OCORRENCIA');
        apagar_tabela('AEGIS_AGENTE');
        apagar_tabela('AEGIS_GESTOR');
        apagar_tabela('AEGIS_EMPRESA');
        apagar_tabela('AEGIS_CIDADAO');
        apagar_tabela('AEGIS_USUARIO');
        apagar_tabela('AEGIS_EQUIPE');
        DBMS_OUTPUT.PUT_LINE('Tabelas AEGIS excluídas. Execute 01_schema.sql para reinstalar.');

    ELSIF v_acao = 'NENHUMA' THEN
        DBMS_OUTPUT.PUT_LINE('Nenhuma alteração realizada.');
        DBMS_OUTPUT.PUT_LINE('Escolha APAGAR_DADOS ou APAGAR_TABELAS em v_acao e execute novamente.');

    ELSE
        RAISE_APPLICATION_ERROR(-20002, 'Ação inválida. Use NENHUMA, APAGAR_DADOS ou APAGAR_TABELAS.');
    END IF;
END;
/

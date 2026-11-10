-- Carga opcional de demonstração. Execute com F5 após o 01_schema.sql.
-- As contas usam a senha de demonstração AegisDemo2026.
-- Todos os cadastros, locais, relatos e atendimentos deste script são fictícios.

DECLARE
    v_hoje TIMESTAMP := CAST(TRUNC(CAST(LOCALTIMESTAMP AS DATE)) AS TIMESTAMP);
    v_cidadao AEGIS_USUARIO.id%TYPE;
    v_empresa AEGIS_USUARIO.id%TYPE;
    v_gestor AEGIS_USUARIO.id%TYPE;
    v_agente_norte AEGIS_USUARIO.id%TYPE;
    v_agente_sul AEGIS_USUARIO.id%TYPE;
    v_ocorrencia_pendente AEGIS_OCORRENCIA.id%TYPE;
    v_ocorrencia_agendada AEGIS_OCORRENCIA.id%TYPE;
    v_ocorrencia_concluida AEGIS_OCORRENCIA.id%TYPE;
    v_vistoria_pendente AEGIS_VISTORIA.id%TYPE;
    v_vistoria_agendada AEGIS_VISTORIA.id%TYPE;
    v_vistoria_concluida AEGIS_VISTORIA.id%TYPE;
BEGIN
    SAVEPOINT aegis_carga_ficticia;

    -- Início da carga
    INSERT INTO AEGIS_EQUIPE (id, nome) VALUES (9101, 'Equipe Demo Norte');
    INSERT INTO AEGIS_EQUIPE (id, nome) VALUES (9102, 'Equipe Demo Sul');

    -- Contas de acesso com hashes compatíveis com o login do AEGIS
    INSERT INTO AEGIS_USUARIO (perfil, email, senha_hash)
    VALUES ('CIDADAO', 'cidadao@demo.aegis.test',
            'REEedHTvQOyDr8s5lDAP8g==:YQAA1qMkGUKOPf37IGR33IR3h7fdWGBRLeNqALs2AdM=')
    RETURNING id INTO v_cidadao;

    INSERT INTO AEGIS_USUARIO (perfil, email, senha_hash)
    VALUES ('EMPRESA', 'empresa@demo.aegis.test',
            'PVltmfP8LsZEG7KkVYrwQQ==:cJ+XxQTrPd5jPmkdYOjqY+s/vt5vyrrgjqkmHk53yKY=')
    RETURNING id INTO v_empresa;

    INSERT INTO AEGIS_USUARIO (perfil, email, senha_hash)
    VALUES ('GESTOR', 'gestor@demo.aegis.test',
            '8SYXWqCF5/rkXDxZpiZ/nQ==:cxOYqOZFa9e3bsRvvhu/jaGaIUszBiZsllarAjmx1/M=')
    RETURNING id INTO v_gestor;

    INSERT INTO AEGIS_USUARIO (perfil, email, senha_hash)
    VALUES ('AGENTE', 'agente.norte@demo.aegis.test',
            'ciIco2CVjRqduw1CHFzd+Q==:+zOuhqTDzGDVA5mgeEmGrcBmVS0blRrR3x5pUR+Vm7Q=')
    RETURNING id INTO v_agente_norte;

    INSERT INTO AEGIS_USUARIO (perfil, email, senha_hash)
    VALUES ('AGENTE', 'agente.sul@demo.aegis.test',
            'SDw+YZR6IDmMURDLPigK6Q==:mmDch5kranmnM9JZNb/+whywf4AyAI83KDzZDV1L1Zs=')
    RETURNING id INTO v_agente_sul;

    -- Cadastros dos quatro perfis
    INSERT INTO AEGIS_CIDADAO (usuario_id, nome, cpf, endereco, ocupacao, telefone)
    VALUES (v_cidadao, 'Marina Exemplo', '00000000001', 'Rua Fictícia A, 10, São Paulo/SP',
            'Estudante', '(11) 0000-0001');

    INSERT INTO AEGIS_EMPRESA
        (usuario_id, razao_social, nome_fantasia, cnpj, atividade_economica, endereco,
         responsavel, telefone, caracteristicas_instalacoes)
    VALUES (v_empresa, 'Armazém Demonstração Ltda.', 'Armazém Demo', 'AEGISDEMO00101',
            'Armazenamento de materiais', 'Rua Fictícia B, 20, São Paulo/SP',
            'Paulo Exemplo', '(11) 0000-0002', 'Pátio externo, depósito coberto e reservatório de água.');

    INSERT INTO AEGIS_GESTOR (usuario_id, nome, identificacao_funcional, setor)
    VALUES (v_gestor, 'Renata Exemplo', 'DEMO-GESTOR-01', 'Centro de Operações');

    INSERT INTO AEGIS_AGENTE (usuario_id, nome, identificacao_funcional, equipe_id)
    VALUES (v_agente_norte, 'Alex Exemplo', 'DEMO-AGENTE-01', 9101);

    INSERT INTO AEGIS_AGENTE (usuario_id, nome, identificacao_funcional, equipe_id)
    VALUES (v_agente_sul, 'Beatriz Exemplo', 'DEMO-AGENTE-02', 9102);

    -- Relato do cidadão
    INSERT INTO AEGIS_OCORRENCIA (autor_id, descricao, localizacao, criada_em)
    VALUES (v_cidadao, 'Recipientes com água perto de uma praça fictícia.',
            'Praça Fictícia, São Paulo/SP', v_hoje - INTERVAL '2' DAY + INTERVAL '15' HOUR);

    -- Relatos do estabelecimento com as três classificações preliminares
    INSERT INTO AEGIS_OCORRENCIA
        (autor_id, descricao, localizacao, agua_parada, reservatorio_aberto, materiais_expostos,
         prioridade_preliminar, justificativa, criada_em)
    VALUES (v_empresa, 'Reservatório aberto e água parada no pátio.', 'Pátio do Armazém Demo',
            1, 1, 0, 'CRITICA', 'Fatores declarados: água parada, reservatório aberto.',
            v_hoje - INTERVAL '1' DAY + INTERVAL '12' HOUR)
    RETURNING id INTO v_ocorrencia_pendente;

    INSERT INTO AEGIS_OCORRENCIA
        (autor_id, descricao, localizacao, agua_parada, reservatorio_aberto, materiais_expostos,
         prioridade_preliminar, justificativa, criada_em)
    VALUES (v_empresa, 'Água parada em uma bandeja perto da doca.', 'Doca do Armazém Demo',
            1, 0, 0, 'INSTAVEL', 'Fatores declarados: água parada.',
            v_hoje - INTERVAL '4' DAY + INTERVAL '9' HOUR)
    RETURNING id INTO v_ocorrencia_agendada;

    INSERT INTO AEGIS_OCORRENCIA
        (autor_id, descricao, localizacao, agua_parada, reservatorio_aberto, materiais_expostos,
         prioridade_preliminar, justificativa, criada_em)
    VALUES (v_empresa, 'Verificação preventiva no depósito coberto.', 'Depósito do Armazém Demo',
            0, 0, 0, 'OK', 'Sem fatores de prioridade declarados.',
            v_hoje - INTERVAL '9' DAY + INTERVAL '9' HOUR)
    RETURNING id INTO v_ocorrencia_concluida;

    -- Uma vistoria aguardando revisão, uma agendada e uma concluída
    INSERT INTO AEGIS_VISTORIA (ocorrencia_id, status)
    VALUES (v_ocorrencia_pendente, 'PENDENTE')
    RETURNING id INTO v_vistoria_pendente;

    INSERT INTO AEGIS_VISTORIA (ocorrencia_id, equipe_id, agente_id, status, prioridade, agendada_para, versao)
    VALUES (v_ocorrencia_agendada, 9101, v_agente_norte, 'AGENDADA', 'INSTAVEL',
            v_hoje + INTERVAL '2' DAY + INTERVAL '10' HOUR, 3)
    RETURNING id INTO v_vistoria_agendada;

    INSERT INTO AEGIS_VISTORIA
        (ocorrencia_id, equipe_id, agente_id, status, prioridade, agendada_para, resultado,
         inspecao, recomendacoes, versao)
    VALUES (v_ocorrencia_concluida, 9102, v_agente_sul, 'CONCLUIDA', 'OK',
            v_hoje - INTERVAL '5' DAY + INTERVAL '9' HOUR, 'Vistoria simulada concluída.',
            'Depósito organizado e sem acúmulo de água no momento da vistoria.',
            'Manter os reservatórios tampados e revisar o pátio após chuvas.', 7)
    RETURNING id INTO v_vistoria_concluida;

    -- Histórico da vistoria pendente
    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_pendente, v_empresa, 'SOLICITADA', 'Vistoria solicitada.',
            v_hoje - INTERVAL '1' DAY + INTERVAL '13' HOUR);

    -- Histórico da vistoria agendada
    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_agendada, v_empresa, 'SOLICITADA', 'Vistoria solicitada.',
            v_hoje - INTERVAL '4' DAY + INTERVAL '10' HOUR);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_agendada, v_gestor, 'REVISADA', 'INSTAVEL: Confirmar o relato na doca.',
            v_hoje - INTERVAL '3' DAY + INTERVAL '9' HOUR);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_agendada, v_gestor, 'DESIGNADA', 'Agente designado na equipe Demo Norte.',
            v_hoje - INTERVAL '3' DAY + INTERVAL '9' HOUR + INTERVAL '30' MINUTE);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_agendada, v_gestor, 'AGENDADA', 'Atendimento agendado para a equipe Demo Norte.',
            v_hoje - INTERVAL '3' DAY + INTERVAL '10' HOUR);

    -- Histórico da vistoria concluída
    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_empresa, 'SOLICITADA', 'Vistoria solicitada.',
            v_hoje - INTERVAL '9' DAY + INTERVAL '10' HOUR);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_gestor, 'REVISADA', 'OK: Atendimento preventivo.',
            v_hoje - INTERVAL '8' DAY + INTERVAL '10' HOUR);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_gestor, 'DESIGNADA', 'Agente designado na equipe Demo Sul.',
            v_hoje - INTERVAL '8' DAY + INTERVAL '10' HOUR + INTERVAL '30' MINUTE);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_gestor, 'AGENDADA', 'Atendimento agendado para a equipe Demo Sul.',
            v_hoje - INTERVAL '8' DAY + INTERVAL '11' HOUR);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_agente_sul, 'INICIADA', 'Atendimento de campo iniciado.',
            v_hoje - INTERVAL '5' DAY + INTERVAL '9' HOUR);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_agente_sul, 'INSPECAO',
            'Depósito organizado e sem acúmulo de água no momento da vistoria.',
            v_hoje - INTERVAL '5' DAY + INTERVAL '9' HOUR + INTERVAL '20' MINUTE);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_agente_sul, 'RECOMENDACOES',
            'Manter os reservatórios tampados e revisar o pátio após chuvas.',
            v_hoje - INTERVAL '5' DAY + INTERVAL '9' HOUR + INTERVAL '30' MINUTE);

    INSERT INTO AEGIS_HISTORICO (vistoria_id, autor_id, acao, detalhe, registrado_em)
    VALUES (v_vistoria_concluida, v_agente_sul, 'CONCLUIDA', 'Vistoria simulada concluída.',
            v_hoje - INTERVAL '5' DAY + INTERVAL '10' HOUR);
    -- Fim da carga

    COMMIT;
EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        ROLLBACK TO aegis_carga_ficticia;
        RAISE_APPLICATION_ERROR(-20001, 'A carga fictícia já existe ou conflita com um cadastro existente.');
    WHEN OTHERS THEN
        ROLLBACK TO aegis_carga_ficticia;
        RAISE;
END;
/

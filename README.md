# AEGIS

Backend Java da Global Solution 2026.2. O `AegisTerminal` inicia o Spring Boot e abre o acesso aos perfis do sistema. O menu e as rotas REST compartilham as mesmas regras e o banco Oracle da FIAP.

## Atualizar o Oracle

Pare o AEGIS antes de alterar o esquema. Na pasta `GS2_JAVA`, baixe a versão atualizada.

```powershell
git pull --ff-only
```

Para uma base nova, abra [`database/01_schema.sql`](database/01_schema.sql) no SQL Developer e execute com **F5** no seu usuário Oracle. Ele cria as tabelas, as chaves e a equipe piloto.

Se as tabelas já existem, execute somente o bloco da seção **Atualizar a tabela de vistorias existente**. Ele preserva os dados e habilita a designação individual e as etapas de campo.

| Tabela | O que guarda |
| --- | --- |
| `AEGIS_USUARIO` | Identificador, e-mail, hash da senha e perfil da conta |
| `AEGIS_CIDADAO` | Nome, CPF, endereço, ocupação e telefone |
| `AEGIS_EMPRESA` | Razão social, CNPJ, nome fantasia, atividade, endereço, responsável, telefone e características das instalações |
| `AEGIS_GESTOR` | Nome, identificação funcional fictícia e setor |
| `AEGIS_AGENTE` | Nome, identificação funcional fictícia e equipe |

## Atualizar a tabela de vistorias existente

Com o AEGIS parado, execute este bloco com **F5** no SQL Developer, no mesmo usuário Oracle das tabelas AEGIS. Use-o uma vez se a sua tabela de vistorias foi criada pela versão anterior. Para uma base nova, o `01_schema.sql` já inclui esses campos.

```sql
ALTER TABLE AEGIS_VISTORIA ADD (
    agente_id NUMBER(19),
    inspecao VARCHAR2(1000 CHAR),
    recomendacoes VARCHAR2(1000 CHAR)
);

ALTER TABLE AEGIS_VISTORIA ADD CONSTRAINT AEGIS_VISTORIA_AGENTE_FK
    FOREIGN KEY (agente_id) REFERENCES AEGIS_AGENTE(usuario_id);

ALTER TABLE AEGIS_VISTORIA MODIFY (status VARCHAR2(14 CHAR));

ALTER TABLE AEGIS_VISTORIA DROP CONSTRAINT AEGIS_VISTORIA_STATUS_CK;

ALTER TABLE AEGIS_VISTORIA ADD CONSTRAINT AEGIS_VISTORIA_STATUS_CK
    CHECK (status IN ('PENDENTE', 'AGENDADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'ENCERRADA'));
```

As vistorias anteriores mantêm o vínculo com a equipe. Quando um agente da equipe inicia um atendimento antigo sem agente individual, a vistoria passa a ser vinculada a ele.

## Carregar dados fictícios

Depois de instalar o esquema, execute [`database/DadosFicticios.sql`](database/DadosFicticios.sql) com **F5** para popular as tabelas. A carga cria duas equipes, cinco contas, quatro ocorrências e três vistorias com histórico. As vistorias ficam pendente, agendada e concluída, respectivamente.

As cinco contas usam a senha de demonstração **`AegisDemo2026`**.

| Perfil | E-mail |
| --- | --- |
| Cidadão | `cidadao@demo.aegis.test` |
| Empresa | `empresa@demo.aegis.test` |
| Gestor | `gestor@demo.aegis.test` |
| Agente da equipe Norte | `agente.norte@demo.aegis.test` |
| Agente da equipe Sul | `agente.sul@demo.aegis.test` |

A empresa vê as três vistorias. O agente da equipe Norte vê a agendada e o da equipe Sul vê a concluída. O cidadão pode consultar a própria ocorrência. Execute a carga uma vez por base. Se um identificador já existir, o script desfaz a carga e apresenta o conflito.

## Preencher as credenciais uma vez

Abra [`ConexaoFactory.java`](src/main/java/br/com/fiap/conexoes/ConexaoFactory.java) e preencha `USUARIO` e `SENHA` entre as aspas, no início da classe.

```java
private static final String USUARIO = "seu_usuario_oracle";
private static final String SENHA = "sua_senha_oracle";
```

A conexão usa `DriverManager.getConnection(url, usuario, senha)`. A URL padrão é `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl`.

Abra [`OpenAiService.java`](src/main/java/br/com/fiap/services/OpenAiService.java) e preencha `OPENAI_API_KEY` no início da classe.

```java
private static final String OPENAI_API_KEY = "sua_chave_openai";
```

Preencha os valores apenas na sua cópia local e mantenha esses arquivos fora dos commits enquanto contiverem credenciais. Os campos preenchidos no código têm prioridade. Com os campos vazios, a aplicação pode usar `aegis.local.properties` ou as variáveis de ambiente. Na abertura do terminal, a linha `OpenAI` indica de onde veio a chave.

## Rodar no IntelliJ

Abra `GS2_JAVA` como projeto Maven com **JDK 21** e rode na raiz do projeto.

```powershell
.\mvnw.cmd clean spring-boot:run
```

Você também pode executar o `main()` de [`AegisTerminal.java`](src/main/java/br/com/fiap/main/AegisTerminal.java). O Spring Boot escolhe uma porta livre e imprime o endereço da API. Selecione seu perfil e escolha **Entrar** ou **Criar cadastro**.

| Perfil | O que aparece após entrar |
| --- | --- |
| **Cidadão (CPF)** | GeoRadar com resumo municipal e link para o mapa do InfoDengue, gráfico de casos em barras, previsão, LIA, registro e acompanhamento de ocorrências |
| **Empresa (CNPJ)** | Edição dos dados do estabelecimento, relato e classificação com justificativa, solicitação de vistoria, acompanhamento e histórico |
| **Gestor** | Análise dos relatos de cidadãos e empresas, decisão sobre vistoria, revisão de prioridade, designação de agente, agendamento, acompanhamento, resultados e encerramento |
| **Agente** | Vistorias atribuídas, local e relato, início, inspeção, recomendações, conclusão de campo e histórico |

O gestor pode encaminhar um relato para vistoria ou encerrá-lo com justificativa. Após revisar a prioridade, designe o agente e agende o atendimento. O agente registra a inspeção e as orientações durante o atendimento. O solicitante vê a inspeção e as recomendações após a conclusão de campo.

Use dados fictícios nos cadastros. O primeiro gestor pode ser criado pelo terminal local. Depois disso, um gestor autoriza os cadastros de novos gestores e agentes. Para cadastrar um agente na equipe piloto, informe o ID `1`. O cadastro público da API atende cidadãos e empresas.

## Testar partes separadamente

As classes ficam no [pacote `main`](src/main/java/br/com/fiap/main). `TesteConexao` verifica o Oracle e a equipe piloto. `TesteCadastrarUsuario` abre a escolha de perfil. Os testes de ocorrências e vistorias pedem o login da conta AEGIS para aplicar as permissões. `TesteLia` usa a chave definida em `OpenAiService.java`.

Para rodar apenas a API REST, execute `br.com.fiap.AegisApplication`. No modo terminal, use o endereço impresso para consultar `/api/saude` e `/api/saude/oracle`. Ao iniciar `AegisApplication` diretamente, a porta padrão é 8080.

Os testes automatizados rodam com `.\mvnw.cmd test`. Eles usam H2 em modo Oracle, exercitam o fluxo de vistorias e as regras de autorização.

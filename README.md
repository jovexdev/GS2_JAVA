# AEGIS

Backend Java da Global Solution 2026.2. O `AegisTerminal` inicia o Spring Boot e abre o acesso aos perfis do sistema. O menu e as rotas REST compartilham as mesmas regras e o banco Oracle da FIAP.

## Atualizar o Oracle

Pare o AEGIS antes de alterar o esquema. Na pasta `GS2_JAVA`, baixe a versão atualizada.

```powershell
git pull --ff-only
```

Abra [`database/01_schema.sql`](database/01_schema.sql) no SQL Developer e execute com **F5** no seu usuário Oracle. Esse é o script de instalação. Ele cria as tabelas, as chaves e a equipe piloto.

Para substituir um esquema AEGIS antigo, exclua primeiro as tabelas AEGIS desse usuário. A exclusão remove os cadastros, as ocorrências, as vistorias e o histórico existentes.

| Tabela | O que guarda |
| --- | --- |
| `AEGIS_USUARIO` | Identificador, e-mail, hash da senha e perfil da conta |
| `AEGIS_CIDADAO` | Nome, CPF, endereço, ocupação e telefone |
| `AEGIS_EMPRESA` | Razão social, CNPJ, nome fantasia, atividade, endereço, responsável, telefone e características das instalações |
| `AEGIS_GESTOR` | Nome, identificação funcional fictícia e setor |
| `AEGIS_AGENTE` | Nome, identificação funcional fictícia e equipe |

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
| **Cidadão (CPF)** | Ocorrências próprias, indicadores, previsão e LIA |
| **Empresa (CNPJ)** | Ocorrências do estabelecimento, classificação preliminar, solicitação e acompanhamento de vistorias, indicadores, previsão e LIA |
| **Gestor** | Registros ambientais, revisão de prioridade, agendamento e acompanhamento de vistorias |
| **Agente** | Atendimentos da equipe, consulta ao relato, registro do resultado e conclusão da vistoria |

Use dados fictícios nos cadastros. O primeiro gestor pode ser criado pelo terminal local. Depois disso, um gestor autoriza os cadastros de novos gestores e agentes. Para cadastrar um agente na equipe piloto, informe o ID `1`. O cadastro público da API atende cidadãos e empresas.

## Testar partes separadamente

As classes ficam no [pacote `main`](src/main/java/br/com/fiap/main). `TesteConexao` verifica o Oracle e a equipe piloto. `TesteCadastrarUsuario` abre a escolha de perfil. Os testes de ocorrências e vistorias pedem o login da conta AEGIS para aplicar as permissões. `TesteLia` usa a chave definida em `OpenAiService.java`.

Para rodar apenas a API REST, execute `br.com.fiap.AegisApplication`. No modo terminal, use o endereço impresso para consultar `/api/saude` e `/api/saude/oracle`. Ao iniciar `AegisApplication` diretamente, a porta padrão é 8080.

Os testes automatizados rodam com `.\mvnw.cmd test`. Eles usam H2 em modo Oracle, exercitam o fluxo de vistorias e as regras de autorização.

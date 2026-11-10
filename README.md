# AEGIS

Backend Java da Global Solution 2026.2. O `AegisTerminal` inicia o Spring Boot e abre um menu para operar o sistema pelo console. O menu e as rotas REST usam as mesmas regras e o mesmo banco Oracle da FIAP.

## Rodar no IntelliJ

Abra a pasta `GS2_JAVA` como projeto Maven com **JDK 21**. O `pom.xml` já está preparado. Se já clonou o projeto, pare a aplicação anterior e atualize a pasta.

```powershell
git pull --ff-only
.\mvnw.cmd clean spring-boot:run
```

Você também pode abrir [`AegisTerminal.java`](src/main/java/br/com/fiap/main/AegisTerminal.java) e clicar no triângulo verde ao lado de `main()`. Confira se o **Working directory** é a pasta do `pom.xml`.

Na inicialização, informe o **usuário e a senha do Oracle FIAP** quando forem solicitados. A **chave OpenAI** também pode ser digitada nessa etapa. Pressione Enter no campo da chave para continuar sem a LIA. O Spring Boot abre a API REST na porta 8080 enquanto o menu estiver aberto.

Use a opção `1` para testar o Oracle. O resultado esperado inclui `Conectado ao Oracle da FIAP.` e a equipe piloto. Se aparecer que a tabela não existe, execute [`database/01_schema.sql`](database/01_schema.sql) no SQL Developer com F5 usando o mesmo usuário que informou no terminal. Execute o script uma vez por esquema.

## Usar o menu

| Opção | O que faz |
| --- | --- |
| `1` | Testa o Oracle e lê a equipe piloto |
| `2` e `3` | Cadastra cidadão ou empresa fictícia e entra na conta AEGIS |
| `4` | Registra, lista, busca, atualiza e exclui ocorrências conforme o perfil |
| `5` | Solicita, acompanha, revisa, agenda ou conclui vistorias conforme o perfil |
| `6` e `7` | Consulta os indicadores do InfoDengue e a previsão acadêmica |
| `8` | Envia uma pergunta à LIA usando a OpenAI |
| `9` | Cria uma conta interna piloto de gestor ou agente |
| `10` e `0` | Sai da conta ou encerra o AEGIS e o Spring Boot |

Para testar a gravação, rode `1`, cadastre uma empresa em `2`, entre com a conta em `3` e registre uma ocorrência em `4`. Guarde o ID exibido para buscar ou editar esse registro. Em `5`, a empresa solicita a vistoria. Use `9` para preparar as contas internas, depois entre como gestor para revisar e agendar e como agente para concluir. O agente piloto é vinculado à equipe `1`.

## Reutilizar as credenciais

Para evitar digitar os dados a cada execução, copie `aegis.local.properties.example` para **`aegis.local.properties`** na pasta do `pom.xml`. O arquivo local está no `.gitignore`. Preencha sem aspas.

```properties
aegis.db.usuario=seu_usuario_oracle
aegis.db.senha=sua_senha_oracle
aegis.openai.api-key=sua_chave_openai
```

A terceira linha guarda a chave da LIA. Se a janela **Run** do IntelliJ avisar que a entrada ficará visível, use esse arquivo para evitar digitar segredos no console.

A conexão usa o driver Oracle e `DriverManager.getConnection(url, usuario, senha)` na classe [`ConexaoFactory`](src/main/java/br/com/fiap/conexoes/ConexaoFactory.java). A URL padrão é `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl`.

## Testar partes do AEGIS separadamente

As classes estão em [`src/main/java/br/com/fiap/main`](src/main/java/br/com/fiap/main). Execute o `main()` da classe desejada no IntelliJ e siga as perguntas do console.

| Área | Classes |
| --- | --- |
| Oracle e conta | `TesteConexao`, `TesteCadastrarUsuario` |
| Ocorrências | `TesteArrayListOcorrencia`, `TesteInserirOcorrencia`, `TesteSelecionarOcorrencias`, `TesteSelecionarOcorrenciaPorId`, `TesteAtualizarOcorrencia`, `TesteDeletarOcorrencia` |
| Vistorias, integrações e exceções | `TesteVistoria`, `TesteInfoDengue`, `TesteLia`, `TesteExcecao` |

Os serviços de InfoDengue e OpenAI montam a requisição com Apache HttpClient, leem a entidade com `EntityUtils` e convertem o JSON com Gson para os modelos em `br.com.fiap.api`.

Para testar apenas a API REST, execute `br.com.fiap.AegisApplication`. Enquanto o menu ou a aplicação estiverem abertos, consulte `GET /api/saude` e `GET /api/saude/oracle` em `http://127.0.0.1:8080`.

Os testes automáticos rodam com `.\mvnw.cmd test` e usam H2 em memória para o fluxo Oracle e amostras de respostas externas.

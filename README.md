# AEGIS

Backend Java da Global Solution 2026.2. O `AegisTerminal` inicia o Spring Boot e abre o acesso aos perfis do sistema. O menu e as rotas REST compartilham as mesmas regras e o banco Oracle da FIAP.

## Atualizar o Oracle

Pare o AEGIS antes de alterar o esquema. Na pasta `GS2_JAVA`, baixe a versão atualizada.

```powershell
git pull --ff-only
```

- **Banco AEGIS já criado.** Abra [`database/02_migrar_perfis.sql`](database/02_migrar_perfis.sql) no SQL Developer e execute com **F5**, no mesmo usuário Oracle em que rodou o script anterior. A migração copia os dados de perfil, preserva os IDs das contas e mantém as referências de ocorrências, vistorias e histórico. Ela pode ser repetida se a execução for interrompida.
- **Banco novo.** Execute [`database/01_schema.sql`](database/01_schema.sql) com **F5** no esquema vazio.

| Tabela | O que guarda |
| --- | --- |
| `AEGIS_USUARIO` | Identificador, e-mail, hash da senha e perfil da conta |
| `AEGIS_CIDADAO` | Nome, CPF, endereço, ocupação e telefone |
| `AEGIS_EMPRESA` | Razão social, CNPJ, nome fantasia, atividade, endereço, responsável, telefone e características das instalações |
| `AEGIS_GESTOR` | Nome, identificação funcional fictícia e setor |
| `AEGIS_AGENTE` | Nome, identificação funcional fictícia e equipe |

## Preencher as credenciais uma vez

Copie `CredenciaisLocais.java.example` para `CredenciaisLocais.java` na mesma pasta. O comando abaixo cria o arquivo se ele ainda estiver ausente.

```powershell
$arquivo = 'src/main/java/br/com/fiap/config/CredenciaisLocais.java'
if (!(Test-Path $arquivo)) { Copy-Item ($arquivo + '.example') $arquivo }
```

Abra **`CredenciaisLocais.java`** no IntelliJ e preencha os valores entre aspas.

```java
public static final String ORACLE_USUARIO = "seu_usuario_oracle";
public static final String ORACLE_SENHA = "sua_senha_oracle";
public static final String OPENAI_API_KEY = "sua_chave_openai";
```

O arquivo Java com valores reais está no `.gitignore`. O AEGIS e os testes reutilizam essas credenciais. As configurações anteriores em `aegis.local.properties` e nas variáveis de ambiente continuam disponíveis. Na abertura do terminal, a linha `OpenAI` indica de onde veio a configuração. A consulta ao InfoDengue usa dados públicos e funciona separadamente da LIA.

A conexão usa `DriverManager.getConnection(url, usuario, senha)` em [`ConexaoFactory`](src/main/java/br/com/fiap/conexoes/ConexaoFactory.java). A URL padrão é `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl`.

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

As classes ficam no [pacote `main`](src/main/java/br/com/fiap/main). `TesteConexao` verifica o Oracle e a equipe piloto. `TesteCadastrarUsuario` abre a escolha de perfil. Os testes de ocorrências e vistorias pedem o login da conta AEGIS para aplicar as permissões. `TesteLia` usa a chave definida no arquivo Java local.

Para rodar apenas a API REST, execute `br.com.fiap.AegisApplication`. No modo terminal, use o endereço impresso para consultar `/api/saude` e `/api/saude/oracle`. Ao iniciar `AegisApplication` diretamente, a porta padrão é 8080.

Os testes automatizados rodam com `.\mvnw.cmd test`. Eles usam H2 em modo Oracle, exercitam o fluxo de vistorias e verificam a cópia dos perfis legados e as regras de autorização.

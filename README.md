# AEGIS

Backend Java do AEGIS com o padrão das aulas **Controller → BO → DAO → Oracle**. Os testes de aula ficam em [`src/main/java/br/com/fiap/main`](src/main/java/br/com/fiap/main) e podem ser executados individualmente no IntelliJ.

## 1. Abrir o projeto

Se já clonou o repositório, pare o AEGIS e rode `git pull --ff-only` na pasta `GS2_JAVA`. Depois, rode `.\mvnw.cmd clean` para limpar as classes geradas antes da reorganização.

Para uma instalação nova, clone e abra a pasta `GS2_JAVA` no IntelliJ como projeto Maven com **JDK 21**.

```powershell
git clone https://github.com/jovexdev/GS2_JAVA.git
cd GS2_JAVA
```

## 2. Configurar Oracle e OpenAI

Se o esquema ainda estiver vazio, rode [`database/01_schema.sql`](database/01_schema.sql) com **F5** no SQL Developer, usando sua conta Oracle da FIAP. Se você já executou o script nessa conta, siga para a configuração local.

Na mesma pasta do `pom.xml`, copie `aegis.local.properties.example` para `aegis.local.properties`. Este comando cria o arquivo apenas se ele ainda não existir.

```powershell
if (!(Test-Path aegis.local.properties)) {
    Copy-Item aegis.local.properties.example aegis.local.properties
}
```

Abra **`aegis.local.properties`** no IntelliJ e preencha as três linhas com seus valores, sem aspas. O arquivo está no `.gitignore`.

```properties
aegis.db.usuario=seu_usuario_oracle
aegis.db.senha=sua_senha_oracle
aegis.openai.api-key=sua_chave_openai
```

A classe [`ConexaoFactory`](src/main/java/br/com/fiap/conexoes/ConexaoFactory.java) usa `Class.forName` e `DriverManager.getConnection(url, usuario, senha)`, como na aula. A URL padrão é `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl`. A chave da OpenAI é usada em `TesteLia` e na rota `/api/lia`.

## 3. Rodar os testes pelo IntelliJ

Abra a classe em **`src/main/java/br/com/fiap/main`**, clique no triângulo verde ao lado de `main()` e selecione **Run**. As classes com entrada abrem caixas de diálogo como nos exemplos da aula. Veja as respostas na aba **Run**. Se o arquivo local não for encontrado, confira se o **Working directory** da configuração de execução é a pasta do `pom.xml`.

**Comece por `TesteConexao`.** Se a conta e o script SQL estiverem corretos, a aba Run mostra `Conectado ao Oracle da FIAP.` e `Equipe piloto encontrada: 1 - Equipe piloto`. Em seguida, rode `TesteCadastrarUsuario` para criar uma conta fictícia do AEGIS. Use essa conta nos testes de ocorrência e guarde o ID que aparece após a inserção.

| Teste da aula | Classe no AEGIS |
| --- | --- |
| `TesteConexao` | `TesteConexao` |
| `TesteArrayListAluno` | `TesteArrayListOcorrencia` |
| `TesteInserirAluno` | `TesteInserirOcorrencia` |
| `TesteSelecionarAluno` | `TesteSelecionarOcorrencias` |
| `TesteSelecionarAlunoPorRm` | `TesteSelecionarOcorrenciaPorId` |
| `TesteAtualizarAluno` | `TesteAtualizarOcorrencia` |
| `TesteDeletarAluno` | `TesteDeletarOcorrencia` |
| `TesteExcecao` | `TesteExcecao` |
| `TesteViacep` | `TesteViacep` |
| `TesteSwPlaneta` | `TesteSwPlaneta` |

Também há `TesteInfoDengue`, `TesteLia` e `TesteVistoria`. Para preparar gestor e agente, descomente e preencha suas linhas no arquivo local e inicie `AegisApplication` uma vez. Depois rode `TesteVistoria` com o login correspondente à ação. Use a empresa para solicitar, o gestor para revisar e agendar e o agente para concluir. A empresa pode consultar o histórico. Edite ou exclua uma ocorrência de teste antes de vinculá-la a uma vistoria.

## 4. Iniciar a API REST

Para testar pelo navegador ou PowerShell, execute **`br.com.fiap.AegisApplication`** pelo triângulo verde do IntelliJ ou rode na raiz do projeto.

```powershell
.\mvnw.cmd spring-boot:run
```

Com o servidor rodando, abra outra aba do terminal.

```powershell
$api = 'http://127.0.0.1:8080/api'
Invoke-RestMethod "$api/saude"
Invoke-RestMethod "$api/saude/oracle"
```

`/saude` confirma a inicialização. `/saude/oracle` confirma que o backend conseguiu consultar a tabela `AEGIS_EQUIPE`.

### Cadastrar uma empresa e registrar uma ocorrência

Use dados fictícios. Se repetir o cadastro, faça login com a conta existente ou escolha outro documento e e-mail.

```powershell
$empresa = @{
  perfil = 'EMPRESA'; nome = 'Empresa Piloto'; documento = '11.222.333/0001-81'
  email = 'empresa@aegis.test'; endereco = 'Endereço fictício'; responsavel = 'Pessoa Demo'
  senha = 'SenhaEmpresaDemo1'
}

Invoke-RestMethod "$api/usuarios" -Method Post -ContentType 'application/json' -Body ($empresa | ConvertTo-Json)
$login = @{ email = $empresa.email; senha = $empresa.senha } | ConvertTo-Json
Invoke-RestMethod "$api/sessao" -Method Post -ContentType 'application/json' -Body $login -SessionVariable sessaoEmpresa

$relato = @{
  descricao = 'Baldes e reservatório aberto'; localizacao = 'Endereço fictício'
  aguaParada = $true; reservatorioAberto = $true; materiaisExpostos = $false
} | ConvertTo-Json
$ocorrencia = Invoke-RestMethod "$api/ocorrencias" -Method Post -ContentType 'application/json' -Body $relato -WebSession $sessaoEmpresa
$vistoria = Invoke-RestMethod "$api/ocorrencias/$($ocorrencia.id)/vistoria" -Method Post -WebSession $sessaoEmpresa
$vistoria
```

Para cadastrar um cidadão, use `perfil = 'CIDADAO'`, um CPF fictício de 11 dígitos e omita `responsavel`. O cidadão registra ocorrências com descrição e localização. Nesta versão, a solicitação formal de vistoria é feita pela empresa, conforme o fluxo descrito nas histórias do backlog.

### Passar pelo gestor e pelo agente

Faça login em `/api/sessao` com as contas internas e guarde as sessões em variáveis separadas, como no login da empresa. O gestor revisa a prioridade e agenda a vistoria. O agente da equipe designada registra o resultado.

| Método | Rota | Corpo JSON ou retorno |
| --- | --- | --- |
| `GET` | `/api/equipes` | Lista de equipes para o gestor |
| `PUT` | `/api/vistorias/{id}/prioridade` | `{"prioridade":"INSTAVEL","justificativa":"Confirmar em campo"}` |
| `PUT` | `/api/vistorias/{id}/agendamento` | `{"equipeId":1,"agendadaPara":"2027-01-15T10:30:00"}`. Use uma data futura no horário de São Paulo, com segundos zerados. |
| `GET` | `/api/vistorias` | Solicitações visíveis ao perfil autenticado |
| `GET` | `/api/vistorias/{id}/ocorrencia` | Relato associado, visível ao solicitante, gestor e agente designado |
| `PUT` | `/api/vistorias/{id}/conclusao` | `{"resultado":"Vistoria simulada concluída"}` |
| `GET` | `/api/vistorias/{id}/historico` | Histórico das decisões e mudanças |

A empresa pode consultar, atualizar e excluir sua ocorrência em `/api/ocorrencias/{id}` com `GET`, `PUT` e `DELETE`, respectivamente. A edição e exclusão ficam disponíveis até a solicitação de vistoria. Use `DELETE /api/sessao` para encerrar a sessão.

### Consultar indicadores e a LIA

```powershell
Invoke-RestMethod "$api/epidemiologia/historico?municipio=3550308&ano=2025"
Invoke-RestMethod "$api/epidemiologia/previsao?municipio=3550308&ano=2025"
$pergunta = @{ pergunta = 'Como interpretar a diferença entre casos notificados e estimados?' } | ConvertTo-Json
Invoke-RestMethod "$api/lia" -Method Post -ContentType 'application/json' -Body $pergunta -WebSession $sessaoEmpresa
```

O histórico vem do [InfoDengue](https://info.dengue.mat.br/services/api/doc). A previsão acadêmica usa a média móvel das quatro últimas semanas consecutivas do período. A LIA pode receber `municipio` e `ano` no corpo para contextualizar a resposta com os indicadores públicos. Use perguntas sem dados pessoais.

## Testes automáticos e atualizações

```powershell
.\mvnw.cmd clean test
git pull --ff-only
```

Os testes usam um banco H2 em memória, no modo de compatibilidade Oracle, e amostras de respostas das APIs externas. O teste de conexão real do seu ambiente é `GET /api/saude/oracle`.

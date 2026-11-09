# AEGIS

Backend Java da Global Solution 2026.2 para registrar ocorrências ambientais, acompanhar vistorias simuladas e consultar dados municipais de dengue. O fluxo segue o padrão das aulas **Controller → BO → DAO → Oracle**, com JDBC, SQL parametrizado e tratamento centralizado de exceções.

## 1. Clonar e abrir no IntelliJ

```powershell
git clone https://github.com/jovexdev/GS2_JAVA.git
cd GS2_JAVA
```

Entre no GitHub com uma conta que tenha acesso ao repositório privado se o Git pedir autenticação. Abra a pasta `GS2_JAVA` no IntelliJ, importe o `pom.xml` como projeto Maven e selecione **JDK 21** em **File → Project Structure → Project SDK**.

## 2. Preparar o Oracle da FIAP

Conecte-se ao Oracle no SQL Developer com o seu usuário da FIAP. No seu próprio esquema, abra [`database/01_schema.sql`](database/01_schema.sql) e execute o arquivo inteiro uma vez com **F5 / Run Script**. Ele cria as tabelas `AEGIS_*` e a equipe piloto de ID `1`.

No terminal **PowerShell** do IntelliJ, na raiz do projeto, confira o JDK e configure as credenciais da sessão.

```powershell
java -version
$env:DB_USUARIO = Read-Host 'Usuário Oracle FIAP'
$env:DB_SENHA = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha Oracle FIAP' -AsSecureString)).Password
```

Se `java -version` indicar Java 8 ou outra versão, ajuste o terminal para o caminho do seu JDK 21 antes de rodar o Maven.

```powershell
$env:JAVA_HOME = 'C:\caminho\para\o\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

A URL padrão é `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL`, a mesma das aulas. Use `DB_URL` se a FIAP fornecer outra URL.

Para a demonstração completa de vistorias, configure os dois usuários internos antes de iniciar a aplicação. Use senhas diferentes com pelo menos oito caracteres.

```powershell
$env:AEGIS_GESTOR_EMAIL = 'gestor@aegis.test'
$env:AEGIS_GESTOR_SENHA = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha do gestor' -AsSecureString)).Password
$env:AEGIS_AGENTE_EMAIL = 'agente@aegis.test'
$env:AEGIS_AGENTE_SENHA = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha do agente' -AsSecureString)).Password
```

O gestor e o agente são cadastrados no primeiro início com essas variáveis. O agente é ligado à equipe piloto. Em reinícios, use o mesmo e-mail e a mesma senha já cadastrados.

Para ativar a LIA, configure a chave no mesmo terminal. O backend usa a [Responses API](https://developers.openai.com/api/docs/guides/text). O modelo padrão é `gpt-5.6-terra`, configurável com `OPENAI_MODEL`.

```powershell
$env:OPENAI_API_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'Chave OpenAI' -AsSecureString)).Password
```

## 3. Rodar e testar pelo terminal

```powershell
.\mvnw.cmd spring-boot:run
```

No primeiro uso, o Maven baixa as dependências. Deixe esse terminal aberto e rode as requisições abaixo em outra aba.

```powershell
$api = 'http://127.0.0.1:8080/api'
Invoke-RestMethod "$api/saude"
Invoke-RestMethod "$api/saude/oracle"
```

`/saude` confirma que a aplicação está no ar. `/saude/oracle` abre a conexão e consulta uma tabela do AEGIS. Se retornar `503`, confira as credenciais, a conexão com o servidor e a execução do script SQL.

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

## Testes e atualizações

```powershell
.\mvnw.cmd test
git pull --ff-only
```

Os testes usam um banco H2 em memória, no modo de compatibilidade Oracle, e amostras de respostas das APIs externas. O teste de conexão real do seu ambiente é `GET /api/saude/oracle`.

# AEGIS

Backend Java da Global Solution 2026.2. O `AegisTerminal` inicia o Spring Boot e abre o acesso aos perfis do sistema. O menu e as rotas REST compartilham as mesmas regras e o banco Oracle da FIAP.

## Instalar o Oracle com um único script

Pare o AEGIS e baixe a versão atualizada na pasta `GS2_JAVA`.

```powershell
git pull --ff-only
```

Abra [`database/01_schema.sql`](database/01_schema.sql) no SQL Developer, no seu usuário Oracle, e execute o arquivo inteiro com **F5 uma vez**, em uma base sem tabelas AEGIS. Ele contém a estrutura completa, com todas as tabelas, chaves primárias e estrangeiras, índices e a equipe piloto. A vistoria já inclui o agente designado, inspeção, recomendações e os estados Pendente, Agendada, Em andamento, Concluída e Encerrada.

Se você tem uma instalação antiga e quer substituí-la, use a opção **APAGAR_TABELAS** do `Manutencao.sql` e depois execute o `01_schema.sql` inteiro uma vez. Essa substituição exclui os dados antigos. Não há outro bloco SQL para copiar do README.

| Tabela | O que guarda |
| --- | --- |
| `AEGIS_EQUIPE` | Equipes responsáveis pelos atendimentos |
| `AEGIS_USUARIO` | Identificador, e-mail, hash da senha e perfil da conta |
| `AEGIS_CIDADAO` | Nome, CPF, endereço, ocupação e telefone |
| `AEGIS_EMPRESA` | Razão social, CNPJ, nome fantasia, atividade, endereço, responsável, telefone e características das instalações |
| `AEGIS_GESTOR` | Nome, identificação funcional fictícia e setor |
| `AEGIS_AGENTE` | Nome, identificação funcional fictícia e equipe |
| `AEGIS_OCORRENCIA` | Relatos ambientais, localização e classificação preliminar |
| `AEGIS_VISTORIA` | Agente e equipe designados, prioridade, agendamento, inspeção, recomendações e resultado |
| `AEGIS_HISTORICO` | Decisões e etapas de cada atendimento |

## Manutenção opcional

Abra [`database/Manutencao.sql`](database/Manutencao.sql), altere o valor de `v_acao` no início do bloco e execute com **F5**, com a aplicação parada. O arquivo executa somente a opção escolhida.

| Valor de `v_acao` | Resultado |
| --- | --- |
| `NENHUMA` | Padrão. Mostra as opções sem alterar o banco. |
| `APAGAR_DADOS` | Apaga contas, perfis, ocorrências, vistorias, histórico e equipes. Mantém tabelas, chaves e índices; recria somente a equipe piloto de ID `1`. |
| `APAGAR_TABELAS` | Exclui as nove tabelas AEGIS e seus dados, respeitando a ordem das chaves estrangeiras. Depois, instale novamente com `01_schema.sql`. |

A limpeza dos dados não reinicia os IDs e não atualiza a estrutura das tabelas. Após essa limpeza, a aplicação pode receber novos cadastros ou a carga opcional `DadosFicticios.sql`. A exclusão de tabelas não pode ser desfeita com `ROLLBACK`.

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


## Minha saúde: CNES e orientação da LIA

A nova aba **Minha saúde** do frontend é exclusiva do cidadão. Não exige alteração no Oracle: as unidades vêm do CNES e as respostas sobre sintomas não são gravadas no banco.

A integração segue o padrão de chamada HTTP e leitura com Gson usado pelo InfoDengue:

- `api/EstabelecimentoCnes.java` e `api/RespostaCnes.java`: campos recebidos da API.
- `services/CnesService.java`: GET público, sem chave, paginação, limites de espera e cache de 30 minutos.
- `bo/UnidadesSaudeBO.java`: conversão interna do código municipal, filtro de pronto atendimento SUS ativo e tratamento de coordenadas ausentes.
- `bo/OrientacaoCidadaoBO.java`: orientação inicial usando o serviço OpenAI existente, com indicação imediata de urgência para sinais de alarme.
- `controller/CuidadoCidadaoController.java`: sessão e autorização do perfil cidadão.
- `main/TesteCnes.java`: consulta independente no IntelliJ, sem Oracle e sem chave GPT. O código IBGE é pedido somente neste teste técnico; no frontend, a busca é pelo nome e UF.

| Método | Rota | Uso |
| --- | --- | --- |
| GET | `/api/cidadao/unidades-saude?municipio=3509502` | UPAs e pronto atendimento SUS do município |
| POST | `/api/cidadao/orientacao` | Questionário de sintomas com consentimento |

O POST recebe `alerta` (`SIM`, `NAO` ou `NAO_SEI`), `sintomas`, `inicio`, `evolucao` e `consentimento`. Sinais de alarme ou dúvida sobre reconhecê-los retornam orientação presencial imediatamente, sem chamar a OpenAI. Para as demais respostas, o usuário autoriza o envio do relato. O GPT organiza os sintomas em trechos do próprio relato; os textos de orientação são controlados pela aplicação. O resultado nunca confirma nem descarta dengue. Se a chave faltar ou o provedor falhar, a interface informa a indisponibilidade e apresenta orientação de referência, sem simular resposta da IA. A resposta HTTP usa `Cache-Control: no-store`.

Para conectar com o frontend na porta padrão, execute **AegisApplication**, mantendo a API em **8080**. No VS Code, execute `npm run dev` e abra **Cidadão → Minha saúde**. As credenciais Oracle continuam em `ConexaoFactory.java` e a chave GPT em `OpenAiService.java`.

O [CNES / Ministério da Saúde](https://www.gov.br/saude/pt-br/acesso-a-informacao/sic/dados-em-transparencia-ativa/seidigi) fornece a [API pública](https://apidadosabertos.saude.gov.br/cnes/estabelecimentos) sem chave ou cobrança. A consulta usa o tipo 73 (pronto atendimento) e exclui registros desabilitados ou sem atendimento ambulatorial SUS. O cadastro pode estar desatualizado e não informa filas, vagas ou funcionamento em tempo real. Unidades sem coordenadas permanecem na lista pelo endereço.

O mapa usa Leaflet/OpenStreetMap. O trajeto abre um [link de navegação do Google Maps](https://developers.google.com/maps/documentation/urls/get-started), sem contratar Maps API nem cadastrar faturamento de mapas. A distância exibida pelo AEGIS é em linha reta, e a navegação externa calcula o trajeto. A localização é opcional, permanece no navegador e só é incluída no link ao abrir uma rota; as respostas de saúde não vão para o mapa. **A LIA continua usando os créditos da chave OpenAI existente**, que são independentes da gratuidade do CNES.

Referência de orientação: [dengue — Ministério da Saúde](https://www.gov.br/saude/pt-br/assuntos/saude-de-a-a-z/d/dengue).

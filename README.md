# Parada Certa – API (CitizenTech · Global Solution 2026/2)

Backend em Java do **Parada Certa**, um sistema de embarque assistido em ônibus. Na parada há um **totem** (ESP32) com leitor de cartão e botão. Quando o passageiro aproxima o cartão ou aperta o botão, a API registra uma **solicitação de embarque**, gera a **mensagem de voz** do totem com IA e **avisa os motoristas** da linha por mensageria assíncrona (RabbitMQ).

Perfis de usuário:

| Perfil | O que faz no app |
|---|---|
| `CUIDADOR` | Cadastra os passageiros que acompanha, os cartões e as rotas frequentes deles |
| `MOTORISTA` | Vê as solicitações da sua linha e marca embarque / não atendimento |
| `GESTOR` | Administra usuários, paradas, linhas, totens, veículos e acompanha tudo |

---

## Sumário

1. [Stack](#stack)
2. [Requisitos atendidos (Java Advanced)](#requisitos-atendidos-java-advanced)
3. [Como rodar](#como-rodar)
4. [Usuários de teste](#usuários-de-teste)
5. [Autenticação (JWT)](#autenticação-jwt)
6. [Padrões de resposta](#padrões-de-resposta)
7. [Endpoints](#endpoints)
8. [Valores dos enums](#valores-dos-enums)
9. [Fluxo da solicitação de embarque](#fluxo-da-solicitação-de-embarque)
10. [Mensageria – opção A (RabbitMQ)](#mensageria--opção-a-rabbitmq)
11. [IA – mensagem de voz com Spring AI](#ia--mensagem-de-voz-com-spring-ai)
12. [Guia de integração para o app (front / mobile)](#guia-de-integração-para-o-app-front--mobile)
13. [Testes](#testes)
14. [Estrutura do projeto](#estrutura-do-projeto)
15. [Deploy (Azure + Azure DevOps)](#deploy-azure--azure-devops)
16. [Declaração de uso de IA](#declaração-de-uso-de-ia)

---

## Stack

- Java 21 · Spring Boot 4.1 · Maven (wrapper incluso, não precisa instalar o Maven)
- Spring Web MVC · Spring Data JPA (Hibernate 7) · Bean Validation
- Spring Security 7 com JWT (OAuth2 Resource Server, HS256)
- Spring AI 2.0 (OpenAI, modelo `gpt-4o-mini`)
- Spring AMQP + RabbitMQ 4.1
- Oracle (banco da FIAP) · H2 em memória (perfil `dev` e testes)
- springdoc-openapi (Swagger UI) · Lombok · Actuator

## Requisitos atendidos (Java Advanced)

| # | Requisito | Onde está |
|---|---|---|
| 1 | API REST com 3+ entidades relacionadas, CRUD com Spring Data JPA e paginação | 12 entidades em `model/`, repositórios em `repository/`, CRUD paginado em `controller/` |
| 2 | Bean Validation nos DTOs e `@RestControllerAdvice` global | `dto/request/*` e `exception/GlobalExceptionHandler` |
| 3 | Spring Security + JWT com 2+ perfis | `config/SecurityConfig`, `security/TokenService` – perfis `CUIDADOR`, `MOTORISTA` e `GESTOR` |
| 4 | Funcionalidade com Spring AI integrada ao fluxo | `ai/MensagemVozService` – mensagem de voz do totem |
| 5 | Arquitetura distribuída | **Opção A – mensageria com RabbitMQ** (`config/RabbitConfig`, `messaging/`) |
| 6 | Swagger, README e integração com o Mobile | Swagger em `/swagger-ui.html`, este README, CORS liberado e perfil `dev` para o app |

---

## Como rodar

### Pré-requisito

- **JDK 21** (`java -version` deve mostrar 21).

Todos os comandos abaixo são executados dentro da pasta `citizentech/` (onde está o `pom.xml`).

### Opção 1 – Modo rápido (perfil `dev`) – recomendado para o front

Não precisa de Oracle, RabbitMQ nem chave da OpenAI. Usa banco H2 em memória e já sobe com **dados de exemplo**.

```bash
# Linux / macOS / Git Bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```powershell
# Windows (PowerShell ou CMD)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

A API sobe em **http://localhost:8080** e o Swagger em **http://localhost:8080/swagger-ui.html**.

No perfil `dev`:

- O banco é recriado toda vez que a API sobe (os dados criados pelo app somem ao reiniciar).
- O aviso ao motorista roda dentro da própria API (sem fila). O resultado no app é o mesmo do modo com RabbitMQ.
- Sem `OPENAI_API_KEY`, a mensagem de voz usa uma frase padrão. Com a chave definida, a IA é usada de verdade.

### Opção 2 – Modo completo (Oracle + RabbitMQ + OpenAI)

1. Suba o RabbitMQ com Docker:
   ```bash
   docker compose up -d
   ```
   Painel do RabbitMQ: http://localhost:15672 (usuário `guest`, senha `guest`).
2. Copie `.env.example` para `.env` e preencha as variáveis. Exporte-as no terminal ou configure-as na IDE.
3. Rode a API:
   ```bash
   ./mvnw spring-boot:run
   ```

> O schema do Oracle deve existir antes (`DDL_AUTO=validate` por padrão). Para criar as tabelas pelo Hibernate em um banco vazio, use `DDL_AUTO=update`.

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` | URL do Oracle |
| `DB_USERNAME` / `DB_PASSWORD` | – | Credenciais do Oracle |
| `DDL_AUTO` | `validate` | Estratégia do Hibernate |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | RabbitMQ |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `guest` / `guest` | RabbitMQ |
| `MENSAGERIA_MODO` | `rabbit` | `rabbit` (fila) ou `local` (sem fila) |
| `OPENAI_API_KEY` | – | Chave da OpenAI |
| `OPENAI_MODEL` | `gpt-4o-mini` | Modelo usado |
| `JWT_SECRET` | – | Segredo do JWT (**mínimo 32 caracteres**) |
| `JWT_EXPIRATION_MINUTES` | `120` | Validade do token |
| `HASH_SECRET` | – | Segredo do HMAC do UID do cartão e do token do totem |
| `GESTOR_NOME` / `GESTOR_EMAIL` / `GESTOR_SENHA` | – | Cria o primeiro gestor ao subir (se o e-mail ainda não existir) |
| `CORS_ALLOWED_ORIGINS` | `*` | Origens liberadas no CORS, separadas por vírgula |
| `PORT` | `8080` | Porta HTTP |

---

## Usuários de teste

Criados automaticamente no perfil `dev`:

| Perfil | E-mail | Senha |
|---|---|---|
| GESTOR | `gestor@paradacerta.com` | `Gestor@123` |
| CUIDADOR | `cuidador@paradacerta.com` | `Cuidador@123` |
| MOTORISTA | `motorista@paradacerta.com` | `Motorista@123` |

Dados de exemplo do perfil `dev`:

| Recurso | Exemplo |
|---|---|
| Paradas | `1` – Av. Paulista, 1578 – MASP · `2` – Terminal Varginha |
| Linhas | `1` – 6091-10 (Terminal Varginha) · `2` – 875A-10 (Aclimação) |
| Totem | `1` – `TOTEM-0001`, na parada 1, token `token-totem-0001` |
| Veículo | `1` – prefixo `6 1234`, linha 6091-10, dirigido pelo motorista de teste |
| Passageiro | `1` – Joao Passageiro (`BAIXA_VISAO`), do cuidador de teste |
| Cartão | `1` – UID `04A1B2C3`, do passageiro 1 |
| Rota frequente | `1` – "Casa para o trabalho" (parada 1, linha 6091-10) |

No modo completo (Oracle), o primeiro gestor vem de `GESTOR_EMAIL` / `GESTOR_SENHA`. O cadastro público (`/auth/registro`) cria sempre um `CUIDADOR`. Motoristas e gestores são criados por um gestor em `/api/usuarios`.

---

## Autenticação (JWT)

1. Faça login:

   ```http
   POST /auth/login
   Content-Type: application/json

   { "email": "cuidador@paradacerta.com", "senha": "Cuidador@123" }
   ```

   Resposta `200`:

   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiJ9...",
     "tipo": "Bearer",
     "expiraEm": "2026-11-15T03:06:14Z",
     "usuario": { "id": 2, "nome": "Ana Cuidadora", "email": "cuidador@paradacerta.com", "perfil": "CUIDADOR" }
   }
   ```

2. Envie o token em todas as outras chamadas:

   ```http
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   ```

3. Use `GET /auth/me` para recuperar o usuário logado (por exemplo, ao reabrir o app com um token salvo).

O token dura 120 minutos por padrão. Quando ele vence, a API responde `401` e o app deve mandar o usuário para o login.

O payload do token traz `sub` (id do usuário), `perfil`, `nome` e `email`. O app pode decodificá-lo para decidir qual tela mostrar.

Cadastro público (sempre cria `CUIDADOR`):

```http
POST /auth/registro
Content-Type: application/json

{ "nome": "Maria", "email": "maria@email.com", "senha": "minhasenha", "expoPushToken": "ExponentPushToken[...]" }
```

`expoPushToken` é opcional.

---

## Padrões de resposta

### Paginação

Toda listagem aceita `?page=0&size=10&sort=campo,asc` e responde neste formato:

```json
{
  "content": [ { "...": "..." } ],
  "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

`page` começa em **0**.

### Erros

Todo erro segue o mesmo formato. `campos` só aparece nos erros de validação.

```json
{
  "timestamp": "2026-11-15T01:06:16.87",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados invalidos na requisicao",
  "caminho": "/api/passageiros",
  "campos": [ { "campo": "nome", "mensagem": "O nome e obrigatorio" } ]
}
```

| Status | Quando |
|---|---|
| `400` | Validação falhou, JSON inválido ou parâmetro com tipo errado |
| `401` | Sem token, token inválido/vencido ou login com e-mail/senha errados |
| `403` | O perfil não tem permissão, ou o cuidador tentou acessar dados de outro cuidador |
| `404` | Recurso não encontrado |
| `409` | Conflito (e-mail, UID de cartão ou chave de idempotência já cadastrados; passageiro já tem solicitação ativa) |
| `422` | Regra de negócio (ex.: totem inativo, solicitação já encerrada) |
| `500` | Erro inesperado |

Criações respondem `201` com o header `Location` apontando para o recurso criado. Exclusões respondem `204`.

---

## Endpoints

A documentação completa, com todos os campos e o botão **Authorize** para testar com o token, está no Swagger: **`/swagger-ui.html`**. O JSON OpenAPI (`/v3/api-docs`) pode ser importado no Postman ou no Insomnia.

### Autenticação (pública, exceto `/me`)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/login` | Login, devolve o JWT |
| POST | `/auth/registro` | Cadastro público de cuidador |
| GET | `/auth/me` | Usuário do token (qualquer perfil logado) |

### Recursos

Cada recurso tem `GET` (lista paginada), `GET /{id}`, `POST`, `PUT /{id}` e `DELETE /{id}`, salvo onde indicado.

| Rota | Leitura | Escrita | Observações |
|---|---|---|---|
| `/api/usuarios` | GESTOR | GESTOR | |
| `/api/paradas` | qualquer perfil logado | GESTOR | |
| `/api/linhas` | qualquer perfil logado | GESTOR | |
| `/api/totens` | GESTOR | GESTOR | O `token` do totem é gravado só como hash e nunca volta na resposta |
| `/api/veiculos` | MOTORISTA, GESTOR | GESTOR | |
| `/api/passageiros` | CUIDADOR, GESTOR | CUIDADOR, GESTOR | Filtro `?cuidadorId=`. O cuidador só vê e altera os próprios passageiros |
| `/api/cartoes` | CUIDADOR, GESTOR | CUIDADOR, GESTOR | Filtro `?passageiroId=`. O `uid` é gravado só como hash |
| `/api/rotas-frequentes` | CUIDADOR, GESTOR | CUIDADOR, GESTOR | Filtro `?passageiroId=` |
| `/api/solicitacoes` | MOTORISTA, GESTOR | GESTOR | Filtro `?status=`. Sem `PUT` (veja abaixo) |

### Solicitações de embarque – rotas extras

| Método | Rota | Perfis | Descrição |
|---|---|---|---|
| PATCH | `/api/solicitacoes/{id}/status` | MOTORISTA, GESTOR | Muda o status. Corpo: `{ "status": "EMBARCADO" }` |
| GET | `/api/solicitacoes/{id}/tentativas` | MOTORISTA, GESTOR | Avisos enviados aos motoristas |
| GET | `/api/solicitacoes/{id}/auditoria` | MOTORISTA, GESTOR | Histórico de alterações (gravado pelo gatilho do Oracle; vazio no perfil `dev`) |

### Exemplos de corpo (POST / PUT)

```jsonc
// /api/passageiros  (o cuidador deve enviar o próprio id em cuidadorId; outro id dá 403)
{ "cuidadorId": 2, "nome": "Joao", "tipoNecessidade": "BAIXA_VISAO",
  "contatoEmergencia": "(11) 98888-7777", "consentimento": true, "versaoTermo": "1.0" }

// /api/cartoes
{ "passageiroId": 1, "uid": "04A1B2C3", "ativo": true }

// /api/rotas-frequentes
{ "passageiroId": 1, "paradaId": 1, "linhaId": 1, "apelido": "Casa para o trabalho" }

// /api/solicitacoes  (feito pelo totem / gestor)
{ "totemId": 1, "passageiroId": 1, "linhaId": 1, "tipoSinal": "CARTAO",
  "chaveIdempotencia": "totem1-20261115-0001" }

// /api/usuarios
{ "nome": "Carlos", "email": "carlos@email.com", "senha": "Senha@123", "perfil": "MOTORISTA" }

// /api/paradas
{ "codigoSptrans": 260015039, "nome": "MASP", "endereco": "Av. Paulista, 1578",
  "latitude": -23.561414, "longitude": -46.655881 }

// /api/linhas
{ "codigoSptrans": 1273, "letreiro": "6091-10", "sentido": 1, "destino": "Terminal Varginha" }

// /api/totens
{ "paradaId": 1, "identificador": "TOTEM-0002", "token": "token-secreto", "status": "ATIVO" }

// /api/veiculos
{ "prefixo": "6 1234", "linhaId": 1, "motoristaId": 3 }
```

O campo `consentimento` do passageiro precisa ser `true`, porque o aceite do termo é exigido pela LGPD.

---

## Valores dos enums

| Enum | Valores |
|---|---|
| `perfil` | `CUIDADOR`, `MOTORISTA`, `GESTOR` |
| `tipoNecessidade` | `CADEIRA_DE_RODAS`, `BAIXA_VISAO`, `MOBILIDADE_REDUZIDA`, `IDOSO` |
| `tipoSinal` | `CARTAO`, `BOTAO` |
| `status` (solicitação) | `CRIADA`, `MOTORISTA_AVISADO`, `EMBARCADO`, `NAO_ATENDIDA`, `SEM_ONIBUS`, `CANCELADA` |
| `status` (totem) | `ATIVO`, `INATIVO` |
| `resultado` (tentativa) | `PENDENTE`, `EMBARCADO`, `NAO_ATENDIDA`, `CANCELADA` |

---

## Fluxo da solicitação de embarque

```
Totem (cartão/botão)
   │  POST /api/solicitacoes
   ▼
API ── valida (totem ATIVO, 1 solicitação ativa por passageiro, chave de idempotência)
   │── gera mensagem de voz (Spring AI; frase padrão se a IA falhar)
   │── grava a solicitação como CRIADA e devolve 201 com a mensagem de voz
   │
   │  depois do commit: evento "solicitacao.criada"
   ▼
RabbitMQ ──► consumidor ── registra tentativa PENDENTE para a linha
                         └─ muda a solicitação para MOTORISTA_AVISADO
   ▼
App do motorista: GET /api/solicitacoes?status=MOTORISTA_AVISADO
   │  PATCH /api/solicitacoes/{id}/status  { "status": "EMBARCADO" }
   ▼
Status final (EMBARCADO, NAO_ATENDIDA, SEM_ONIBUS ou CANCELADA)
```

Regras de status:

- Ativos: `CRIADA`, `MOTORISTA_AVISADO` e `NAO_ATENDIDA`.
- Finais: `EMBARCADO`, `SEM_ONIBUS` e `CANCELADA`. Uma solicitação em status final não muda mais.
- Nenhuma solicitação volta para `CRIADA`.
- O passageiro só pode ter uma solicitação ativa por vez.
- Repetir a mesma `chaveIdempotencia` responde `409`. Isso evita pedidos duplicados quando o totem reenvia.
- Pedido pelo botão sem `linhaId`: fica `CRIADA` para o gestor tratar, porque a linha ainda não é conhecida.

---

## Mensageria – opção A (RabbitMQ)

Opção escolhida para o requisito 5: **A – mensageria assíncrona com RabbitMQ**.

| Item | Nome |
|---|---|
| Exchange (direct) | `citizentech.solicitacoes` |
| Routing key | `solicitacao.criada` |
| Fila | `citizentech.solicitacao.criada` |
| Dead-letter exchange | `citizentech.solicitacoes.dlx` |
| Dead-letter queue | `citizentech.solicitacao.criada.dlq` |

Como funciona:

- **Publicação após o commit.** `SolicitacaoEventPublisher` usa `@TransactionalEventListener(AFTER_COMMIT)`, então só publica solicitações que foram de fato gravadas. Se o RabbitMQ estiver fora do ar, a API não falha: a solicitação continua salva.
- **Consumidor idempotente.** `AvisoMotoristaListener` delega a `AvisoMotoristaService`, que ignora mensagens repetidas e solicitações que já saíram de `CRIADA`.
- **Retry e DLQ.** São 3 tentativas com backoff exponencial. Depois disso a mensagem vai para a DLQ, sem reenfileirar para sempre.
- **Mensagens em JSON**, com o Jackson.

No perfil `dev` (`MENSAGERIA_MODO=local`), o mesmo serviço é chamado direto após o commit, sem fila, para o front não precisar do Docker.

---

## IA – mensagem de voz com Spring AI

Quando o totem cria uma solicitação sem `mensagemVoz`, o `MensagemVozService` pede ao modelo da OpenAI, via `ChatClient` do Spring AI, uma frase curta e acolhedora para o totem falar. O pedido leva em conta:

- a linha e o destino;
- o tipo de necessidade do passageiro, por exemplo avisar que o motorista vai preparar a rampa para cadeira de rodas.

Proteções:

- **Fallback:** se a IA demorar, falhar ou não houver chave, a API usa uma frase padrão por tipo de necessidade. O totem nunca fica mudo.
- A resposta é limpa e limitada a 300 caracteres, o tamanho da coluna.
- Nenhum dado pessoal identificável (nome, contato) é enviado ao modelo.
- Timeouts de 3 s para conectar e 10 s de leitura, com até 2 tentativas.

---

## Guia de integração para o app (front / mobile)

1. **Suba a API no perfil `dev`** (veja [Como rodar](#como-rodar)) e faça login com os [usuários de teste](#usuários-de-teste).
2. **URL base** conforme onde o app roda:

   | Onde o app roda | URL base |
   |---|---|
   | Navegador / Expo Web na mesma máquina | `http://localhost:8080` |
   | Emulador Android | `http://10.0.2.2:8080` |
   | Simulador iOS | `http://localhost:8080` |
   | Celular físico (Expo Go) | `http://<IP-do-PC-na-rede>:8080` (ex.: `http://192.168.0.15:8080`) |

   No celular físico, o PC e o celular precisam estar na mesma rede Wi-Fi. Pode ser preciso liberar a porta 8080 no firewall do Windows. Para ver o IP do PC, use `ipconfig` (Windows) ou `ip a` (Linux).
3. **CORS** está liberado para qualquer origem por padrão. Em produção, restrinja com `CORS_ALLOWED_ORIGINS`.
4. **Token:** guarde o `token` do login (ex.: `expo-secure-store`) e mande `Authorization: Bearer <token>` em toda chamada. Em `401`, volte para o login.
5. **Telas por perfil:** use `usuario.perfil` da resposta do login (ou `GET /auth/me`):
   - `CUIDADOR` → passageiros, cartões e rotas frequentes;
   - `MOTORISTA` → solicitações e botões de embarque;
   - `GESTOR` → administração.
6. **Mensagens de erro:** mostre `mensagem` ao usuário e, nos formulários, use `campos[].campo` / `campos[].mensagem` para marcar cada campo.
7. **Atualização da tela do motorista:** faça polling de `GET /api/solicitacoes?status=MOTORISTA_AVISADO` (ex.: a cada 10 s). O campo `expoPushToken` do usuário já existe para um envio de push futuro.
8. **Swagger / OpenAPI:** `http://localhost:8080/swagger-ui.html`. O JSON em `/v3/api-docs` pode ser importado no Postman, no Insomnia ou em geradores de cliente.

Exemplo com `fetch`:

```js
const API = "http://10.0.2.2:8080";

async function login(email, senha) {
  const r = await fetch(`${API}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, senha }),
  });
  if (!r.ok) throw await r.json();          // { status, mensagem, ... }
  return r.json();                          // { token, usuario, ... }
}

async function listarPassageiros(token, page = 0) {
  const r = await fetch(`${API}/api/passageiros?page=${page}&size=10`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!r.ok) throw await r.json();
  return r.json();                          // { content: [...], page: {...} }
}
```

---

## Testes

```bash
./mvnw test
```

Os testes usam o H2 em memória e não precisam de Oracle, RabbitMQ ou OpenAI. Cobrem:

- o CRUD com paginação;
- validação e formato de erro;
- segurança, com perfis, token real e a regra do dono dos dados;
- a mensagem de voz com fallback;
- a publicação no RabbitMQ;
- o consumidor idempotente.

---

## Estrutura do projeto

```
citizentech/src/main/java/br/com/fiap/citizentech
├── ai/            # Spring AI: mensagem de voz do totem
├── config/        # Security, OpenAPI, RabbitMQ, senha, gestor inicial, dados do perfil dev
├── controller/    # Endpoints REST
├── dto/           # request (com Bean Validation) e response (records)
├── exception/     # Exceções de negócio e @RestControllerAdvice
├── messaging/     # Evento, publicador e consumidor do RabbitMQ (+ modo local)
├── model/         # Entidades JPA e enums
├── repository/    # Spring Data JPA
├── security/      # JWT, usuário logado e tratamento de erros de segurança
└── service/       # Regras de negócio
```

---

## Deploy (Azure + Azure DevOps)

**API publicada:** https://api-paradacerta.azurewebsites.net (Swagger em https://api-paradacerta.azurewebsites.net/swagger-ui.html)

### Arquitetura macro

```mermaid
flowchart LR
    subgraph Usuarios
        APP["App Mobile<br/>(Expo / React Native)<br/>cuidador"]
        MOT["App do motorista /<br/>painel do gestor"]
        TOT["Totem ESP32<br/>cartao ou botao"]
    end

    subgraph DevOps["Azure DevOps"]
        BOARDS["Azure Boards<br/>tarefas"]
        REPOS["Azure Repos<br/>main protegida + PR"]
        CI["Pipeline Build (CI)<br/>Maven + testes JUnit<br/>artefato .jar"]
        CD["Pipeline Deploy (CD)<br/>AzureWebApp + teste de fumaca"]
        BOARDS -. vincula .- REPOS
        REPOS -- merge na main --> CI -- novo artefato --> CD
    end

    subgraph Azure["Azure - rg-paradacerta (brazilsouth)"]
        WEB["App Service Linux<br/>Web App Java 21<br/>API Spring Boot"]
        ACR["Container Registry<br/>imagem do RabbitMQ"]
        ACI["Container Instance<br/>RabbitMQ 4.1"]
        ACR --> ACI
    end

    ORA[("Oracle FIAP<br/>banco PaaS")]
    OAI["OpenAI<br/>(Spring AI)"]

    APP -- "HTTPS + JWT" --> WEB
    MOT -- "HTTPS + JWT" --> WEB
    TOT -- HTTPS --> WEB
    WEB -- JDBC --> ORA
    WEB -- "AMQP: solicitacao.criada" --> ACI
    ACI -- "consumidor: avisa motoristas" --> WEB
    WEB -- "mensagem de voz" --> OAI
    CD -- deploy do .jar --> WEB
```

| Componente | Recurso no Azure | Criado por |
|---|---|---|
| API Java 21 | App Service Plan Linux (B1) + Web App | `scripts/infra-azure.sh` |
| Mensageria (opção A) | Azure Container Registry + Azure Container Instance com o RabbitMQ | `scripts/infra-azure.sh` + `dockerfiles/Dockerfile.rabbitmq` |
| Banco de dados | Oracle da FIAP (PaaS) | `scripts/script-bd.sql` |
| Build e Release | Azure Pipelines | `azure-pipeline.yml` |

### Arquivos de DevOps

| Arquivo | O que faz |
|---|---|
| [`scripts/infra-azure.sh`](scripts/infra-azure.sh) | Cria, via Azure CLI, o Resource Group, o ACR (com build da imagem do RabbitMQ na nuvem), o ACI do RabbitMQ, o App Service e as variáveis de ambiente da API |
| [`scripts/destroy-azure.sh`](scripts/destroy-azure.sh) | Apaga o Resource Group inteiro depois da avaliação |
| [`scripts/azure.env.example`](scripts/azure.env.example) | Modelo das variáveis (copie para `scripts/azure.env`, que fica fora do Git) |
| [`scripts/script-bd.sql`](scripts/script-bd.sql) | DDL das 12 tabelas no Oracle |
| [`dockerfiles/Dockerfile.rabbitmq`](dockerfiles/Dockerfile.rabbitmq) | Imagem do RabbitMQ usada no ACI |
| [`dockerfiles/Dockerfile.api`](dockerfiles/Dockerfile.api) | Imagem da API, alternativa ao App Service |
| [`azure-pipeline.yml`](azure-pipeline.yml) | Pipeline com os stages **Build** (CI) e **Deploy** (CD) |

### Passo a passo

1. **Banco:** rode `scripts/script-bd.sql` no Oracle da FIAP (SQL Developer), com o seu usuário.
2. **Infraestrutura:** na raiz do repositório, rode:
   ```bash
   az login
   cp scripts/azure.env.example scripts/azure.env   # preencha os valores
   bash scripts/infra-azure.sh
   ```
3. **Azure DevOps** (https://dev.azure.com):
   - Crie a organização e um projeto **privado**. Convide o professor com acesso **Basic** na organização e **Project Administrators** no projeto.
   - Em **Repos**, importe este repositório.
   - Em **Project settings > Service connections**, crie uma conexão *Azure Resource Manager* chamada `sc-paradacerta`, apontando para o grupo `rg-paradacerta`.
   - Em **Pipelines > Library**, crie o variable group `paradacerta-secrets` com as variáveis abaixo, todas marcadas como secretas (cadeado), usando os mesmos valores do `azure.env`:
     - `DB_USERNAME`, `DB_PASSWORD`
     - `JWT_SECRET`, `HASH_SECRET`
     - `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
     - `OPENAI_API_KEY`
   - Em **Pipelines > New pipeline**, escolha *Azure Repos Git* e *Existing YAML file*, e selecione `/azure-pipeline.yml`.
   - Em **Repos > Branches > main > Branch policies**, ative:
     - revisor obrigatório (o próprio aluno pode aprovar);
     - vínculo obrigatório de work item;
     - o seu RM como revisor padrão.
4. **Fluxo do dia a dia:**
   1. Crie a tarefa no Boards.
   2. Crie uma branch e faça um commit citando `#<id da tarefa>`.
   3. Abra um PR para a `main` e faça o merge.
   4. O Build roda e publica os testes e o `.jar`; o Deploy roda em seguida e publica na Web App.
   5. Confira o build publicado em `/actuator/info`, nos campos `versao` e `commit`.

### Variáveis de ambiente

- **Web App:** os segredos ficam nas *App Settings*. São definidos pelo script e atualizados pela pipeline a partir do variable group.
- **Repositório:** não há senha no código. O `application.yaml` só lê variáveis de ambiente.


## Declaração de uso de IA

Este projeto foi desenvolvido com o apoio de uma ferramenta de IA generativa (Claude, da Anthropic, via Claude Code). Ela ajudou a estruturar o código, os testes e esta documentação, e todo o conteúdo foi revisado pela equipe. Em tempo de execução, a aplicação usa um modelo da OpenAI, via Spring AI, para gerar a mensagem de voz do totem, com uma frase padrão como fallback.

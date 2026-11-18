# DECISIONS

> Registro das decisões técnicas da solução. As ambiguidades estão detalhadas em
> [`docs/REQUIREMENTS_ANALYSIS.md`](docs/REQUIREMENTS_ANALYSIS.md#5-ambiguidades); aqui ficam a escolha e o porquê.

## 1. Visão geral

Serviço REST de inserção e listagem de anúncios. Todo anúncio nasce `PENDING_MODERATION`, é submetido à API de moderação
fornecida pela OLX e passa a `APPROVED` ou `REJECTED` (com motivo). A listagem pública mostra apenas os aprovados.

Monólito modular em Java 21 + Spring Boot 3, com arquitetura hexagonal em três camadas:

- **domain** — Java puro: aggregate `Ad`, Value Objects e regras de transição de status;
- **application** — casos de uso, ports de entrada e saída, orquestração da moderação;
- **infrastructure** — REST, JPA/PostgreSQL, cliente HTTP da moderação, agendador, configuração.

Diagramas: [arquitetura](docs/architecture/architecture.mmd) · [sequência](docs/architecture/sequence-diagram.mmd).

## 2. Decisões técnicas

### 2.1 Linguagem e stack

**Problema:** o desafio permite Node, Kotlin, Go, Java ou Python.
**Escolha:** Java 21, Spring Boot 3.5, Maven, PostgreSQL, Flyway, Testcontainers, WireMock, Springdoc.
**Justificativa:** é a stack que domino em produção e que eu consigo explicar e alterar ao vivo; o ecossistema Spring
resolve validação, Problem Details (RFC 9457), health checks e logs estruturados sem bibliotecas extras.
**Descartado:** Spring Boot 4 (a linha 3.5 é a que uso no dia a dia e é suportada; migrar não traz ganho para este
problema); Kotlin (sem ganho relevante para o problema).

### 2.2 Arquitetura hexagonal em um monólito modular

**Problema:** separar regra de negócio de detalhes técnicos sem inflar o projeto.
**Alternativas:** (a) camadas tradicionais controller/service/repository; (b) hexagonal; (c) microsserviços.
**Escolha:** (b), em um único módulo Maven com pacotes `domain`, `application` e `infrastructure`.
**Vantagens:** domínio testável sem Spring; a dependência externa de moderação fica atrás de uma porta, então os testes
de aplicação controlam aprovação, reprovação e falha sem HTTP.
**Desvantagens:** mais mapeamentos (DTO ↔ domínio ↔ entidade JPA).
**Por que não (a):** a entidade JPA tende a virar o modelo de domínio e as regras vazam para services.
**Por que não (c):** um único bounded context, um time, sem necessidade de escala independente.
**Por que não módulos Maven separados:** a regra de dependência é verificável por revisão e pela ausência de imports de
Spring/JPA no pacote `domain`; módulos separados seriam cerimônia para este tamanho.

### 2.3 Modelagem do domínio

- `Ad` é o Aggregate Root. Construção via `Ad.create(...)` (estado inicial `PENDING_MODERATION`) e reconstituição via
  `Ad.restore(...)` (usado pela persistência).
- Value Objects onde há regra: `AdTitle` (10–100 caracteres), `Price` (> 0, até 2 casas), `ImageUrl` (URL absoluta
  http/https), `RejectionReason` (não vazio), `AdId` (UUID).
- Descrição é `String` validada no próprio aggregate: a única regra é ser obrigatória (e um limite técnico de tamanho);
  um Value Object não acrescentaria semântica.
- `Category` é um enum com as seis categorias do contrato de moderação.
- `RejectionReason` guarda o código devolvido pela moderação como texto, e não como enum: os três códigos documentados
  são exemplos do contrato atual, e um código novo do moderador não deve fazer o anúncio ficar pendente para sempre.
- Transições: `approve()` e `reject(reason)` só a partir de `PENDING_MODERATION`; qualquer outra lança
  `InvalidStatusTransitionException`.

### 2.4 Persistência

- PostgreSQL, schema gerido por Flyway (`ddl-auto=validate`).
- Tabelas `ads` e `ad_images` (imagens como coleção de Value Objects: PK `(ad_id, position)`, FK com `ON DELETE CASCADE`).
- `price numeric(12,2)`; datas `timestamptz` (`created_at`, `updated_at`, `moderated_at`).
- A gravação do resultado da moderação é um `UPDATE ... WHERE id = ? AND status = 'PENDING_MODERATION'`
  (`AdRepository.saveModerationOutcome`). Se a criação e o job de reprocessamento moderarem o mesmo anúncio ao mesmo tempo,
  só a primeira gravação vence; a segunda recebe `false` e devolve o que está no banco. Escolhi essa condição de estado em
  vez de `@Version` porque ela expressa a regra real ("só um pendente pode ser moderado") sem levar um campo técnico de
  versão para o aggregate.
- Índices parciais, alinhados às consultas reais:
  - `(created_at DESC, id DESC) WHERE status = 'APPROVED'` — listagem sem filtro;
  - `(category, created_at DESC, id DESC) WHERE status = 'APPROVED'` — listagem por categoria;
  - `(created_at) WHERE status = 'PENDING_MODERATION'` — busca do job de reprocessamento.
- Ordenação determinística: `created_at DESC, id DESC` (o seed tem várias datas repetidas).
- Paginação por offset no banco (`LIMIT/OFFSET` + `count`); imagens carregadas em lote (`@BatchSize`), evitando N+1 e o
  "fetch join + paginação em memória" do Hibernate.

### 2.5 Integração com a moderação

- Porta `ModerationGateway` na aplicação, retornando `ModerationDecision` (`Approved` | `Rejected(reason)`); falhas
  técnicas são `ModerationUnavailableException`. O domínio não conhece a API externa.
- Implementação `HttpModerationGateway` com `RestClient` (JDK HttpClient): URL, timeout de conexão, timeout de leitura,
  número de tentativas e backoff configuráveis.
- O campo do motivo: lê `rejection_reason` (o que o serviço de fato devolve) e aceita `reason` (documentado no
  `API.md`) como alternativa — ambiguidade A1.
- Cliente fixado em **HTTP/1.1**: por padrão o `HttpClient` do JDK tenta upgrade para HTTP/2 em texto puro (h2c), e o
  servidor de testes (WireMock) derrubava o stream (`RST_STREAM`). O mock oficial (Node `http`) fala HTTP/1.1. Bug real,
  pego pelos testes do gateway na primeira execução.
- Retentativa curta **apenas** para erros transitórios (timeout, falha de conexão, 429, 5xx), no máximo 3 tentativas,
  respeitando `Retry-After` com teto. 400 e resposta inválida não são retentados na mesma requisição.

### 2.6 Estratégia transacional

**Problema:** gravar o anúncio e consultar um serviço externo lento (até 2 s) e instável (20% de 500).
**Alternativas:** (a) uma transação envolvendo tudo; (b) moderar antes de gravar; (c) gravar pendente → moderar fora de
transação → gravar resultado; (d) outbox + worker assíncrono.
**Escolha:** (c). Duas transações curtas; nenhuma conexão de banco presa durante o HTTP.
**Por que não (a):** segura conexão do pool por segundos; uma falha HTTP desfaria a criação.
**Por que não (b):** se o banco falhar após a moderação, a decisão se perde; e o anúncio não teria id para rastrear.
**Por que não (d) agora:** resolveria latência e picos, mas exige worker, polling ou broker; para 4–6 h, (c) com o job de
reprocessamento cobre a consistência eventual com muito menos peças. É a evolução natural (seção 6).

Cenários de falha:

| Cenário | Resultado |
|---|---|
| Banco salva, moderação falha | anúncio fica `PENDING_MODERATION`; job reprocessa |
| Moderação responde, update no banco falha | anúncio fica `PENDING_MODERATION`; job consulta a moderação de novo (decisão é determinística para os mesmos dados) |
| Moderação indisponível | igual ao primeiro cenário; API responde 201 com `pending_moderation` |
| Resposta inválida | tratada como falha técnica, nunca como reprovação |
| Cliente repete a requisição | cria outro anúncio (limitação conhecida; evolução: `Idempotency-Key`) |

### 2.7 Reprocessamento de pendentes

Um `@Scheduled` na infraestrutura chama `RetryPendingModerationUseCase` em intervalo fixo, processando um lote limitado
de anúncios pendentes há mais de um tempo mínimo (para não competir com a moderação síncrona em andamento).
Lote e intervalo pequenos mantêm o consumo bem abaixo do rate limit de 5 req/s.
**Limitação:** em várias instâncias, duas poderiam pegar o mesmo anúncio; o `UPDATE` condicional impede gravação dupla,
mas haveria chamada duplicada à moderação. Evolução: `SELECT ... FOR UPDATE SKIP LOCKED` para reservar o lote, ou ShedLock.

### 2.8 API REST e erros

- `POST /api/v1/ads` → `201 Created` + `Location`, corpo com o status resultante;
- `GET /api/v1/ads?category=&page=&size=` → somente `APPROVED`, `page` base 0, `size` padrão 20 e máximo 100;
- `GET /api/v1/ads/{id}` → qualquer status, com `rejectionReason` quando reprovado; 404 se não existir.
- Erros em Problem Details (RFC 9457) via `@RestControllerAdvice`, com `code`, `errors` por campo e `correlationId`.
- Validação em dois níveis, sem duplicar regra: Bean Validation no DTO cuida da **forma** (campos presentes, tipos);
  as **regras de negócio** (tamanho do título, preço, imagens) vivem só no domínio, e suas exceções viram 400 com o campo.

### 2.9 Observabilidade

Logs estruturados nativos do Spring Boot (`logging.structured.format.console`), `X-Request-Id` propagado no MDC e
devolvido no header e nos erros, logs da integração externa com tentativa, status HTTP e duração, e Actuator `health`.

### 2.10 Estratégia de testes

| Nível | Ferramenta | Foco |
|---|---|---|
| Domínio | JUnit 5 | invariantes e transições, sem Spring |
| Aplicação | JUnit 5 + Mockito (só nas portas) | aprovação, reprovação, falha, pendente, listagem, 404 |
| Persistência | Testcontainers PostgreSQL | migrations, mapeamento, imagens, paginação, filtro, FK |
| API | Spring Boot Test + MockMvc + Testcontainers + WireMock | contrato HTTP, códigos, Problem Details, DTOs |
| Integração externa | WireMock | aprovado, reprovado (`reason` e `rejection_reason`), timeout, 4xx, 5xx, 429, resposta inválida, conexão recusada |
| Mock real | roteiro manual documentado no README | validação ponta a ponta |

Sem meta percentual de cobertura; JaCoCo gera relatório informativo (resultado obtido em `docs/FINAL_REVIEW.md`).

### 2.11 Testcontainers 1.21.4 (Docker Engine 29)

**Problema:** com o Docker Desktop atual (Engine 29.9.3), os testes de integração não subiam:
`client version 1.32 is too old. Minimum supported API version is 1.40`. O Testcontainers 1.21.3, gerenciado pelo
Spring Boot 3.5.7, pede a API 1.32 por padrão.
**Alternativas:** (a) `src/test/resources/docker-java.properties` com `api.version=1.44`; (b) baixar a versão mínima de API
no daemon do Docker; (c) atualizar o Testcontainers para a versão corrigida; (d) migrar para Spring Boot 4 / Testcontainers 2.
**Escolha:** (c), `testcontainers.version=1.21.4` no `pom.xml`.
**Justificativa:** (a) funcionou, mas era um contorno: cada projeto precisaria repetir. (b) exige mudar a máquina de quem
avalia. (d) é uma migração grande demais para o problema. A 1.21.4 é a correção oficial na mesma linha.

## 3. Ambiguidades

Resumo (detalhes e impacto em `docs/REQUIREMENTS_ANALYSIS.md`):

- **A1** `reason` × `rejection_reason`: aceitar os dois, **preferindo o que o serviço realmente devolve**
  (`rejection_reason`). Primeira versão preferia o documentado, seguindo a hierarquia "documentação acima do código do
  mock"; revisei: numa integração real o comportamento observado é a verdade operacional e a documentação é que está
  desatualizada. O campo documentado segue aceito, para a integração não quebrar se o serviço for alinhado à doc.
  O desempate só importa se os dois campos vierem juntos, e está fixado em teste
  (`HttpModerationGatewayTest.prefersFieldReturnedByTheServiceWhenBothArePresent`).
- **A2** listagem mostra apenas aprovados.
- **A3** imagens são URLs.
- **A4** status expostos em minúsculas (`rejected`, conforme o README).
- **A5** falha técnica mantém pendente + reprocessamento.
- **A6** `POST` sempre 201.
- **A7** faixa de preço é decisão da moderação, não replicada.
- **A8** categoria validada localmente.
- **A9** título: `trim` + code points.
- **A10** preço com até 2 casas decimais.
- **A11** seed carregado por script que chama a própria API, com imagem placeholder.
- **A12** `POST` não idempotente nesta versão.
- **A13** limites técnicos: descrição ≤ 5.000, até 10 imagens, página ≤ 100.

## 4. Trade-offs

**Priorizado**
- Correção do fluxo de moderação: falha técnica nunca vira reprovação; nenhum anúncio se perde.
- Regras de negócio em um único lugar (domínio) e testadas sem Spring.
- Testes de integração com PostgreSQL real e de contrato do mock com WireMock, incluindo as falhas.
- Execução com um comando (`docker compose up --build`).

**Aceito conscientemente**
- `POST` síncrono: o cliente espera a moderação (0,1–2 s; ~8 s no pior caso com retentativas). Em troca, na maioria dos
  casos ele recebe o status final na própria resposta, sem fila nem worker.
- Paginação por offset com `count`: simples e suficiente para o volume do desafio; degrada em offsets altos.
- `POST` não idempotente: repetir a requisição cria outro anúncio.
- Erros de validação de negócio retornam o primeiro campo inválido, não todos (o domínio para na primeira invariante).

**Fora do escopo**
- Autenticação, edição e remoção de anúncios, upload de imagens (as imagens são URLs).

## 5. Tempo gasto

Aproximadamente **6 horas** até a validação ponta a ponta (estimativa da candidata), entre leitura dos enunciados,
análise, revisão do código gerado com IA, execução e correção dos testes e preparação do ambiente (instalação do Docker
Desktop com WSL 2 no Windows).

## 6. Melhorias futuras

1. **Idempotência:** header `Idempotency-Key` no `POST`, com uma tabela de chaves.
2. **Moderação assíncrona:** outbox + worker; `POST` respondendo 202; rate limiter global respeitando 5 req/s.
3. **Job de pendentes em várias instâncias:** `SELECT ... FOR UPDATE SKIP LOCKED` ou ShedLock, e limite de tentativas
   por anúncio. Hoje um pendente que recebe sempre resposta inválida é retentado indefinidamente, a no máximo 10
   chamadas a cada 30 s.
4. **Listagem:** paginação por cursor (keyset em `created_at, id`).
5. **Observabilidade:** métricas Micrometer da integração (latência, taxa de erro, retentativas, pendentes) e tracing.
6. **Testes de contrato** do cliente de moderação (ex.: Pact), para detectar divergências como `reason` × `rejection_reason`.

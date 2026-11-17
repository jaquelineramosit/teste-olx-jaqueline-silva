# Guia de Apresentação

> Material de apoio para a conversa com os avaliadores (até 1h30). Todas as respostas apontam para o código real.

---

## 1. Roteiro de ~10 minutos

| Min | Tema | O que mostrar |
|---|---|---|
| 0–1 | **Contexto** | Inserção e listagem de anúncios; todo anúncio passa por uma moderação externa lenta (até 2 s) e instável (20% de HTTP 500, rate limit de 5 req/s). |
| 1–2 | **Solução** | API REST com 3 endpoints, PostgreSQL, integração com o mock oficial sem alterá-lo. `docker compose up --build` sobe tudo. |
| 2–4 | **Arquitetura** | `docs/architecture/architecture.mmd`: domain (Java puro) ← application (casos de uso + portas) ← infrastructure (REST, JPA, HTTP, scheduler). Composição em `ApplicationConfig`. |
| 4–5 | **Domínio** | `Ad` (Aggregate Root), Value Objects `AdTitle`, `Price`, `ImageUrl`, `RejectionReason`; transições `approve()` / `reject(reason)` só a partir de `PENDING_MODERATION`. |
| 5–7 | **Fluxo de moderação** | `docs/architecture/sequence-diagram.mmd`: grava pendente → chama moderação fora de transação → grava resultado com `UPDATE ... WHERE status = PENDING`. Falha técnica ≠ reprovação; job reprocessa. |
| 7–8 | **Persistência** | `V1__create_ads.sql`: índices parciais para a listagem pública, `CHECK` motivo ⇔ `REJECTED`, imagens como coleção com `@BatchSize`. |
| 8–9 | **Testes** | 57 unitários + 19 de integração (PostgreSQL real via Testcontainers, moderação via WireMock). |
| 9–10 | **Decisões e trade-offs** | Síncrono + job em vez de fila; ambiguidade `reason` × `rejection_reason`; POST não idempotente (limitação conhecida). |

---

## 2. Perguntas técnicas prováveis

**Por que arquitetura hexagonal?**
A dependência mais arriscada do problema é a moderação externa. Com a porta `ModerationGateway`, os casos de uso (`CreateAdService`, `AdModerator`) são testados controlando aprovação, reprovação e indisponibilidade sem HTTP (`CreateAdServiceTest`). O domínio não importa Spring nem JPA — dá para conferir pelos imports do pacote `domain`.

**Por que monólito modular e não microsserviços?**
Um único bounded context (anúncio), um time, sem necessidade de escalar partes separadamente. Microsserviços trariam rede, deploy e consistência distribuída sem benefício. Os pacotes já separam as fronteiras caso um dia seja preciso extrair algo.

**Como o domínio foi modelado? Qual a responsabilidade do Aggregate Root?**
`Ad` garante as invariantes de criação (`Ad.create`: título, preço, descrição, 1 a 10 imagens, categoria) e é o único lugar onde o status muda. `approve`/`reject` lançam `InvalidStatusTransitionException` se o anúncio não estiver pendente; `reject` exige motivo. `Ad.restore` reconstitui do banco sem reaplicar regras de criação.

**Por que Value Objects?**
Onde há regra: `AdTitle` (10–100 caracteres, `trim` e code points), `Price` (> 0, até 2 casas), `ImageUrl` (URL absoluta http/https). A regra fica num lugar só e o tipo impede passar um `String` qualquer. A descrição ficou `String` porque a única regra é ser obrigatória — um VO não acrescentaria nada.

**Como a aplicação depende da API de moderação?**
Só da interface `ModerationGateway` (application/port/out), que devolve `ModerationDecision` (`Approved` | `Rejected(reason)`) ou lança `ModerationUnavailableException`. O `HttpModerationGateway` (infraestrutura) conhece URL, JSON, códigos HTTP e o campo `rejection_reason`.

**Como o sistema trata a indisponibilidade externa?**
No adapter: timeout de conexão (1 s) e de leitura (2,5 s), até 3 tentativas com backoff exponencial curto, respeitando `Retry-After` do 429 (com teto). Só erros transitórios são retentados (timeout, I/O, 429, 5xx); 400 e resposta inválida não. Esgotadas as tentativas, o `AdModerator` mantém o anúncio `PENDING_MODERATION` e a API responde 201 com esse status. O `PendingModerationScheduler` chama `RetryPendingModerationUseCase` a cada 30 s, em lotes de 10.

**Como evitar inconsistência entre banco e serviço externo?**
Duas transações curtas e nenhuma conexão presa durante o HTTP. Se o banco falhar depois da moderação, o anúncio continua pendente e o job consulta de novo (a decisão é determinística para os mesmos dados). Se a requisição e o job moderarem ao mesmo tempo, `saveModerationOutcome` usa `UPDATE ... WHERE status = 'PENDING_MODERATION'`: só o primeiro vence, e o segundo recebe `false` e devolve o que está no banco (`AdModeratorTest`, `AdPersistenceAdapterIT.doesNotOverwriteAnOutcomeAlreadyStored`).

**Como funciona a paginação?**
Offset no banco (`PageRequest` → `LIMIT/OFFSET` + `count`), `page` base 0, `size` de 1 a 100, ordenação `created_at DESC, id DESC`. O `id` desempata — o seed tem 57 anúncios em só 28 datas distintas (`ordersDeterministicallyWhenCreationDatesTie`).

**Quais índices são importantes?**
Índices parciais só sobre o que é consultado: `(created_at DESC, id DESC) WHERE status = 'APPROVED'`, `(category, created_at DESC, id DESC) WHERE status = 'APPROVED'` e `(created_at) WHERE status = 'PENDING_MODERATION'` para o job. Pendentes e reprovados não ocupam os índices da vitrine.

**Como evitar N+1?**
Imagens são `@ElementCollection` com `@BatchSize(size = 100)`: uma página de até 100 anúncios faz 1 consulta da página, 1 `count` e 1 consulta de imagens com `IN`. Não usei `fetch join` com paginação porque o Hibernate pagina em memória nesse caso.

**Quais são os possíveis gargalos?**
1. Latência do `POST`: a moderação síncrona leva até 2 s, ou ~8 s no pior caso com retentativas.
2. Rate limit de 5 req/s do moderador limita a vazão de criação.
3. `count(*)` e offset alto em tabelas grandes.
4. O job de reprocessamento em várias instâncias pode duplicar chamadas.

**Como evoluiria para maior volume?**
- **Criação:** `POST` devolve 202 e a moderação vai para um worker (outbox + fila), com rate limiter global.
- **Listagem:** paginação por cursor (keyset em `created_at, id`) em vez de offset e `count`.
- **Job:** `SELECT ... FOR UPDATE SKIP LOCKED` para reservar lotes entre instâncias.
- **Leitura:** réplicas de leitura ou cache da primeira página por categoria.

**Em quais situações introduziria mensageria?**
Quando a latência do `POST` deixar de ser aceitável, quando a vazão de criação passar do rate limit do moderador, ou quando outros consumidores precisarem do evento "anúncio aprovado" (busca, notificação). Hoje o job de pendentes cobre a recuperação com menos peças.

**O que mudaria com mais tempo?**
`Idempotency-Key` no `POST`, outbox + worker, limite de tentativas por anúncio (hoje um pendente com resposta sempre inválida é retentado indefinidamente, a ≤10 chamadas a cada 30 s), métricas (Micrometer) da integração e testes de contrato.

**Como usou a IA? Como validou?**
Ver `AI_USAGE.md`. Pontos concretos para citar: o bug de HTTP/2 (h2c) no cliente, o teste intermitente causado pelo read timeout de 300 ms numa partida a frio, e a troca do contorno `docker-java.properties` pela correção oficial (Testcontainers 1.21.4).

---

## 3. Exercícios de alteração ao vivo

### 3.1 Nova regra de validação — "descrição com no mínimo 20 caracteres"
- **Onde:** `Ad.validDescription` (domínio). Não colocar no DTO nem no controller.
- **Testes:** novo caso em `AdTest.requiresDescription` (ou um teste dedicado); ajustar descrições curtas em `AdFixtures` e no `validAd` do `AdApiIT` se ficarem abaixo do limite.
- **Por quê:** a regra de negócio tem uma fonte de verdade; o `ApiExceptionHandler` já converte `DomainValidationException` em 400 com o campo.

### 3.2 Novo filtro na listagem — "faixa de preço (`minPrice`, `maxPrice`)"
1. `AdController.list`: novos `@RequestParam` opcionais.
2. `ListAdsQuery`: novos campos (validar `min <= max`).
3. Porta `AdRepository.findPublished`: receber um objeto de filtro em vez de mais parâmetros soltos.
4. `AdPersistenceAdapter` / `AdJpaRepository`: trocar os métodos derivados por `Specification` ou `@Query` (com 3 filtros opcionais, métodos derivados explodem em combinações).
5. Índice: avaliar `(category, price)` parcial em `APPROVED` só se a consulta pedir.

**Testes:** `QueryServicesTest`, `AdPersistenceAdapterIT` (filtro e paginação) e `AdApiIT`.

### 3.3 Modificar uma transição — "permitir reenviar à moderação um anúncio reprovado (após edição)"
1. Novo método no aggregate, por exemplo `Ad.resubmit()`: só a partir de `REJECTED`, volta para `PENDING_MODERATION` e limpa `rejectionReason`.
2. Ajustar o `CHECK` do banco com uma nova migration `V2__...`. Nunca editar a `V1`.
3. Persistência: hoje só existe `saveModerationOutcome` (que exige o anúncio pendente). É preciso uma nova operação de gravação para a reabertura.

**Testes:** novos casos em `AdTest` (transição válida e inválida a partir de `APPROVED`) e `AdPersistenceAdapterIT`.

### 3.4 Criar um novo teste — "rejeição por preço fora da faixa ponta a ponta"
No `AdApiIT`, stub `{"status":"rejected","rejection_reason":"price_out_of_range"}`, criar o anúncio e verificar o `GET /{id}` com `status = rejected` e o motivo, além de a listagem por categoria não trazer o anúncio.

### 3.5 Alterar uma resposta da API — "expor `published: true/false`"
- **Onde:** só em `AdResponse.from` (`ad.status().isPublished()`). Domínio e casos de uso não mudam.
- **Testes:** `AdApiIT.createsApprovedAd` (`jsonPath("$.published")`). A documentação OpenAPI se atualiza sozinha pelo record.

---

## 4. Comandos úteis durante a apresentação

```powershell
mvn clean verify                              # todos os testes + relatório JaCoCo em target/site/jacoco
mvn test -Dtest=AdTest                         # um teste específico
docker compose up --build -d                   # app + PostgreSQL + mock
docker compose --profile seed run --rm seed    # carrega os 57 anúncios do seed pela API
docker compose logs -f app                     # logs estruturados (ECS) com correlationId
```

Swagger: http://localhost:8080/swagger-ui.html · Health: http://localhost:8080/actuator/health

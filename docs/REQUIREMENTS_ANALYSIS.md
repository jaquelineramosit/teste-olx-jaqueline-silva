# Análise de Requisitos — Marketplace Ads API

> Documento produzido na fase de Discovery, antes da implementação.
> Fontes analisadas, em ordem de precedência:
> 1. `README.md` do Code Challenge (raiz do repositório oficial) e `backend/README.md` (desafio Backend);
> 2. `backend/api-mock/docs-candidato/API.md` (contrato documentado da moderação);
> 3. `backend/api-mock/server.js`, `Dockerfile`, `docker-compose.yml` e `seed/ads.json` (comportamento real do mock).

---

## 1. Requisitos funcionais

| ID | Requisito | Origem |
|---|---|---|
| RF01 | Criar anúncio com título, descrição, preço, categoria e ao menos 1 imagem (pode ser URL) | backend/README §1 |
| RF01.1 | Preço maior que zero | backend/README §1 |
| RF01.2 | Título entre 10 e 100 caracteres | backend/README §1 |
| RF01.3 | Pelo menos uma imagem | backend/README §1 |
| RF02 | Listar anúncios com paginação | backend/README §2 |
| RF02.1 | Filtro por categoria | backend/README §2 |
| RF03 | Antes de publicar, o anúncio passa pela API de moderação | backend/README §3 |
| RF03.1 | Reprovados ficam com status `rejected` | backend/README §3 |
| RF03.2 | O motivo da reprovação deve ficar visível | backend/README §3 |
| RF04 | Exibir um anúncio individual com seus dados e status | backend/README §4 |
| RF05 | Contrato da própria API documentado | backend/README "Entrega esperada" |
| RF06 | Persistência dos dados | backend/README "Entrega esperada" |

## 2. Regras de negócio

| ID | Regra | Onde será protegida |
|---|---|---|
| RN01 | Título com 10 a 100 caracteres (após `trim`, contados em code points) | Value Object `AdTitle` |
| RN02 | Preço > 0, em reais, no máximo 2 casas decimais | Value Object `Price` |
| RN03 | Ao menos uma imagem, cada uma uma URL absoluta `http`/`https` | `Ad` + Value Object `ImageUrl` |
| RN04 | Descrição obrigatória (o mock rejeita descrição vazia com 400) | `Ad` |
| RN05 | Categoria obrigatória e pertencente ao conjunto aceito pela moderação | enum `Category` |
| RN06 | Todo anúncio nasce `PENDING_MODERATION` | `Ad.create(...)` |
| RN07 | Só `PENDING_MODERATION` → `APPROVED` ou `PENDING_MODERATION` → `REJECTED` são transições válidas | `Ad.approve()` / `Ad.reject(reason)` |
| RN08 | Reprovação exige motivo, que é persistido e exposto | `Ad.reject(reason)` + `RejectionReason` |
| RN09 | Falha técnica da moderação **não** é reprovação: o anúncio continua pendente | `AdModerator` (aplicação) |
| RN10 | Apenas anúncios `APPROVED` são considerados publicados e aparecem na listagem | consulta do repositório |

As regras de faixa de preço por categoria, contato e itens proibidos **pertencem ao serviço de moderação** e não são replicadas no nosso domínio (ver A7).

## 3. Requisitos não funcionais relevantes

| ID | Requisito | Origem / motivação |
|---|---|---|
| RNF01 | Consumir o mock "como se fosse um serviço externo real" | backend/README |
| RNF02 | Timeouts configuráveis; latência do mock varia de 100 ms a 2 s | API.md "Observações", `server.js` |
| RNF03 | Tolerar falhas: o mock devolve HTTP 500 em 20% das chamadas por padrão (`ERROR_RATE=0.2`) | `server.js` |
| RNF04 | Respeitar rate limit de 5 req/s (HTTP 429 + `Retry-After`) | API.md, `server.js` |
| RNF05 | Paginação executada no banco, consultas sem N+1, ordenação determinística | desempenho (critério de avaliação) |
| RNF06 | Execução local com um comando ou poucos passos | README do Code Challenge |
| RNF07 | Testes dos fluxos críticos, determinísticos, sem dependência de serviços reais | README do Code Challenge |

## 4. Contrato de integração (moderação)

### 4.1 Endpoints

| Método | Caminho | Uso |
|---|---|---|
| `GET` | `/health` | `200 {"status":"ok"}` |
| `POST` | `/v1/moderation/check` | moderação de um anúncio |
| qualquer outro | — | `404 {"error":"not_found"}` |

Porta padrão `8787` (`PORT`).

### 4.2 Request `POST /v1/moderation/check`

```json
{ "title": "string", "description": "string", "price": 123.45, "category": "eletronicos" }
```

Validações do mock (`validate`): `title` e `description` strings não vazias após `trim`; `price` do tipo number; `category` em `eletronicos | moveis | veiculos | imoveis | esportes | colecionaveis`.
**Imagens não fazem parte do contrato** e não são enviadas.

### 4.3 Respostas

| HTTP | Corpo | Interpretação na nossa aplicação |
|---|---|---|
| 200 | `{"status":"approved"}` | decisão de negócio: aprovar |
| 200 | `{"status":"rejected","reason":"..."}` (doc) / `{"status":"rejected","rejection_reason":"..."}` (implementação) | decisão de negócio: reprovar com motivo (ver A1) |
| 200 | outro `status`, JSON malformado, rejeição sem motivo | **resposta inválida** → falha técnica |
| 400 | `{"error":"validation_error","details":[...]}` ou `{"error":"invalid_json"}` | falha técnica não retentável (indica bug nosso de contrato) |
| 429 | `{"error":"too_many_requests"}` + `Retry-After: 1` | falha técnica retentável |
| 500 | `{"error":"internal_error"}` | falha técnica retentável |
| timeout / conexão recusada | — | falha técnica retentável |

Motivos documentados: `contact_info_not_allowed`, `prohibited_item`, `price_out_of_range`.

### 4.4 Regras de decisão do mock (ordem de avaliação em `moderate`)

1. Contato no título/descrição (`whatsapp`, `zap`, `telegram`, padrão de telefone BR) → `contact_info_not_allowed`;
2. Item proibido (`arma(s)`, `munição`, `cigarro(s)`, `medicamento(s)`) → `prohibited_item`;
3. Preço fora da faixa da categoria → `price_out_of_range`;
4. Caso contrário → `approved`.

| Categoria | Mín. (R$) | Máx. (R$) |
|---|---|---|
| eletronicos | 10 | 50.000 |
| moveis | 20 | 30.000 |
| veiculos | 1.000 | 500.000 |
| imoveis | 10.000 | 5.000.000 |
| esportes | 5 | 20.000 |
| colecionaveis | 1 | 100.000 |

### 4.5 Comportamentos não documentados, observados no código

- O rate limit é verificado **antes** do erro aleatório e conta toda requisição `POST /v1/moderation/check` (janela deslizante de 1 s, em memória).
- O erro 500 aleatório e o 429 são devolvidos **sem latência**; a latência só existe nas respostas 200.
- O header `x-mock-force` é um gancho de teste: `rate-limit` força 429, `error` força 500 e **qualquer outro valor** desliga o 429 e o 500 aleatórios. Nossa aplicação **não** envia esse header; ele é usado apenas no roteiro de validação manual contra o mock real.
- Configuração por variáveis de ambiente: `PORT`, `ERROR_RATE`, `RATE_LIMIT_RPS`, `LATENCY_MIN_MS`, `LATENCY_MAX_MS`.

### 4.6 Seed

`seed/ads.json`: 6 categorias e 57 anúncios (`id`, `title`, `description`, `price`, `category`, `created_at`). Não há imagens nem status; não há anúncios de `colecionaveis`; existem apenas 28 valores distintos de `created_at` (empates de data reforçam a necessidade de desempate na ordenação). Uma simulação das regras de `moderate` sobre o seed aprova os 57 anúncios.

## 5. Ambiguidades

| # | Problema | Interpretações | Escolha | Justificativa | Impacto |
|---|---|---|---|---|---|
| A1 | `API.md` documenta o motivo em `reason`, mas `server.js` responde `rejection_reason` | (a) só `reason`; (b) só `rejection_reason`; (c) aceitar ambos | (c): ler `rejection_reason`, usar `reason` como alternativa | O que o serviço realmente envia é a verdade operacional; seguir só a documentação faria toda reprovação real virar "resposta inválida". Manter `reason` protege contra o alinhamento futuro do serviço à doc. (Revisado: a 1ª versão preferia o documentado.) | Adapter um pouco mais tolerante; testes cobrem os dois formatos e o desempate |
| A2 | "Lista os anúncios": quais estados aparecem? | (a) todos; (b) só aprovados; (c) filtro de status | (b): apenas `APPROVED` | Listagem de marketplace é a vitrine pública; o README diz "antes de publicar, passa pela moderação". Pendentes e reprovados seguem consultáveis por id | Um rejeitado nunca aparece como publicado |
| A3 | Formato das imagens | upload binário; lista de URLs | lista de URLs absolutas `http(s)` | O README permite URL; upload exigiria storage, fora do escopo | Não validamos se a URL existe |
| A4 | Nome dos status | interno x externo | domínio: `PENDING_MODERATION`, `APPROVED`, `REJECTED`; API: `pending_moderation`, `approved`, `rejected` | O README exige o valor `rejected` exposto | Mapeamento explícito no adapter REST |
| A5 | O que fazer quando a moderação falha | rejeitar; devolver erro 5xx e descartar; manter pendente | manter `PENDING_MODERATION`, retentativa curta no adapter e reprocessamento periódico | Falha técnica ≠ reprovação (requisito). Descartar perderia o dado do usuário | Cliente recebe o anúncio pendente; um job resolve depois |
| A6 | Código HTTP do `POST` | sempre 201; 201 x 202 conforme status | sempre `201 Created` + `Location` | O recurso foi criado em todos os casos; o `status` no corpo informa o resultado | Cliente precisa ler o `status` |
| A7 | Validar faixa de preço por categoria localmente? | replicar regras; delegar | delegar à moderação | Duplicar a regra criaria duas fontes de verdade; reprovação de preço é decisão do moderador | Preço fora da faixa resulta em `rejected` / `price_out_of_range` |
| A8 | Categoria inválida | aceitar e deixar a moderação devolver 400; validar localmente | validar localmente (400 na nossa API) | O 400 da moderação deixaria o anúncio pendente para sempre; é contrato, não regra de moderação | Lista de categorias espelha o contrato do mock |
| A9 | Contagem de caracteres do título | `length()` UTF-16; code points; com/sem espaços nas pontas | `trim` + code points | Evita que emojis contem 2 e que espaços inflem o tamanho | Documentado no Value Object |
| A10 | Precisão do preço | double; BigDecimal sem limite; 2 casas | `BigDecimal`, máximo 2 casas, `numeric(12,2)` | Valor monetário em reais; maior faixa do mock é 5.000.000 | Preço com 3+ casas → 400 |
| A11 | Seed sem imagens | ignorar seed; migration; script via API | script que usa `POST /api/v1/ads` com imagem placeholder (decisão da candidata) | Cada anúncio do seed passa pela moderação real | Imagem placeholder documentada como dado de demonstração |
| A12 | Requisição repetida pelo cliente | idempotência obrigatória; documentar | `POST` não idempotente nesta versão | Sem requisito explícito; `Idempotency-Key` documentado como evolução | Repetições criam anúncios duplicados |
| A13 | Limites técnicos (descrição, nº de imagens, tamanho de página) | sem limite; limites técnicos | descrição ≤ 5.000 caracteres, até 10 imagens, `size` ≤ 100 | Proteção do serviço, não regra de negócio; documentados como tal | 400 acima dos limites |

## 6. Restrições

- Não alterar nada em `backend/` (orientação da candidata) — o mock é usado como está, via `build` a partir de `backend/api-mock`.
- Sem microsserviços, filas ou transações distribuídas (proporcionalidade, prompt mestre).
- Esforço esperado de 4 a 6 horas.
- Ambiente de desenvolvimento: build e testes executados na máquina da candidata (Windows + Docker), pois o ambiente de nuvem usado pela IA não tem acesso ao Maven Central nem a Docker.

## 7. Matriz de rastreabilidade (planejada)

> Preenchida com os nomes reais de classes e testes ao final da implementação; o resultado efetivo das execuções fica em `docs/FINAL_REVIEW.md`.

| Requisito | Implementação (planejada) | Testes (planejados) |
|---|---|---|
| RF01 Criar anúncio | `AdController.create`, `CreateAdService`, `Ad.create` | `AdTest`, `CreateAdServiceTest`, `AdControllerIT` |
| RF01.1 Preço > 0 | `Price` | `PriceTest`, `AdControllerIT` (400) |
| RF01.2 Título 10–100 | `AdTitle` | `AdTitleTest`, `AdControllerIT` (400) |
| RF01.3 ≥ 1 imagem | `Ad.create`, `ImageUrl` | `AdTest`, `AdControllerIT` (400) |
| RF02 Listagem paginada | `ListAdsService`, `AdPersistenceAdapter.findPublished` | `ListAdsServiceTest`, `AdPersistenceAdapterIT`, `AdControllerIT` |
| RF02.1 Filtro por categoria | idem | idem |
| RF03 Moderação | `AdModerator`, `HttpModerationGateway` | `AdModeratorTest`, `HttpModerationGatewayTest` (WireMock) |
| RF03.1/03.2 Reprovação + motivo | `Ad.reject`, coluna `rejection_reason` | `AdTest`, `AdPersistenceAdapterIT`, `AdControllerIT` |
| RN09 Falha ≠ reprovação | `AdModerator`, `PendingModerationRetryService` | `AdModeratorTest`, `HttpModerationGatewayTest` |
| RF04 Detalhe + 404 | `GetAdByIdService`, `AdController.getById` | `GetAdByIdServiceTest`, `AdControllerIT` |
| RF05 Contrato documentado | Springdoc OpenAPI (`/swagger-ui.html`) | verificação manual + `AdControllerIT` |
| RF06 Persistência | Flyway + `AdPersistenceAdapter` | `AdPersistenceAdapterIT` |

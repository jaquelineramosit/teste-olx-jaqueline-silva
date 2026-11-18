# AI_USAGE

> Registro honesto de como a IA foi usada neste desafio. A responsabilidade por cada linha entregue é minha: revisei,
> executei e validei o que está aqui, e consigo explicar e alterar qualquer parte.

## 1. Ferramentas

| Ferramenta | Uso |
|---|---|
| **Claude (Anthropic), modelo Claude Opus 5.5, no app desktop do claude.ai** | Análise dos enunciados e do mock, proposta de arquitetura, geração do código, dos testes e da documentação, diagnóstico das falhas de teste. Pelo app, o assistente lia e gravava arquivos na pasta do projeto e lia os relatórios do Maven (`target/*-reports`). |
| Insomnia, PowerShell, Docker Desktop | Execução e validação manual, feitas por mim. |

O ambiente onde o assistente rodava **não conseguia compilar nem rodar os testes**: sem acesso ao Maven Central e sem
Docker. Por isso, **toda execução de `mvn` e `docker compose` foi feita por mim na minha máquina**, e os resultados foram
colados de volta na conversa. O assistente só compilou localmente, com `javac`, o domínio e a camada de aplicação.

## 2. Etapas

| Etapa | Como a IA foi usada | O que eu fiz |
|---|---|---|
| Análise | Leitura dos dois READMEs, do `API.md`, do `server.js` e do seed; levantamento de regras, ambiguidades e da matriz de rastreabilidade (`docs/REQUIREMENTS_ANALYSIS.md`). | Defini as restrições: o repositório já existia e a pasta `backend/` não podia ser alterada. Escolhi como popular a base (seed via API). |
| Arquitetura | Proposta hexagonal, modelo de domínio, estratégia transacional e de falhas, diagramas Mermaid. | Revisei as decisões e alternativas no `DECISIONS.md`. |
| Implementação | Geração do código das três camadas, migrations, Docker e script de seed. | Revisão; execução do build. |
| Testes | Geração dos testes unitários, de integração e de contrato com WireMock. | Execução em cada rodada; envio das falhas para diagnóstico. |
| Documentação | README, DECISIONS, AI_USAGE, guia de apresentação e revisão final. | Revisão; dados reais (tempo gasto, resultados). |
| Revisão | Conferência da entrega por md5 (assistente × minha máquina) e leitura dos relatórios de teste. | Questionei contornos e decidi a correção definitiva. |

## 3. Prompts e fluxos que funcionaram

- **Prompt mestre estruturado.** Comecei com um prompt longo definindo papel, hierarquia das fontes (READMEs > `API.md` >
  código do mock > decisões próprias), regras inegociáveis ("não invente comportamentos da moderação", "não declare
  testes executados sem executá-los"), fases e critérios de aceite. Isso evitou que a IA inventasse campos ou regras.
- **"Primeiro leia, depois proponha."** Pedir a análise antes do código fez a IA comparar o `API.md` com o `server.js`
  e encontrar a divergência `reason` × `rejection_reason` antes de escrever a integração.
- **Ciclo curto de execução.** "Rodei `mvn verify`, segue a saída" → diagnóstico → correção mínima → nova execução. Em
  cada rodada só um tipo de problema mudava.
- **Pedir a causa, não o remendo.** Para o erro do Docker, pedi primeiro o comando de diagnóstico, e só depois a correção.

## 4. Correções e rejeições

Situações reais, na ordem em que aconteceram:

1. **Contrato divergente detectado.** O `API.md` documenta `reason`; o mock responde `rejection_reason`. Seguir só a
   documentação faria toda reprovação real virar "resposta inválida". A IA propôs aceitar os dois, **preferindo o
   documentado**, por seguir à risca a hierarquia de fontes do prompt. **Eu revisei:** prefiro o campo que o serviço
   realmente devolve, mantendo o documentado como alternativa (ambiguidade A1). Validado contra o mock real: reprovação
   `contact_info_not_allowed` exibida corretamente.
2. **Restrições que a IA não tinha como saber.** Ela ia criar o repositório e propor alterações; eu corrigi: o
   repositório já existia e `backend/` não podia ser alterada. A solução ficou na raiz e o mock é usado via `build:`,
   sem modificação.
3. **Revisões de desenho antes da primeira execução.**
   - Controle de concorrência trocado de `@Version` (campo técnico no aggregate) para
     `UPDATE ... WHERE status = 'PENDING_MODERATION'`.
   - Um teste de título com emoji que passaria mesmo com a implementação errada foi reescrito para diferenciar code
     points de `length()`.
   - A montagem duplicada do cliente HTTP (configuração × teste) foi extraída para `HttpModerationGateway.create`.
   - Uma asserção sem sentido do header `Location` foi refeita.
4. **Bug real no código de produção (1ª execução: 48/57).** O `HttpClient` do JDK tentava HTTP/2 (h2c) e o servidor
   derrubava o stream (`RST_STREAM`). Correção: HTTP/1.1 explícito. O que revelou o bug foram as asserções sobre a
   *mensagem* do erro ("esperava HTTP 500, veio I/O error"). Os testes de timeout passavam por acaso, porque também
   esperavam falha.
5. **Entrega que não chegou ao disco.** A 2ª execução falhou igual à 1ª: o arquivo corrigido não tinha sido gravado na
   minha pasta (cópia em cache na ferramenta de transferência). A partir daí, cada entrega foi conferida por md5.
6. **Diagnóstico errado, corrigido pela evidência.** Um teste falhava de forma intermitente com 2 requisições em vez de 1.
   - **Hipótese 1 da IA:** vazamento do teste de timeout para o seguinte. Isolar esse teste não resolveu.
   - **Hipótese 2:** vazamento por servidor compartilhado. Um servidor por teste também não resolveu.
   - **Causa real**, achada medindo (o teste que falhava era sempre o primeiro da classe e levava 2,4 s): read timeout
     de 300 ms nos testes, estourado na partida a frio da JVM e do WireMock, o que gerava uma retentativa legítima.
   - **Correção:** timeout de 2 s nos testes e 300 ms só no teste de timeout. Pelo mesmo motivo, ajustei o timeout dos
     testes de API (500 ms → 2 s).
7. **Contorno rejeitado em favor da correção definitiva.** Os testes de integração não subiam no Docker Engine 29
   (`client version 1.32 is too old`). A IA propôs `docker-java.properties` com `api.version=1.44`, e funcionou.
   **Eu questionei** depender de um contorno. Após pesquisa, troquei pela correção oficial (Testcontainers 1.21.4 no
   `pom.xml`) e apaguei o arquivo. A mudança no `pom` fui eu que fiz.
8. **Commits.** A IA sugeriu três commits, um deles só para a troca de versão. Como o `pom.xml` ainda não tinha sido
   commitado, esse commit exigiria reconstruir uma versão antiga: seria histórico artificial. Ficaram dois commits.

## 5. Validação

- **Testes automatizados**, executados por mim (`mvn clean verify` e `mvn clean install`): 57 unitários + 19 de
  integração, 0 falhas; JaCoCo (unitários + integração) com 92% das instruções e 73% dos branches.
- **Execução real** com `docker compose up --build`: app, PostgreSQL e mock da OLX `healthy`.
- **Testes manuais contra o mock real** (Insomnia): aprovação, reprovação `contact_info_not_allowed`, 400 com Problem
  Details e `correlationId`, listagem filtrada sem o reprovado, consulta por id e health `UP`.
  Detalhes em `docs/FINAL_REVIEW.md`.
- **Revisão de regras:** cada requisito do enunciado foi rastreado até uma classe e um teste (`docs/FINAL_REVIEW.md`).
- **Revisão arquitetural:** o pacote `domain` não importa Spring nem JPA; os casos de uso dependem apenas das portas.
- **Integridade da entrega:** checksums conferidos entre o que a IA gerou e o que está no repositório.

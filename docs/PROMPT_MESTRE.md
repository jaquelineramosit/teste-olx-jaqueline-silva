# PROMPT MESTRE — DESAFIO TÉCNICO OLX
## Backend Engineer | Marketplace Ads API
### Arquitetura Hexagonal · DDD · SOLID · Clean Code · Testes · Documentação

---

# 1. PAPEL E RESPONSABILIDADES

Atue como um Staff Software Engineer / Principal Backend Engineer, com experiência em desenvolvimento de sistemas backend, arquitetura de software e engenharia de soluções escaláveis.

Você possui domínio avançado de:

- Java e Spring Boot.
- Arquitetura Hexagonal (Ports and Adapters).
- Domain-Driven Design (DDD).
- SOLID.
- Clean Code.
- Design Patterns.
- APIs REST.
- Sistemas distribuídos.
- Integrações com serviços externos.
- Persistência relacional.
- Testes unitários, integração e contrato.
- Observabilidade e resiliência.
- Performance e otimização de aplicações backend.

Sua responsabilidade é analisar, projetar, implementar, testar, documentar e revisar integralmente o desafio técnico de Backend proposto pelo Grupo OLX.

Você deverá atuar como responsável técnico pela solução, tomando decisões arquiteturais fundamentadas e garantindo que a implementação respeite os requisitos oficiais.

Não quero apenas uma API funcional.

Quero uma solução que demonstre maturidade de engenharia, clareza arquitetural, domínio das regras de negócio, qualidade de implementação e capacidade de evolução.

Entretanto, existe uma diretriz fundamental:

**A qualidade da solução será medida pela qualidade das decisões, e não pela quantidade de código, funcionalidades ou tecnologias utilizadas.**

A implementação deve ser proporcional ao problema.

Não adicione complexidade para demonstrar conhecimento. Utilize a solução mais simples que preserve as fronteiras arquiteturais, as regras de negócio e a qualidade do software.

---

# 2. FONTES OFICIAIS E HIERARQUIA DOS REQUISITOS

Você receberá dois documentos oficiais relacionados ao desafio.

## 2.1 README geral — Code Challenge Grupo OLX

Este documento estabelece:

- Propósito do desafio.
- Autorização e incentivo ao uso de IA.
- Processo de desenvolvimento.
- Repositório privado.
- Histórico incremental de commits.
- Entregáveis.
- Critérios de avaliação.
- Prazo e esforço esperado.
- Processo de apresentação técnica.
- Possibilidade de alteração do código ao vivo.

## 2.2 README específico — Desafio Técnico Backend

Este documento estabelece:

- Objetivo funcional.
- Requisitos da API.
- Regras de negócio.
- Funcionalidades obrigatórias.
- Integração com a API de moderação.
- Persistência.
- Contrato da API.

## 2.3 Documentação da API de moderação

A documentação localizada em:

`api-mock/docs-candidato/API.md`

É a fonte de verdade para a integração com o serviço externo.

Também deverá ser inspecionada a implementação disponibilizada em `api-mock`, quando necessária para compreender o comportamento do mock.

## 2.4 Hierarquia de decisões

Respeite a seguinte ordem:

1. Requisitos oficiais dos dois READMEs.
2. Contrato documentado da API de moderação.
3. Regras identificadas na implementação do mock.
4. Decisões arquiteturais justificadas para atender aos requisitos.
5. Boas práticas gerais de engenharia de software.

Não substitua requisitos oficiais por preferências pessoais.

Quando existirem ambiguidades, documente a interpretação adotada e sua justificativa.

Não invente regras que não estejam documentadas.

---

# 3. OBJETIVO DO SISTEMA

Desenvolver um serviço backend de inserção e listagem de anúncios de um marketplace.

A aplicação deverá permitir:

1. Criar anúncios.
2. Listar anúncios com paginação.
3. Filtrar anúncios por categoria.
4. Submeter anúncios à moderação automática.
5. Persistir os resultados da moderação.
6. Consultar detalhes individuais de anúncios.

O serviço deverá integrar-se à API de moderação fornecida pela OLX, consumindo-a como se fosse uma dependência externa real.

O projeto deverá ser funcional, testado, documentado e executável localmente.

---

# 4. REQUISITOS FUNCIONAIS

## RF01 — Criar anúncio

Implementar um endpoint REST para criação de anúncios.

Dados necessários:

- Título.
- Descrição.
- Preço.
- Categoria.
- Pelo menos uma imagem, podendo ser uma URL.

Validações obrigatórias:

- Preço maior que zero.
- Título entre 10 e 100 caracteres.
- Pelo menos uma imagem.

Os demais campos devem respeitar as regras de obrigatoriedade e o contrato definido pela aplicação e pelo mock.

Não adicione restrições de negócio que não tenham justificativa.

## RF02 — Listar anúncios

Implementar endpoint de listagem.

Requisitos:

- Paginação.
- Filtro opcional por categoria.
- Ordenação determinística.
- Consulta eficiente no banco de dados.

Defina claramente quais anúncios serão considerados visíveis na listagem, respeitando o ciclo de vida e os requisitos do desafio.

Não permita que anúncios rejeitados sejam apresentados como publicados.

Documente o comportamento adotado.

## RF03 — Moderação automática

Antes da publicação, o anúncio deve passar obrigatoriamente pelo serviço externo de moderação.

Requisitos:

- Consumir o mock fornecido.
- Respeitar integralmente seu contrato.
- Interpretar aprovação e reprovação.
- Persistir o resultado.
- Manter anúncios reprovados com status `rejected`.
- Persistir o motivo da reprovação.
- Disponibilizar o motivo na consulta individual.

Uma falha técnica de comunicação não deve ser interpretada como reprovação de negócio.

## RF04 — Consultar anúncio

Implementar endpoint de consulta individual.

Deve apresentar:

- Identificador.
- Título.
- Descrição.
- Preço.
- Categoria.
- Imagens.
- Status.
- Motivo da reprovação, quando aplicável.
- Datas relevantes.

Anúncios inexistentes devem retornar HTTP 404.

---

# 5. ANÁLISE OBRIGATÓRIA ANTES DA IMPLEMENTAÇÃO

Não comece escrevendo código.

Primeiro, compreenda completamente o projeto e os requisitos.

## 5.1 Inspeção do repositório

Analise:

- Estrutura de diretórios.
- Arquivos existentes.
- Configurações.
- Scripts.
- Dependências.
- Infraestrutura disponibilizada.
- Mock.
- Documentações.
- Instruções adicionais.

Não sobrescreva ou remova arquivos sem compreender sua finalidade.

Preserve o conteúdo do mock oficial.

## 5.2 Leitura integral dos documentos

Leia os dois READMEs oficiais.

Em seguida, leia:

`api-mock/docs-candidato/API.md`

Inspecione os arquivos necessários da implementação do mock.

Identifique:

- Endpoints.
- Métodos HTTP.
- Request bodies.
- Response bodies.
- Códigos HTTP.
- Categorias aceitas.
- Faixas de preço.
- Regras de aprovação.
- Regras de reprovação.
- Motivos retornados.
- Erros possíveis.
- Forma de inicialização.
- Configurações necessárias.

Não implemente a integração antes de compreender esse contrato.

## 5.3 Análise de ambiguidades

Identifique requisitos incompletos ou que permitam mais de uma interpretação.

Para cada ambiguidade:

1. Descreva o problema.
2. Apresente as interpretações possíveis.
3. Escolha uma abordagem.
4. Justifique a decisão.
5. Registre o impacto da escolha.

Se uma ambiguidade comprometer a correção da solução, apresente-a antes de implementar.

Não faça perguntas desnecessárias quando existir uma interpretação técnica segura e proporcional.

## 5.4 Documento de análise

Crie:

`docs/REQUIREMENTS_ANALYSIS.md`

Inclua:

- Requisitos funcionais.
- Regras de negócio.
- Requisitos não funcionais relevantes.
- Contrato de integração.
- Ambiguidades.
- Restrições.
- Matriz de rastreabilidade entre requisitos, implementação e testes.

---

# 6. ARQUITETURA DE SOFTWARE

Utilize arquitetura hexagonal, DDD, SOLID e Clean Code.

A solução deverá preferencialmente ser um monólito modular.

Não implemente microsserviços.

A arquitetura deverá apresentar separação clara entre domínio, aplicação e infraestrutura.

## 6.1 Arquitetura Hexagonal

### Domain

Responsável por:

- Entidades.
- Value Objects.
- Regras de negócio.
- Invariantes.
- Enums.
- Domain Services, somente quando necessários.
- Exceções de domínio.

O domínio não pode depender de:

- Spring.
- JPA.
- Hibernate.
- HTTP.
- PostgreSQL.
- DTOs de infraestrutura.
- Bibliotecas externas desnecessárias.

### Application

Responsável por:

- Casos de uso.
- Orquestração.
- Ports de entrada.
- Ports de saída.
- Coordenação das operações.
- Transações, quando apropriado.

Casos de uso esperados:

- CreateAdUseCase.
- ListAdsUseCase.
- GetAdByIdUseCase.

Crie outros casos de uso somente quando existir uma responsabilidade real que justifique sua separação.

### Infrastructure

Responsável por:

- Controllers REST.
- Implementações dos repositórios.
- Persistência JPA.
- Integração HTTP com a moderação.
- Configurações.
- Mapeamentos.
- Tratamento de erros HTTP.
- Observabilidade.

As implementações externas devem depender dos contratos definidos pelas camadas internas.

## 6.2 Domain-Driven Design

Considere o anúncio como conceito central do domínio.

Avalie uma modelagem com:

- Ad.
- AdId.
- AdTitle.
- AdDescription.
- Money.
- AdImage.
- Category.
- ModerationStatus.
- RejectionReason.

O Aggregate Root deverá proteger as invariantes do anúncio.

Value Objects devem ser utilizados quando agregarem semântica, validação ou proteção das regras.

Não crie classes artificiais apenas para aumentar a quantidade de componentes de domínio.

Regras fundamentais:

- Entidades de domínio não são entidades JPA.
- DTOs não devem ser utilizados como modelos de domínio.
- O domínio não conhece a API de moderação.
- O domínio protege as transições de estado.
- O domínio não depende de infraestrutura.
- O motivo da reprovação deve ser preservado quando existir uma reprovação.

## 6.3 SOLID

Respeite:

- SRP: responsabilidade única.
- OCP: extensibilidade sem modificações desnecessárias.
- LSP: contratos respeitados pelas implementações.
- ISP: interfaces específicas.
- DIP: dependência de abstrações.

Não crie interfaces sem necessidade.

A existência de uma interface deve representar uma fronteira arquitetural ou um contrato com utilidade real.

## 6.4 Clean Code

Utilize:

- Nomes expressivos.
- Métodos objetivos.
- Classes coesas.
- Baixo acoplamento.
- Tratamento consistente de erros.
- Eliminação de duplicações.
- Código fácil de testar.
- Separação entre regra de negócio e detalhes técnicos.

Evite:

- God Classes.
- Métodos excessivamente grandes.
- Controllers com regras de negócio.
- Repositórios com lógica de aplicação.
- Anotações de infraestrutura no domínio.
- Abstrações prematuras.
- Design Patterns desnecessários.
- Comentários que apenas repetem o código.
- Complexidade desnecessária.

---

# 7. STACK TECNOLÓGICA

Utilize preferencialmente:

- Java 21.
- Spring Boot 3.
- Maven.
- PostgreSQL.
- Spring Web.
- Spring Data JPA.
- Flyway.
- Bean Validation.
- JUnit 5.
- Mockito.
- Spring Boot Test.
- Testcontainers.
- WireMock, quando apropriado.
- JaCoCo.
- Springdoc OpenAPI.
- Docker e Docker Compose.

Antes de definir as versões:

1. Verifique o ambiente existente.
2. Confirme a compatibilidade entre dependências.
3. Utilize versões estáveis.
4. Evite dependências sem necessidade.

O README permite outras linguagens e frameworks. A escolha por Java deve ser tratada como decisão técnica da solução, não como exigência oficial da OLX.

Se o repositório já apresentar alguma restrição tecnológica, analise-a antes de modificar a stack.

Documente a escolha em `DECISIONS.md`.

---

# 8. MODELAGEM E CICLO DE VIDA DO ANÚNCIO

Defina explicitamente os estados do anúncio.

Considere:

- PENDING_MODERATION.
- APPROVED.
- REJECTED.

Esses nomes são sugestões de modelagem. O contrato externo não determina necessariamente os mesmos estados internos.

Documente:

- Estado inicial.
- Transições válidas.
- Transições inválidas.
- Significado de cada estado.
- Quais estados representam anúncios publicados.

Não permita que anúncios pendentes ou rejeitados sejam considerados aprovados.

O domínio deverá impedir alterações de status inválidas.

## 8.1 Fluxo de criação

Implemente o fluxo:

1. Receber requisição.
2. Validar entrada.
3. Construir o modelo de domínio.
4. Persistir o anúncio com estado inicial adequado.
5. Solicitar moderação.
6. Interpretar a resposta externa.
7. Atualizar o domínio.
8. Persistir o resultado.
9. Retornar a resposta ao cliente.

A estratégia de transações deverá ser cuidadosamente definida.

Não mantenha uma transação de banco aberta durante uma chamada HTTP externa sem justificativa.

Não utilize transações distribuídas.

Considere os seguintes riscos:

- O banco salva e a chamada externa falha.
- A moderação responde, mas a atualização no banco falha.
- O serviço externo fica indisponível.
- A resposta externa é inválida.
- O cliente repete a requisição.

Defina uma política coerente para esses cenários.

## 8.2 Tratamento de falhas externas

Uma indisponibilidade do serviço de moderação não significa que o anúncio foi rejeitado.

Avalie manter o anúncio pendente e permitir uma nova tentativa.

Considere:

- Timeout.
- Erros HTTP.
- Falhas de conexão.
- Respostas inválidas.
- Retentativas.
- Duplicidade de processamento.
- Consistência dos dados.

Não implemente retry indiscriminado.

Não adicione filas, mensageria ou infraestrutura distribuída sem necessidade.

Se uma recuperação automática não for implementada por limitação de escopo, documente claramente a limitação e como o fluxo poderia evoluir.

---

# 9. INTEGRAÇÃO COM O SERVIÇO DE MODERAÇÃO

Crie uma porta de saída que represente a capacidade de solicitar moderação.

Exemplo conceitual:

`ModerationGateway`

A aplicação deve depender dessa abstração.

A implementação HTTP deverá permanecer na infraestrutura.

Requisitos:

- URL configurável.
- Timeout configurável.
- Respeito integral ao contrato externo.
- Conversão de modelos externos para modelos internos.
- Tratamento de erros.
- Logs relevantes.
- Separação entre erro técnico e decisão de negócio.
- Não expor detalhes internos do mock na API pública.

Não utilize respostas simuladas na implementação de produção.

Os testes poderão utilizar WireMock para simular a dependência externa.

Disponibilize também um procedimento de execução e validação contra o mock real da OLX.

Não invente endpoints ou campos.

---

# 10. PERSISTÊNCIA

Utilize PostgreSQL.

Modele a persistência considerando:

- Anúncios.
- Imagens.
- Status de moderação.
- Motivo da reprovação.
- Datas relevantes.

Avalie uma estrutura com:

- ads.
- ad_images.

Considere:

- Chaves primárias.
- Chaves estrangeiras.
- Integridade referencial.
- Precisão decimal monetária.
- Índices justificados.
- Consultas eficientes.
- Ordenação determinística.

Utilize Flyway para migrations.

Não utilize `ddl-auto=create` ou `ddl-auto=update` como estratégia de gerenciamento de schema.

A paginação deve ser executada no banco de dados.

Evite carregar todos os registros em memória.

Não utilize banco em memória como substituto do PostgreSQL nos testes de integração.

---

# 11. API REST

Implemente os endpoints mínimos:

### POST /api/v1/ads

Criar anúncio.

### GET /api/v1/ads

Listar anúncios com paginação e filtro opcional por categoria.

### GET /api/v1/ads/{id}

Consultar anúncio individual.

Utilize:

- Métodos HTTP adequados.
- DTOs de entrada e saída.
- Validações.
- Códigos HTTP apropriados.
- Respostas consistentes.
- Tratamento centralizado de erros.

Não exponha entidades de domínio ou JPA.

## 11.1 Tratamento de erros

Implemente `@RestControllerAdvice`.

Utilize Problem Details, preferencialmente alinhado à RFC 9457, quando compatível com a stack escolhida.

As respostas devem apresentar:

- Status HTTP.
- Código identificador do erro.
- Mensagem compreensível.
- Detalhes de validação, quando aplicável.
- Identificador de correlação, quando disponível.

Não exponha:

- Stack traces.
- SQL.
- Credenciais.
- Informações internas sensíveis.

---

# 12. TESTES AUTOMATIZADOS

Os testes são parte fundamental da entrega.

A OLX não exige cobertura total. A qualidade será avaliada pela escolha dos cenários e pela capacidade dos testes de validar comportamentos importantes.

Não estabeleça uma meta artificial de cobertura percentual como condição para conclusão.

Utilize:

- JUnit 5.
- Mockito.
- Spring Boot Test.
- Testcontainers.
- WireMock, quando apropriado.

## 12.1 Testes unitários de domínio

Cubra:

- Criação de anúncio válido.
- Título com menos de 10 caracteres.
- Título com mais de 100 caracteres.
- Preço igual a zero.
- Preço negativo.
- Ausência de imagens.
- Transições válidas.
- Transições inválidas.
- Reprovação com motivo.

Os testes de domínio devem ser independentes de Spring e banco de dados.

## 12.2 Testes unitários de aplicação

Cubra:

- Criação aprovada.
- Criação rejeitada.
- Persistência correta.
- Consulta individual.
- Anúncio inexistente.
- Listagem paginada.
- Filtro por categoria.
- Falha de moderação.
- Timeout.
- Resposta externa inválida.

Utilize mocks nas fronteiras externas, preservando o comportamento real das regras de domínio.

## 12.3 Testes de integração

Utilize Testcontainers com PostgreSQL real.

Cubra:

- Persistência.
- Recuperação.
- Relacionamento com imagens.
- Atualização de status.
- Persistência do motivo.
- Paginação.
- Filtro por categoria.
- Migrations.
- Integridade referencial.

## 12.4 Testes de API

Cubra:

- Criação válida.
- Criação inválida.
- Listagem.
- Paginação.
- Filtro.
- Consulta individual.
- HTTP 404.
- Respostas de erro.
- Formato dos DTOs.

## 12.5 Testes da integração externa

Utilize WireMock ou solução equivalente.

Cubra:

- Aprovação.
- Reprovação.
- Timeout.
- HTTP 4xx.
- HTTP 5xx.
- Resposta inválida.
- Falha de conexão.

Também disponibilize um procedimento para validar a integração com o mock real.

## 12.6 Qualidade dos testes

Os testes devem ser:

- Determinísticos.
- Independentes da ordem de execução.
- Fáceis de compreender.
- Orientados a comportamento.
- Sem dependência de serviços externos reais na suíte automatizada.

Não crie testes redundantes apenas para aumentar a quantidade.

Ao final, apresente os resultados efetivamente obtidos.

---

# 13. DESEMPENHO E OBSERVABILIDADE

A avaliação considera se a performance é adequada ao problema e se o candidato compreende possíveis gargalos.

Implemente o necessário para uma aplicação saudável:

- Logs estruturados.
- Tratamento adequado de exceções.
- Configuração de timeout.
- Health checks, quando apropriado.
- Identificação de requisições.
- Logs relevantes da integração externa.

Considere:

- Paginação no banco.
- Índices adequados.
- Consultas sem N+1.
- Carregamento de imagens sem multiplicar indevidamente registros.
- Evitar chamadas externas desnecessárias.
- Evitar transações longas.

Não implemente uma plataforma complexa de observabilidade.

Documente possíveis gargalos e caminhos de evolução.

---

# 14. DOCUMENTAÇÃO OBRIGATÓRIA

A documentação é um dos critérios de avaliação mais importantes.

O avaliador deve conseguir compreender e executar o projeto sem depender de explicações externas.

## 14.1 README.md

O README da entrega deverá ser o cartão de visita do projeto.

Inclua:

1. Descrição.
2. Objetivo.
3. Tecnologias.
4. Arquitetura.
5. Organização dos pacotes.
6. Fluxo de criação e moderação.
7. Como executar.
8. Pré-requisitos.
9. Configurações.
10. Como iniciar PostgreSQL.
11. Como iniciar o mock.
12. Como executar testes.
13. Como acessar Swagger.
14. Exemplos de utilização.
15. Links para DECISIONS.md e AI_USAGE.md.
16. Links para os diagramas.

A execução deve exigir um comando ou poucos passos.

Valide efetivamente as instruções.

Não escreva comandos que não tenham sido testados ou cuja execução seja impossível no ambiente.

Preserve a documentação oficial do desafio e não modifique o contrato do mock.

## 14.2 DECISIONS.md

Este documento é obrigatório pela OLX.

Crie na raiz do projeto.

Inclua:

### Visão geral

Apresente a solução e a arquitetura.

### Decisões técnicas

Explique:

- Escolha da arquitetura hexagonal.
- Escolha do monólito modular.
- Modelagem do domínio.
- Estratégia de persistência.
- Integração com moderação.
- Estratégia transacional.
- Tratamento de falhas.
- Estratégia de testes.

### Alternativas consideradas

Para decisões relevantes, explique:

- Problema.
- Alternativas.
- Vantagens.
- Desvantagens.
- Escolha.
- Justificativa.
- Alternativas descartadas.

### Ambiguidades

Documente requisitos ambíguos, interpretação adotada e justificativa.

### Trade-offs

Explique o que foi priorizado e o que ficou fora do escopo.

### Tempo gasto

Registre o tempo aproximado realmente utilizado no desenvolvimento.

Não invente informações.

### Melhorias futuras

Descreva o que faria diferente com mais tempo.

O documento deve demonstrar raciocínio de engenharia, não apenas listar tecnologias.

## 14.3 AI_USAGE.md

Este documento é obrigatório pela OLX.

Registre honestamente:

### Ferramentas utilizadas

Liste as ferramentas realmente utilizadas.

### Etapas

Descreva como a IA foi utilizada em:

- Análise.
- Arquitetura.
- Implementação.
- Testes.
- Documentação.
- Revisão.

### Prompts utilizados

Apresente exemplos representativos dos prompts que funcionaram.

### Correções e rejeições

Registre situações reais em que:

- Uma sugestão foi rejeitada.
- Uma solução foi simplificada.
- Uma implementação foi corrigida.
- Um teste precisou ser ajustado.
- Uma resposta não respeitou o contrato.
- Uma decisão técnica precisou ser revista.

Explique os motivos.

Não invente situações.

### Validação

Descreva como o código foi validado:

- Execução de testes.
- Revisão das regras.
- Inspeção da integração.
- Revisão arquitetural.
- Testes manuais.
- Revisão final.

Deixe explícito que a responsabilidade pelo código entregue pertence ao candidato.

Não inclua histórico completo de conversas.

---

# 15. DIAGRAMAS DE ARQUITETURA

Crie diagramas utilizando Mermaid.

Eles devem refletir a implementação real.

## 15.1 Diagrama estrutural

Arquivo:

`docs/architecture/architecture.mmd`

Deve apresentar:

- Cliente HTTP.
- API REST.
- Controllers.
- Application.
- Use Cases.
- Ports de entrada.
- Ports de saída.
- Domain.
- Ad Aggregate.
- Persistence Adapter.
- PostgreSQL.
- Moderation Adapter.
- API externa de moderação.

Mostre claramente:

- Fronteiras arquiteturais.
- Direção das dependências.
- Responsabilidade de cada camada.
- Fluxo de comunicação.

## 15.2 Diagrama de sequência

Arquivo:

`docs/architecture/sequence-diagram.mmd`

Represente o fluxo de criação:

1. Cliente envia requisição.
2. Controller recebe os dados.
3. Caso de uso é acionado.
4. Domínio valida as invariantes.
5. Repositório persiste o anúncio.
6. Gateway solicita moderação.
7. Serviço externo responde.
8. Aplicação interpreta o resultado.
9. Domínio atualiza o status.
10. Repositório persiste o resultado.
11. API retorna a resposta.

Represente também o cenário de falha na comunicação externa, se compatível com a estratégia escolhida.

Inclua os diagramas no README.

Não represente componentes que não existam na implementação.

---

# 16. INFRAESTRUTURA E EXECUÇÃO

Disponibilize uma forma simples de execução local.

Avalie a utilização de:

- Dockerfile.
- docker-compose.yml.
- PostgreSQL.
- `.env.example`.

Não coloque credenciais reais no repositório.

Não altere a configuração original do mock sem necessidade.

Documente como executar os componentes.

A solução deve permitir que outro engenheiro consiga iniciar o projeto sem configurações complexas.

---

# 17. ESTRUTURA DO PROJETO

Organize o código de maneira coerente com a arquitetura escolhida.

Estrutura de referência:

```text
olx-marketplace/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── .../
│   │   │       ├── domain/
│   │   │       ├── application/
│   │   │       │   ├── port/
│   │   │       │   │   ├── in/
│   │   │       │   │   └── out/
│   │   │       │   └── usecase/
│   │   │       └── infrastructure/
│   │   │           ├── adapter/
│   │   │           │   ├── in/
│   │   │           │   └── out/
│   │   │           └── config/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │
│   └── test/
│       ├── java/
│       └── resources/
│
├── api-mock/
│
├── docs/
│   ├── architecture/
│   │   ├── architecture.mmd
│   │   └── sequence-diagram.mmd
│   ├── REQUIREMENTS_ANALYSIS.md
│   └── PRESENTATION_GUIDE.md
│
├── DECISIONS.md
├── AI_USAGE.md
├── README.md
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── pom.xml
```

Adapte a estrutura quando necessário.

Não crie pacotes vazios ou classes artificiais.

---

# 18. PROCESSO DE DESENVOLVIMENTO

Execute o trabalho em etapas.

Não gere todo o projeto de maneira desordenada.

## Fase 1 — Discovery

- Inspecionar os dois READMEs.
- Inspecionar o mock.
- Ler a documentação da moderação.
- Identificar os requisitos.
- Identificar ambiguidades.
- Criar matriz de rastreabilidade.

## Fase 2 — Arquitetura

- Definir o modelo de domínio.
- Definir ciclo de vida do anúncio.
- Definir casos de uso.
- Definir Ports and Adapters.
- Definir estratégia de persistência.
- Definir estratégia transacional.
- Definir tratamento de falhas.
- Criar diagramas.
- Registrar decisões.

## Fase 3 — Domínio

- Criar entidades.
- Criar Value Objects necessários.
- Implementar regras.
- Implementar transições de estado.
- Criar testes unitários.

## Fase 4 — Aplicação

- Criar Ports.
- Criar Use Cases.
- Implementar orquestração.
- Implementar testes unitários.

## Fase 5 — Infraestrutura

- Configurar Spring Boot.
- Configurar PostgreSQL.
- Implementar migrations.
- Implementar repositórios.
- Implementar integração de moderação.
- Implementar controllers.
- Implementar tratamento de erros.

## Fase 6 — Testes

- Testes de domínio.
- Testes de aplicação.
- Testes de integração.
- Testes de API.
- Testes da moderação.
- Validação contra o mock real.

## Fase 7 — Documentação

- README.
- DECISIONS.md.
- AI_USAGE.md.
- OpenAPI.
- Diagramas.
- Guia de apresentação.
- Configuração de execução.

## Fase 8 — Revisão

Faça uma revisão crítica da solução.

Verifique:

- Requisitos atendidos.
- Regras de negócio.
- Fronteiras arquiteturais.
- SOLID.
- Clean Code.
- Persistência.
- Integração.
- Tratamento de falhas.
- Testes.
- Performance.
- Documentação.
- Facilidade de execução.

---

# 19. GIT E HISTÓRICO DE DESENVOLVIMENTO

A OLX exige:

- Repositório privado novo.
- Não utilizar fork.
- Commits incrementais durante o desenvolvimento.

O histórico de commits será utilizado para compreender a evolução do raciocínio técnico.

Organize os commits por etapas coerentes.

Sugestões:

- chore: initialize project
- docs: document requirements and architecture
- feat: implement advertisement domain
- feat: implement advertisement use cases
- feat: integrate moderation service
- feat: implement REST API
- test: cover critical business flows
- docs: finalize project documentation

Adapte as mensagens ao trabalho efetivamente realizado.

Não crie commits artificiais ou retroativos.

**Importante:** não execute commits, push, publicação ou alteração de permissões do repositório sem autorização explícita do candidato.

Ao finalizar uma etapa:

1. Apresente os arquivos modificados.
2. Resuma as alterações.
3. Execute as verificações pertinentes.
4. Apresente o resultado.
5. Aguarde autorização para realizar o commit.

Não altere o histórico para simular uma evolução que não ocorreu.

---

# 20. CRITÉRIOS OFICIAIS DE AVALIAÇÃO

A solução será avaliada por pelo menos dois engenheiros do Grupo OLX.

Considere os seguintes critérios.

## Manutenibilidade

O código é legível e fácil de alterar?

As abstrações são justificadas?

A solução é proporcional ao problema?

## Desenho

As responsabilidades estão bem separadas?

As fronteiras arquiteturais estão claras?

As dependências estão na direção correta?

## Qualidade

Os testes cobrem os comportamentos importantes?

Validam comportamento real?

Seria fácil adaptá-los se uma regra mudasse?

## Documentação

O projeto pode ser compreendido sem precisar perguntar ao autor?

O README explica como executar?

As decisões estão documentadas?

## Desempenho

As consultas são eficientes?

A paginação funciona corretamente?

O candidato compreende os gargalos e como evoluir a solução?

## Decisões e trade-offs

As decisões são justificáveis?

Alternativas foram consideradas?

Ambiguidades foram tratadas explicitamente?

## Uso crítico de IA

O candidato compreende o código gerado?

Consegue explicar como validou as sugestões?

Consegue identificar e corrigir erros produzidos pela IA?

## Domínio da solução

O candidato consegue explicar qualquer parte do código?

Consegue realizar alterações?

**Não otimize a solução para quantidade de código, quantidade de testes ou tecnologias utilizadas. Otimize para esses critérios.**

---

# 21. PREPARAÇÃO PARA A APRESENTAÇÃO

A apresentação poderá durar até 1h30.

Os avaliadores poderão solicitar uma pequena alteração ou evolução do código ao vivo.

Crie:

`docs/PRESENTATION_GUIDE.md`

Prepare:

## 21.1 Roteiro de apresentação

Um roteiro objetivo de aproximadamente 10 minutos:

1. Contexto do problema.
2. Solução implementada.
3. Arquitetura.
4. Modelagem do domínio.
5. Fluxo de moderação.
6. Persistência.
7. Testes.
8. Principais decisões.
9. Trade-offs.

## 21.2 Perguntas técnicas

Prepare respostas fundamentadas para perguntas como:

- Por que arquitetura hexagonal?
- Por que monólito modular?
- Como o domínio foi modelado?
- Qual a responsabilidade do Aggregate Root?
- Por que utilizar Value Objects?
- Como a aplicação depende da API de moderação?
- Como o sistema trata a indisponibilidade externa?
- Como evitar inconsistência entre banco e serviço externo?
- Como funciona a paginação?
- Quais índices são importantes?
- Como evitar N+1?
- Quais são os possíveis gargalos?
- Como evoluiria a solução para maior volume?
- Em quais situações introduziria mensageria?
- O que mudaria com mais tempo?

Não produza respostas genéricas. Todas devem estar relacionadas ao código realmente implementado.

## 21.3 Exercícios de alteração ao vivo

Prepare exemplos de pequenas evoluções:

- Adicionar uma regra de validação.
- Adicionar um filtro na listagem.
- Modificar uma transição de status.
- Criar um novo teste.
- Alterar uma resposta da API.

Explique onde cada alteração deveria ser realizada e quais testes precisariam ser modificados.

O objetivo é garantir que o candidato compreenda a arquitetura e consiga evoluí-la.

---

# 22. REGRAS INEGOCIÁVEIS

1. Não ignore requisitos oficiais.
2. Não invente comportamentos da moderação.
3. Não altere o mock oficial.
4. Não misture domínio e infraestrutura.
5. Não coloque regras de negócio em controllers.
6. Não exponha entidades JPA.
7. Não classifique indisponibilidade externa como reprovação.
8. Não utilize mocks para substituir todos os testes de integração.
9. Não deixe implementações fictícias.
10. Não deixe TODOs importantes.
11. Não declare testes executados sem realmente executá-los.
12. Não declare funcionalidades implementadas sem evidências.
13. Não crie complexidade desnecessária.
14. Não implemente funcionalidades extras sem justificativa.
15. Não invente informações na documentação.
16. Não sobrescreva arquivos oficiais sem justificativa.
17. Não faça commits sem autorização.
18. Não utilize credenciais reais no código.
19. Não esconda limitações conhecidas.
20. Não considere a implementação concluída apenas porque o projeto compila.

Sempre apresente evidências das validações realizadas.

---

# 23. CRITÉRIOS DE ACEITE

O projeto só poderá ser considerado concluído quando os seguintes itens forem verificados:

## Funcionalidades

- [ ] Criar anúncio.
- [ ] Validar título.
- [ ] Validar preço.
- [ ] Validar imagens.
- [ ] Integrar com moderação.
- [ ] Aprovar anúncio corretamente.
- [ ] Rejeitar anúncio corretamente.
- [ ] Persistir motivo de reprovação.
- [ ] Listar anúncios.
- [ ] Paginar resultados.
- [ ] Filtrar por categoria.
- [ ] Consultar anúncio individual.
- [ ] Retornar HTTP 404 quando necessário.

## Arquitetura

- [ ] Domínio independente.
- [ ] Casos de uso organizados.
- [ ] Ports e Adapters bem definidos.
- [ ] Dependências orientadas corretamente.
- [ ] Regras de negócio protegidas.
- [ ] Ausência de abstrações desnecessárias.

## Qualidade

- [ ] Testes unitários executados.
- [ ] Testes de integração executados.
- [ ] Testes da API executados.
- [ ] Testes da moderação executados.
- [ ] Testes determinísticos.
- [ ] Sem erros conhecidos não documentados.

## Documentação

- [ ] README completo.
- [ ] DECISIONS.md.
- [ ] AI_USAGE.md.
- [ ] OpenAPI.
- [ ] Diagrama de arquitetura.
- [ ] Diagrama de sequência.
- [ ] REQUIREMENTS_ANALYSIS.md.
- [ ] PRESENTATION_GUIDE.md.

## Execução

- [ ] Aplicação executável.
- [ ] PostgreSQL funcional.
- [ ] Migrations funcionais.
- [ ] Mock integrado.
- [ ] Instruções validadas.
- [ ] `.env.example`.
- [ ] Ausência de credenciais reais.

Não marque itens como concluídos sem validação.

---

# 24. RELATÓRIO FINAL

Crie:

`docs/FINAL_REVIEW.md`

Inclua:

1. Resumo da solução.
2. Arquitetura implementada.
3. Principais decisões.
4. Requisitos atendidos.
5. Endpoints.
6. Persistência.
7. Integração externa.
8. Tratamento de falhas.
9. Testes implementados.
10. Resultados reais dos testes.
11. Cobertura obtida, se medida.
12. Instruções de execução.
13. Limitações conhecidas.
14. Melhorias futuras.

Inclua uma matriz de rastreabilidade:

| Requisito | Implementação | Teste | Resultado |
|---|---|---|---|
| Criar anúncio | Arquivo/classe | Teste | PASS/FAIL |
| Validar título | Arquivo/classe | Teste | PASS/FAIL |
| Validar preço | Arquivo/classe | Teste | PASS/FAIL |
| Listar anúncios | Arquivo/classe | Teste | PASS/FAIL |
| Paginação | Arquivo/classe | Teste | PASS/FAIL |
| Filtro | Arquivo/classe | Teste | PASS/FAIL |
| Moderação | Arquivo/classe | Teste | PASS/FAIL |
| Reprovação | Arquivo/classe | Teste | PASS/FAIL |
| Consultar anúncio | Arquivo/classe | Teste | PASS/FAIL |

Não marque PASS sem evidência.

---

# 25. PRAZO E ESFORÇO

O prazo oficial informado pela OLX é de 7 dias corridos a partir do recebimento.

O esforço esperado é de 4 a 6 horas de trabalho no total.

A solução deve respeitar esse contexto.

Não prolongue desnecessariamente o desenvolvimento.

Priorize:

1. Funcionalidade correta.
2. Arquitetura consistente.
3. Testes críticos.
4. Documentação.
5. Preparação para apresentação.

O objetivo é concluir uma entrega de qualidade dentro do esforço esperado.

---

# 26. INSTRUÇÃO FINAL DE EXECUÇÃO

Comece pela análise completa do repositório.

Não comece implementando código.

## Etapa inicial obrigatória

Primeiro:

1. Identifique os dois READMEs oficiais.
2. Leia ambos integralmente.
3. Leia `api-mock/docs-candidato/API.md`.
4. Inspecione o mock.
5. Identifique os requisitos.
6. Identifique ambiguidades.
7. Avalie a estrutura existente.

Depois apresente:

- Resumo do desafio.
- Matriz de requisitos.
- Contrato da moderação identificado.
- Arquitetura proposta.
- Modelo de domínio.
- Fluxo de criação e moderação.
- Estratégia de persistência.
- Estratégia de testes.
- Estrutura de diretórios.
- Principais decisões técnicas.

Crie a documentação inicial de análise e arquitetura.

Em seguida, prossiga com a implementação de forma incremental.

Não interrompa o trabalho apenas para apresentar planos ou pedir autorização para cada pequena decisão técnica.

Você tem autonomia para executar as etapas de análise, implementação, testes e documentação.

Solicite minha participação somente quando existir uma decisão relevante que dependa de uma preferência minha, uma ambiguidade que comprometa a correção ou uma ação externa que exija minha autorização.

Ao final de cada etapa:

- Apresente o que foi implementado.
- Informe os arquivos criados ou modificados.
- Informe os testes executados.
- Informe os resultados.
- Identifique pendências.

Não declare conclusão antecipadamente.

**OBJETIVO FINAL**

Entregar um backend de marketplace funcional, executável, testado e documentado, com arquitetura hexagonal, DDD, SOLID e Clean Code aplicados de maneira pragmática.

A solução deve atender integralmente aos requisitos técnicos do desafio e aos critérios oficiais de avaliação do Grupo OLX.

Mais importante: o candidato deve conseguir compreender, explicar, justificar e evoluir qualquer parte do código entregue.

Desenvolva como um engenheiro responsável pela solução, não como um gerador de código.
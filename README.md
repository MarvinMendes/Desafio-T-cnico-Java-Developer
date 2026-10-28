# activation-service

Serviço de ativação de produtos de clientes: registro de solicitações, consulta de status e integração com um provedor externo.

## Como executar

Pré-requisitos: JDK 21. Maven é opcional se usar o wrapper.

```bash
git clone <repositorio>
cd deroyque
./mvnw verify
./mvnw spring-boot:run
```

No Windows: `mvnw.cmd`.

| Recurso | URL |
| --- | --- |
| API | `http://localhost:8080/api/v1/requests` (Bearer JWT) |
| Liveness | `http://localhost:8080/actuator/health/liveness` |
| Readiness | `http://localhost:8080/actuator/health/readiness` |
| Prometheus | `http://localhost:8080/actuator/prometheus` |
| H2 (perfil `dev`) | `http://localhost:8080/h2-console` |

O perfil `dev` usa H2 e stub do provedor. Stack completa (app + PostgreSQL + WireMock):

```bash
docker compose up --build
```

A API exige JWT. Os testes geram o token via `SecurityMockMvcRequestPostProcessors.jwt()` (não há Authorization Server neste repositório).

Coleção Postman em `postman/`: importe `activation-service.postman_collection.json` e o environment `activation-service.local.postman_environment.json`.

## Decisões técnicas

| Escolha | Motivo |
| --- | --- |
| Java 21 + Spring Boot 3.5.3 | LTS e stack da vaga (MVC, Data JPA). `record` nos DTOs e `sealed` em `ProviderResult`. |
| Maven + wrapper | `./mvnw verify` único comando de build/testes. |
| PostgreSQL 16 + H2 | Relacional em produção; H2 para dev/teste. |
| Flyway + `ddl-auto=validate` | Schema versionado, Hibernate não altera tabelas. |
| Pacotes `api` / `application` / `domain` / `infrastructure` | Separação de responsabilidades sem camadas extras. |

ADRs em `docs/adr/`.

### Duplicidade (item 3)

Check-then-insert **não basta** sob corrida. A garantia é unique em `(owner_client_id, external_id)` mais `saveAndFlush` e tratamento de `DataIntegrityViolationException`.

| Cenário | Resposta |
| --- | --- |
| Mesmo `externalId` e mesmo payload | **200** com o registro existente (retry) |
| Mesmo `externalId` e payload diferente | **409** |
| Duas iguais em paralelo | Uma **201**, a outra **200** após a unique |

### Integração (item 5)

POST da API **não espera** o provedor. Commit → evento `AFTER_COMMIT` → HTTP fora da transação. Timeouts configuráveis. Header `Idempotency-Key` = `externalId`.

| Provedor | Status | Retry |
| --- | --- | --- |
| 2xx | COMPLETED | Não |
| 400 | FAILED | Não |
| 500 / 429 / 503 / timeout | PENDING, ou FAILED no limite | Sim, até `provider.max-attempts` |

### Segurança (item 7)

Implementado como Resource Server (Client Credentials no IdP da empresa). **Não** há servidor de autenticação neste repo.

- Assinatura: chave pública local; em produção, JWKS + `iss` + `aud`
- Escopos: `requests:write` (POST), `requests:read` (GET)
- BOLA: `owner_client_id` no JWT (`azp` / `client_id` / `sub`); outro cliente recebe 404
- Segredo do provedor: variável de ambiente / secret store, nunca no git

## Cenário de análise

Sintomas: API 300 ms → 2,5 s; CPU 45%; memória 60%; algumas queries 50 ms → 1,2 s; provedor 400 ms → 3 s.

**O que investigaria primeiro.** O recorte temporal (quando começou) e se a lentidão é em **todas** as rotas ou só no fluxo que chama banco + provedor. CPU e memória **não** estão saturados: não começaria por “falta de máquina”.

**Como separar aplicação, banco e integração.** Comparar três latências no mesmo `traceId`: tempo total HTTP (`http.server.requests`), query (`hikaricp` / logs Hibernate / `pg_stat_statements`) e chamada ao provedor (`activation.provider.calls`). Se o GET simples também ficou lento, o gargalo é banco/pool/locks. Se só o POST/processamento, olhar o provedor. Se o JVM gasta tempo em espera (`thread dump` com TIMED_WAITING em I/O), a app está **aguardando** dependências, não computando.

**Logs e métricas úteis.** `traceId`; `http.server.requests`; `activation.provider.calls` (p99 e taxa de erro); `activation.requests.current{status}`; pool Hikari (conexões ativas/pendentes); timeouts e 5xx do provedor; `pg_locks` / tempo de query.

**Melhorias possíveis.** Timeout e bulkhead no cliente HTTP; índices e `EXPLAIN` nas queries lentas; pool dimensionado; fila/outbox para não acoplar a API ao provedor (já é assíncrono no POST); cache só se for leitura repetida; circuit breaker se o provedor degradar o pool de threads.

**Causa comum (hipótese).** Contenção no pool de conexões HTTP **ou** JDBC: o provedor ficou lento (3 s), as threads da app ficaram presas na chamada, o pool saturado atrasou também as queries (1,2 s) e a API inteira (2,5 s), com CPU/memória ainda folgadas. Confirmar: p99 de `activation.provider.calls` sobe **antes** das queries; conexões Hikari em uso no teto; thread dump em `SocketRead`.

**Como validar a correção.** Comparar p50/p99 da API, do provedor e do banco no mesmo dashboard **antes/depois**, no mesmo horário e carga (ou teste de carga reproduzível). Critério: p99 da API volta à faixa anterior e as filas de pool não saturam. Feature flag / canary para reverter se o p99 não cair.

## Produção

Imagem multi-stage (`Dockerfile`): build JDK 21 Alpine, runtime JRE, usuário `app` não-root, `JAVA_TOOL_OPTIONS` com `MaxRAMPercentage=75`, healthcheck de liveness, `server.shutdown=graceful` (30 s). Compose sobe app + Postgres + WireMock.

Em produção real: registry da imagem → orquestrador (ECS/EKS/Cloud Run) com probes em `/actuator/health/liveness` e `/readiness`, variáveis de ambiente para datasource, `PROVIDER_BASE_URL`, JWKS/`issuer-uri`, secret do provedor no Secrets Manager. Réplicas atrás de um load balancer. Logs JSON (perfil `docker`) para o coletor. Tracing OTLP: `MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true` e endpoint do collector.

**Não** foram criados manifests Kubernetes/Terraform (a spec diz que não é necessário). Em EKS usaria Deployment + Service + HPA, probes HTTP, Secret/ConfigMap, e IRSA para o banco/secret; Terraform só para a infra compartilhada (cluster, RDS, IAM), não para a regra de negócio.

## Observabilidade

- Liveness = processo vivo; readiness inclui banco (não recebe tráfego se o Postgres cair)
- `traceId` no MDC, no `X-Request-Id` da API e na chamada ao provedor; o executor `@Async` copia o MDC
- Logs sem token/credencial; nível de security em INFO
- Métricas: latência da API (`http.server.requests`), `activation.provider.calls` / `activation.provider.errors`, `activation.requests` e `activation.requests.current{status}`
- JSON Logstash no perfil `docker`
- Ponte Micrometer Tracing + OpenTelemetry (export OTLP desligado por padrão)

## Microsserviços e cloud

Várias instâncias: a unique no Postgres serializa a duplicidade entre nós. Processamento concorrente da mesma linha usa `@Version`. Em escala, um worker único por `id` (outbox + fila) evita dois POSTs ao provedor; a `Idempotency-Key` cobre o resto.

Configuração: env vars. Segredos: AWS Secrets Manager / GCP Secret Manager / Kubernetes Secret, injetados no runtime, rotacionados sem rebuild.

Conclusão da ativação: evento `ActivationCompleted` (Kafka/SNS) com `id`, `externalId`, `customerId`, `status`, para os demais serviços. Outbox na mesma transação do update para `COMPLETED`.

CI/CD: GitHub Actions já roda `./mvnw verify` → build da imagem → scan → push no registry → deploy (ECS/EKS) com rollout e probes. Testcontainers no CI precisa de Docker.

## Melhorias futuras

- Outbox + consumer para entrega ao provedor e eventos de domínio
- JWKS, `iss` e `aud` obrigatórios
- Rate limit no Redis
- Circuit breaker quando o provedor degradar
- Reconciliação de solicitações presas
- Testcontainers no pipeline com Docker
- OpenTelemetry exportando para o collector da empresa
- Canary e dashboards de SLO (p99 da API e taxa de `FAILED`)

## Uso de IA

O Cursor foi usado como IA de apoio para geração da estrutura inicial do projeto, criação do README, geração das docs, da coleção do Postman e dos testes com base na implementação e code review.

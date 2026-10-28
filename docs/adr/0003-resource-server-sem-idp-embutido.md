# ADR 0003 — Resource Server sem Authorization Server no repositório

## Contexto

A API é consumida por sistemas. A spec pede OAuth 2.0/JWT e diz que não é necessário criar servidor de autenticação.

## Decisão

Spring Security Resource Server, escopos `requests:read` e `requests:write`, dono da solicitação extraído do JWT. Em testes, JWT mockado. Em produção, JWKS do IdP da empresa.

## Consequências

O repositório não emite tokens. Operação local autenticada depende de um IdP ou dos testes.

# ADR 0001 — Unicidade de `externalId` por consumidor

## Contexto

O sistema de origem pode reenviar o mesmo `externalId`. Duas requisições paralelas não podem gerar dois registros. Com vários consumidores, o mesmo id externo não deve vazar dados entre sistemas (BOLA).

## Decisão

Constraint unique em `(owner_client_id, external_id)`, captura de `DataIntegrityViolationException` e comparação de payload. Retry idêntico responde 200; payload divergente, 409.

## Consequências

A unicidade não é global: dois clientes podem usar o mesmo `externalId`. A regra de negócio vale por origem autenticada.

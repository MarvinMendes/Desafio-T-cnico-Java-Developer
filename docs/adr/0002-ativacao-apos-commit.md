# ADR 0002 — Ativação assíncrona após o commit

## Contexto

O provedor pode ser lento. Manter a transação aberta durante HTTP é arriscado. O POST precisa de tempo de resposta previsível.

## Decisão

Persistir `PENDING`, publicar evento e chamar o provedor só `AFTER_COMMIT`, fora da transação, com `Idempotency-Key`. Falha transitória volta para `PENDING` até o limite de tentativas.

## Consequências

O cliente consulta o status via GET. Se o processo cair entre commit e HTTP, a entrega fica best-effort até existir outbox/reconciliação.

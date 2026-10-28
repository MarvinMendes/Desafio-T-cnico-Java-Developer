ALTER TABLE activation_request
    ADD COLUMN owner_client_id VARCHAR(64) NOT NULL DEFAULT 'unknown';

ALTER TABLE activation_request
    DROP CONSTRAINT uk_activation_request_external_id;

CREATE UNIQUE INDEX uk_activation_request_owner_external
    ON activation_request (owner_client_id, external_id);

CREATE INDEX idx_activation_request_owner_created_at
    ON activation_request (owner_client_id, created_at DESC);

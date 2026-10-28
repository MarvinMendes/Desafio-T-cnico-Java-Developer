CREATE TABLE activation_request (
    id UUID NOT NULL,
    external_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_activation_request PRIMARY KEY (id),
    CONSTRAINT uk_activation_request_external_id UNIQUE (external_id)
);

CREATE INDEX idx_activation_request_status_created_at
    ON activation_request (status, created_at DESC);

CREATE INDEX idx_activation_request_customer_created_at
    ON activation_request (customer_id, created_at DESC);

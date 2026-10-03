CREATE TABLE fraud_assessments (
 id UUID PRIMARY KEY, assessment_request_id UUID NOT NULL UNIQUE, loan_application_id UUID NOT NULL,
 customer_reference UUID NOT NULL, requested_amount NUMERIC(19,2) NOT NULL CHECK (requested_amount > 0),
 currency VARCHAR(3) NOT NULL, evaluated_at TIMESTAMPTZ NOT NULL, ruleset_id VARCHAR(80) NOT NULL,
 ruleset_version VARCHAR(40) NOT NULL, fraud_score INTEGER NOT NULL CHECK (fraud_score BETWEEN 0 AND 100),
 risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('LOW','MEDIUM','HIGH')),
 decision VARCHAR(10) NOT NULL CHECK (decision IN ('PASS','REVIEW','BLOCK')),
 correlation_id VARCHAR(128) NOT NULL, request_hash CHAR(64) NOT NULL, assessment_payload JSONB NOT NULL
);
CREATE INDEX idx_fraud_assessments_customer_recent ON fraud_assessments(customer_reference, evaluated_at DESC);
CREATE INDEX idx_fraud_assessments_application ON fraud_assessments(loan_application_id, evaluated_at DESC);
CREATE TABLE fraud_outbox_events (
 id UUID PRIMARY KEY, event_id UUID NOT NULL UNIQUE, event_type VARCHAR(120) NOT NULL,
 event_version SMALLINT NOT NULL CHECK (event_version > 0), aggregate_id UUID NOT NULL,
 correlation_id VARCHAR(128) NOT NULL, payload JSONB NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 published_at TIMESTAMPTZ, status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','PROCESSING','PUBLISHED')),
 attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0), locked_at TIMESTAMPTZ
);
CREATE INDEX idx_fraud_outbox_pending ON fraud_outbox_events(occurred_at) WHERE status = 'PENDING';

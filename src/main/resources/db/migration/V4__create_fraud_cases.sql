CREATE TABLE fraud_cases (
    id UUID PRIMARY KEY,
    fraud_assessment_id UUID NOT NULL UNIQUE REFERENCES fraud_assessments(id),
    loan_application_id UUID NOT NULL,
    automated_decision VARCHAR(10) NOT NULL CHECK (automated_decision IN ('REVIEW', 'BLOCK')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED')),
    resolution VARCHAR(24) CHECK (resolution IN ('CLEARED', 'CONFIRMED_FRAUD')),
    resolution_note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    correlation_id VARCHAR(128) NOT NULL,
    CONSTRAINT ck_fraud_case_resolution_state CHECK (
      (status = 'RESOLVED' AND resolution IS NOT NULL AND resolved_at IS NOT NULL)
      OR (status <> 'RESOLVED' AND resolution IS NULL AND resolved_at IS NULL)
    )
);
CREATE INDEX idx_fraud_cases_queue ON fraud_cases(status, created_at DESC, id DESC);
CREATE INDEX idx_fraud_cases_application ON fraud_cases(loan_application_id, created_at DESC);

CREATE TABLE fraud_case_actions (
    id UUID PRIMARY KEY,
    fraud_case_id UUID NOT NULL REFERENCES fraud_cases(id),
    action VARCHAR(24) NOT NULL CHECK (action IN ('CASE_OPENED', 'REVIEW_STARTED', 'CASE_RESOLVED')),
    actor_id VARCHAR(200) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    resolution VARCHAR(24) CHECK (resolution IN ('CLEARED', 'CONFIRMED_FRAUD')),
    note VARCHAR(1000)
);
CREATE INDEX idx_fraud_case_actions_history ON fraud_case_actions(fraud_case_id, occurred_at, id);

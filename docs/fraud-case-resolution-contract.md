# Fraud case resolution event

Fraud Detection publishes `fraud.case.resolved.v1` after a case resolution commits. This is a versioned integration event in the existing event envelope (`eventId`, `eventType`, `eventVersion`, `occurredAt`, `aggregateType`, `aggregateId`, `correlationId`, `payload`); `aggregateType` is `FraudCase`. The payload carries `fraudCaseId`, `fraudAssessmentId`, `loanApplicationId`, `resolution` and `resolvedAt`. It does not carry analyst notes or personal data.

Loan Origination consumes the event idempotently and persists the manual disposition by fraud case ID. `CLEARED` can release the fraud gate while retaining the original automated REVIEW/BLOCK result. `CONFIRMED_FRAUD` does not release the gate. Credit decisions and application transitions remain owned by Loan Origination.

Delivery is at least once. Consumers must deduplicate by the stable fraud case identity and tolerate retries. Local standalone Kafka uses `localhost:29093`; the combined portfolio may use the existing broker on `localhost:29092` when its topic and consumer configuration are enabled.

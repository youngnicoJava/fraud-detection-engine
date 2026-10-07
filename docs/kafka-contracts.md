# Kafka contracts

---

### Request

Topic: `loan.fraud-assessment.requested.v1`; eventType `loan.fraud-assessment.requested.v1`; version 1. The envelope follows the portfolio's eventId/eventType/eventVersion/occurredAt/aggregateType/aggregateId/correlationId/payload shape. Payload fields are assessmentRequestId, loanApplicationId, customerReference, requestedAmount, currency, termMonths, productType, monthlyIncome, existingMonthlyDebtObligations, employmentStatus and employmentTenureMonths. It intentionally excludes name, email, subject identity and unprovided device/network/document signals.

---

### Result

Topic: `fraud.assessment.completed.v1`; eventType `fraud.assessment.completed.v1`; version 1. Payload includes assessmentRequestId, loanApplicationId, fraudAssessmentId, decision, fraudScore, riskLevel, reasonCodes, rulesetId/version and evaluatedAt.

Request ID is the assessment idempotency key. Identical material inputs replay the stored result; different material inputs return a conflict. Correlation ID is transport metadata and does not change the material request hash. The database unique key is the final duplicate guard. Result events use a deterministic event ID and are inserted in the same transaction as the assessment; an asynchronous publisher retries pending rows. Consumers must be idempotent because delivery is at-least-once. Malformed/unsupported request envelopes fail to the configured Kafka DLQ; transient application errors use SmallRye's retry behavior before DLQ.

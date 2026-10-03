# Fraud Detection Engine

Deterministic fraud-signal evaluation for loan origination. This is the third independent application in the banking portfolio. It does not make credit decisions and does not update loan applications.

## Architecture and scope

The code is organized by business capability, with API, application, domain and persistence packages inside the assessment capability. This modular layered structure keeps the signal/rule model cohesive while avoiding framework-wide controller/service/repository buckets. The engine uses no machine learning and no external identity, device, bureau or document provider.

## Local run

Requirements: Java 25, Maven and Docker Compose.

```powershell
docker compose up -d postgres
mvn quarkus:dev
```

The API listens on port 8083; Keycloak Dev Services uses 8182. Local development users are `fraud-analyst / fraud-analyst` (FRAUD_ANALYST) and `admin / admin` (ADMIN). These credentials are development-only.

For standalone Kafka, start `docker compose --profile standalone-kafka up -d` and set `FRAUD_KAFKA_ENABLED=true` and `KAFKA_BOOTSTRAP_SERVERS=localhost:29093`. For the full portfolio on one machine, reuse Credit Risk's broker on `localhost:29092`; the applications can run as host processes with their independent PostgreSQL databases. In an all-container setup, attach each compose project to a deliberately shared Docker network and use the broker service DNS name; do not share databases.

## API and operations

- `POST /api/v1/fraud-assessments`: direct deterministic evaluation (FRAUD_ANALYST, ADMIN)
- `GET /api/v1/fraud-assessments/{id}`: retrieve the persisted explanation
- `GET /q/health/live`, `GET /q/health/ready`
- `GET /metrics`, `GET /q/openapi`

A REVIEW response means suspicious signals warrant manual review; it does not reject the loan. BLOCK means a severe repeat-after-block signal was observed; Loan Origination owns any workflow consequence.

## Policy

The policy identifier is `loan-origination-fraud`, version `1.0.0`. It considers application velocity over 24 hours, amount escalation, material declared-income/debt changes, and rapid resubmission following a recent BLOCK. The score is bounded 0–100 and is not a calibrated probability. Thresholds are demonstration policy values, not a bank's proprietary rules. See [fraud policy](docs/fraud-policy.md) and [domain model](docs/domain-model.md).

## Verification and formatting

```powershell
docker compose up -d postgres
mvn spotless:apply
mvn spotless:check verify
```

CI runs Java 25 with Maven formatting verification and `mvn verify`.

## Configuration

Set `DB_USERNAME`, `DB_PASSWORD`, `DB_JDBC_URL`, `OIDC_AUTH_SERVER_URL`, `OIDC_CLIENT_ID`, `KAFKA_BOOTSTRAP_SERVERS`, `FRAUD_KAFKA_ENABLED`, and optional `OTEL_EXPORTER_OTLP_ENDPOINT` / `OTEL_EXPORTER_OTLP_ENABLED`. The production profile requires database and OIDC configuration; no production secrets are stored here.

## Portfolio boundaries

Loan Origination owns applications, offers and loan state. Credit Risk assesses eligibility and affordability. Fraud Detection assesses suspicious application behavior. They communicate through versioned Kafka contracts; there are no source-code dependencies or shared databases. Block 2 adds the analyst investigation experience.

# Local development

---

Standalone: run Fraud Detection PostgreSQL on 5435 and optional Kafka on 29093. With the whole portfolio running as local JVMs, keep the separate PostgreSQL services and reuse Credit Risk's Kafka broker exposed on 29092 by setting the same `KAFKA_BOOTSTRAP_SERVERS` value in each process. LO, CRE and Fraud Detection remain independent applications and databases.

When containerizing all applications, Docker service names are reachable only on a shared Docker network; create and attach to an explicitly named common network, then configure Kafka bootstrap using its network listener. Do not use localhost from one container to reach another container.

Enable the LO fraud integration with `FRAUD_ASSESSMENT_MODE=KAFKA`, `FRAUD_KAFKA_ENABLED=true`, `RISK_ASSESSMENT_MODE=KAFKA` and `RISK_KAFKA_ENABLED=true`. Set `FRAUD_KAFKA_ENABLED=true` in Fraud Detection as well. LO writes separate risk/fraud requests with the same assessment request ID and correlation ID. Credit APPROVE plus Fraud PASS clears automated gates. Credit REFER remains a credit manual-review path; Fraud REVIEW remains a fraud-review gate; Fraud BLOCK stops automatic progression without LO being directly rejected by the Fraud service. Credit REJECT stays owned by LO.

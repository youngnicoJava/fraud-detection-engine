# Fraud Detection Engine

Servicio independiente que detecta señales potencialmente sospechosas en solicitudes de préstamo y permite investigarlas. No calcula capacidad crediticia ni cambia directamente solicitudes o préstamos.

## Ecosistema

LO posee workflow; Credit Risk evalúa finanzas; Fraud busca señales de fraude. Son bounded contexts con persistencia separada, comunicados mediante contratos Kafka.

```mermaid
flowchart LR
 LO[Loan Origination] -->|loan.fraud-assessment.requested.v1| K[(Kafka)]
 K --> FD[Fraud Detection]
 FD -->|fraud.assessment.completed.v1| K
 FD -->|fraud.case.resolved.v1| K
 K --> LO
 A[Analista] --> UI[Consola React]
 UI -->|OIDC + REST| FD
 FD --> DB[(PostgreSQL Fraud)]
```

## Arquitectura y policy

Modular Layered Architecture por capability: API, application, domain y persistence. Policy loan-origination-fraud 1.0.0; determinista, sin ML, score 0–100 no probabilístico.

| Señal                                   | Regla                              | Puntos |
| --------------------------------------- | ---------------------------------- | -----: |
| HIGH_APPLICATION_VELOCITY               | ≥3 solicitudes/referencia en 24 h  |     35 |
| APPLICATION_AMOUNT_ESCALATION           | Importe >50% sobre previa reciente |     20 |
| MATERIAL_INCOME_CHANGE                  | Ingreso declarado cambia >50%      |     20 |
| MATERIAL_DEBT_CHANGE                    | Deuda declarada cambia >50%        |     15 |
| RAPID_RESUBMISSION_AFTER_BLOCK          | BLOCK previo dentro de 24 h        |     60 |
| EXTREME_VELOCITY_WITH_AMOUNT_ESCALATION | ≥6/24 h y subida >50%              |     60 |

LOW 0–24 → PASS; MEDIUM 25–59 y HIGH 60–100 → REVIEW normalmente. Reenvío rápido tras BLOCK o regla combinada extrema → BLOCK. REVIEW/BLOCK abre Fraud Case.

Assessment automático permanece inmutable. Case: OPEN → UNDER_REVIEW → RESOLVED; resolución CLEARED o CONFIRMED_FRAUD. Historial append-only CASE_OPENED/REVIEW_STARTED/CASE_RESOLVED. La resolución genera fraud.case.resolved.v1: CLEARED puede abrir gate de LO; CONFIRMED_FRAUD no. Fraud no aprueba/rechaza la solicitud.

## Roles, UI y API

Sólo FRAUD_ANALYST y ADMIN; no existe rol AUDITOR en esta API. Realm local fraud-detection, cliente SPA fraud-detection-console/PKCE. Usuarios dev fraud-analyst/fraud-analyst y admin/admin. Rutas /login, /assessments, /assessments/:id, /cases y /cases/:id. Lista de cases separada; detalle muestra assessment e historial.

| Método   | Endpoint                              | Función                          |
| -------- | ------------------------------------- | -------------------------------- |
| POST/GET | /api/v1/fraud-assessments             | Evaluar/listar                   |
| GET      | /api/v1/fraud-assessments/{id}        | Detalle                          |
| GET      | /api/v1/fraud-cases                   | Cola                             |
| GET      | /api/v1/fraud-cases/{id}              | Caso/historial                   |
| POST     | /api/v1/fraud-cases/{id}/start-review | Iniciar revisión                 |
| POST     | /api/v1/fraud-cases/{id}/resolve      | Resolver CLEARED/CONFIRMED_FRAUD |

## Kafka y fiabilidad

| Topic                                  | Productor → consumidor                   | Uso                 |
| -------------------------------------- | ---------------------------------------- | ------------------- |
| loan.fraud-assessment.requested.v1     | LO → Fraud; group fraud-detection-engine | Petición            |
| fraud.assessment.completed.v1          | Fraud → LO                               | Resultado           |
| fraud.case.resolved.v1                 | Fraud → LO                               | Disposición humana  |
| loan.fraud-assessment.requested.v1.DLQ | Consumer Fraud                           | Error no procesable |

assessmentRequestId deduplica persistente; clave igual/contenido distinto conflictúa. Estado/outbox commit atómico; publisher posterior at-least-once, no exactly-once. Correlation ID sigue eventos; acciones humanas posteriores pueden tener correlation nueva. Kafka deshabilitado por defecto.

## Desarrollo y operación

|  API |   DB | Keycloak | Frontend | Kafka standalone |
| ---: | ---: | -------: | -------: | ---------------: |
| 8083 | 5435 |     8182 |     5175 |            29093 |

Java25, Maven, Docker Compose, Bun. Desde raíz:

```powershell
docker compose up -d postgres
.\mvnw.cmd quarkus:dev
```

Otra terminal: cd frontend; bun install; bun run dev. API 8083, UI 5175. Para Kafka aislado: docker compose --profile standalone-kafka up -d y FRAUD_KAFKA_ENABLED=true; ecosistema local puede reutilizar broker Credit Risk 29092. No compartir DB.

Variables productivas: PORT, DB_USERNAME, DB_PASSWORD, DB_JDBC_URL, OIDC_AUTH_SERVER_URL, OIDC_CLIENT_ID, FRONTEND_ORIGIN, KAFKA_BOOTSTRAP_SERVERS, FRAUD_KAFKA_ENABLED, OTEL_*. Dev Services/usuarios demo sólo desarrollo. Health /q/health/live, /q/health/ready; métricas /q/metrics; OpenAPI /q/openapi; Swagger /q/swagger-ui. Flyway migrations, Hibernate validate, OTLP opcional, JSON logs prod.

```powershell
.\mvnw.cmd spotless:check verify
cd frontend
bun install --frozen-lockfile
bun run build
bun run format:check
```

## Alcance y competencias

Sin ML, bureau, identidad/dispositivo externo, KYC/AML, fraude de pagos ni investigación documental. Policy demostrativa, no confirmación legal. Demuestra modelado de reglas, Modular Layered, Quarkus/Java, PostgreSQL/Flyway, Kafka/outbox/dedupe/DLQ, OIDC/RBAC, historial append-only, React/TypeScript, health/métricas/OTel y CI. Licencia MIT: ver LICENSE.

| Proyecto                  | Dominio           | Arquitectura      | Responsabilidad        | Integración |
| ------------------------- | ----------------- | ----------------- | ---------------------- | ----------- |
| Loan Origination Platform | Lending           | Hexagonal         | Workflow préstamo      | Kafka       |
| Credit Risk Engine        | Riesgo crediticio | Clean             | Capacidad/eligibilidad | Kafka       |
| Fraud Detection Engine    | Fraude            | Modular por capas | Señales/casos          | Kafka       |

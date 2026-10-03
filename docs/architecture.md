# Architecture

Fraud Detection uses a modular layered architecture organized by capability. `fraudassessment/api` accepts REST and Kafka messages, `application` orchestrates idempotency and persistence, `domain` evaluates observations and rules, and `persistence` stores assessment history and the transactional result outbox. The choice keeps this deterministic policy small and cohesive; the other portfolio services demonstrate Hexagonal and Clean Architecture.

Signals are observations derived from real historical application inputs. Rules interpret those observations, produce stable reason codes, and add explicit score contributions. PostgreSQL stores immutable assessments and JSONB explanations so historical responses retain their ruleset and signal detail. Relational columns support unique idempotency and customer/time-window queries.

Flyway owns schema creation; Hibernate validates it. No framework dependency is placed in the policy model. Fraud cases, append-only case actions and assessment history use separate persistence mappings. Case resolution and its versioned integration event are committed with an outbox row in one database transaction; the publisher runs after commit and retries pending deliveries.

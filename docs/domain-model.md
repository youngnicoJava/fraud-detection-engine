# Domain model

A FraudAssessment is immutable and records request/application/customer references, time, ruleset ID/version, score, level, decision, signals, contributions and reason codes. Customer references are stable pseudonymous UUIDs supplied by Loan Origination; names, email, identity credentials and raw personal identifiers are not accepted.

Signals include HIGH_APPLICATION_VELOCITY, APPLICATION_AMOUNT_ESCALATION, MATERIAL_INCOME_CHANGE, MATERIAL_DEBT_CHANGE and RAPID_RESUBMISSION_AFTER_BLOCK. Signals are not verdicts. The risk score and PASS/REVIEW/BLOCK decision are stored separately.

Fraud Detection recommends an outcome only. Loan Origination coordinates the credit and fraud assessments and alone changes loan-application state.

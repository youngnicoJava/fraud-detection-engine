package com.frauddetection.fraudassessment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssessmentHistory(
    UUID assessmentRequestId,
    UUID customerReference,
    BigDecimal requestedAmount,
    BigDecimal monthlyIncome,
    BigDecimal existingDebt,
    Instant evaluatedAt,
    FraudDecision decision) {}

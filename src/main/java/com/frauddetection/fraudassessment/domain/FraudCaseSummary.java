package com.frauddetection.fraudassessment.domain;

import java.time.Instant;
import java.util.UUID;

public record FraudCaseSummary(
    UUID id,
    UUID assessmentId,
    UUID loanApplicationId,
    FraudDecision automatedDecision,
    int fraudScore,
    FraudRiskLevel riskLevel,
    FraudCaseStatus status,
    FraudCaseResolution resolution,
    Instant createdAt,
    Instant updatedAt) {}

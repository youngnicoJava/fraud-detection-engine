package com.frauddetection.fraudassessment.domain;

import java.time.Instant;
import java.util.UUID;

public record FraudAssessmentSummary(
    UUID fraudAssessmentId,
    UUID assessmentRequestId,
    UUID loanApplicationId,
    FraudDecision decision,
    int fraudScore,
    FraudRiskLevel riskLevel,
    Instant evaluatedAt,
    String rulesetId,
    String rulesetVersion,
    String correlationId) {}

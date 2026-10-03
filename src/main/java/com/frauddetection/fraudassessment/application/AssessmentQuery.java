package com.frauddetection.fraudassessment.application;

import com.frauddetection.fraudassessment.domain.FraudDecision;
import com.frauddetection.fraudassessment.domain.FraudRiskLevel;
import java.util.UUID;

public record AssessmentQuery(
    int page,
    int size,
    FraudDecision decision,
    FraudRiskLevel riskLevel,
    UUID loanApplicationId,
    UUID assessmentRequestId,
    String rulesetVersion) {
  public AssessmentQuery {
    if (page < 0 || size < 1 || size > 100)
      throw new IllegalArgumentException("Invalid page or size");
    if (rulesetVersion != null && rulesetVersion.isBlank()) rulesetVersion = null;
  }
}

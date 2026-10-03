package com.frauddetection.fraudassessment.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAssessment(
    UUID id,
    AssessmentInput input,
    Instant evaluatedAt,
    String rulesetId,
    String rulesetVersion,
    int fraudScore,
    FraudRiskLevel riskLevel,
    FraudDecision decision,
    List<FraudSignal> signals,
    List<ScoreContribution> contributions,
    List<String> reasonCodes) {
  public FraudAssessment {
    signals = List.copyOf(signals);
    contributions = List.copyOf(contributions);
    reasonCodes = List.copyOf(reasonCodes);
    if (fraudScore < 0 || fraudScore > 100)
      throw new IllegalArgumentException("Score must be 0..100");
  }
}

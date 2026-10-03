package com.frauddetection.fraudassessment.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudCase(
    UUID id,
    UUID assessmentId,
    UUID loanApplicationId,
    FraudDecision automatedDecision,
    FraudCaseStatus status,
    FraudCaseResolution resolution,
    String resolutionNote,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    String correlationId,
    List<FraudCaseAction> history) {
  public FraudCase {
    history = List.copyOf(history);
    if (status == FraudCaseStatus.RESOLVED && (resolution == null || resolvedAt == null))
      throw new IllegalArgumentException("A resolved case requires a resolution and timestamp");
    if (status != FraudCaseStatus.RESOLVED && (resolution != null || resolvedAt != null))
      throw new IllegalArgumentException("An unresolved case cannot have a resolution");
  }

  public FraudCase startReview(Instant at, FraudCaseAction action) {
    if (status != FraudCaseStatus.OPEN)
      throw new IllegalStateException("Only OPEN cases can start review");
    return new FraudCase(
        id,
        assessmentId,
        loanApplicationId,
        automatedDecision,
        FraudCaseStatus.UNDER_REVIEW,
        null,
        null,
        createdAt,
        at,
        null,
        correlationId,
        append(action));
  }

  public FraudCase resolve(
      FraudCaseResolution outcome, String note, Instant at, FraudCaseAction action) {
    if (status != FraudCaseStatus.UNDER_REVIEW)
      throw new IllegalStateException("Only cases under review can be resolved");
    return new FraudCase(
        id,
        assessmentId,
        loanApplicationId,
        automatedDecision,
        FraudCaseStatus.RESOLVED,
        outcome,
        note,
        createdAt,
        at,
        at,
        correlationId,
        append(action));
  }

  private List<FraudCaseAction> append(FraudCaseAction action) {
    var actions = new java.util.ArrayList<>(history);
    actions.add(action);
    return List.copyOf(actions);
  }
}

package com.frauddetection.fraudassessment.application;

import com.frauddetection.fraudassessment.domain.FraudAssessment;
import com.frauddetection.fraudassessment.domain.FraudCase;
import com.frauddetection.fraudassessment.domain.FraudCaseAction;
import com.frauddetection.fraudassessment.domain.FraudCaseActionType;
import com.frauddetection.fraudassessment.domain.FraudCaseResolution;
import com.frauddetection.fraudassessment.domain.FraudCaseStatus;
import com.frauddetection.fraudassessment.domain.FraudCaseSummary;
import com.frauddetection.fraudassessment.domain.FraudDecision;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FraudCaseService {
  private final FraudCaseRepository cases;
  private final FraudAssessmentRepository assessments;
  private final FraudCaseResolutionEventPort resolutionEvents;
  private final Clock clock;
  private final MeterRegistry metrics;

  @Inject
  public FraudCaseService(
      FraudCaseRepository cases,
      FraudAssessmentRepository assessments,
      FraudCaseResolutionEventPort resolutionEvents,
      Clock clock,
      MeterRegistry metrics) {
    this.cases = cases;
    this.assessments = assessments;
    this.resolutionEvents = resolutionEvents;
    this.clock = clock;
    this.metrics = metrics;
  }

  @Transactional
  public Optional<FraudCase> openForAssessment(FraudAssessment assessment) {
    if (assessment.decision() == FraudDecision.PASS) return Optional.empty();
    return cases
        .findByAssessmentId(assessment.id())
        .or(
            () -> {
              var now = assessment.evaluatedAt();
              var action =
                  new FraudCaseAction(
                      UUID.randomUUID(),
                      FraudCaseActionType.CASE_OPENED,
                      "system:fraud-policy",
                      now,
                      assessment.input().correlationId(),
                      null,
                      null);
              var fraudCase =
                  new FraudCase(
                      UUID.randomUUID(),
                      assessment.id(),
                      assessment.input().loanApplicationId(),
                      assessment.decision(),
                      FraudCaseStatus.OPEN,
                      null,
                      null,
                      now,
                      now,
                      null,
                      assessment.input().correlationId(),
                      List.of(action));
              cases.create(fraudCase, action);
              metrics
                  .counter("fraud_cases_created_total", "decision", assessment.decision().name())
                  .increment();
              return Optional.of(fraudCase);
            });
  }

  public AssessmentPage<FraudCaseSummary> list(FraudCaseQuery query) {
    return cases.list(query);
  }

  public FraudCaseDetails get(UUID id) {
    var fraudCase = cases.findById(id).orElseThrow(() -> new FraudCaseNotFoundException(id));
    var assessment =
        assessments
            .findById(fraudCase.assessmentId())
            .orElseThrow(() -> new IllegalStateException("Fraud case assessment is missing"));
    return new FraudCaseDetails(fraudCase, assessment);
  }

  @Transactional
  public FraudCase startReview(UUID id, String actorId, String correlationId) {
    var current = cases.findByIdForUpdate(id).orElseThrow(() -> new FraudCaseNotFoundException(id));
    var now = clock.instant();
    var action =
        new FraudCaseAction(
            UUID.randomUUID(),
            FraudCaseActionType.REVIEW_STARTED,
            actorId,
            now,
            correlationId,
            null,
            null);
    FraudCase updated;
    try {
      updated = current.startReview(now, action);
    } catch (IllegalStateException invalidTransition) {
      throw new FraudCaseConflictException(invalidTransition.getMessage(), invalidTransition);
    }
    cases.saveTransition(updated, action);
    return updated;
  }

  @Transactional
  public FraudCase resolve(
      UUID id, FraudCaseResolution resolution, String note, String actorId, String correlationId) {
    if (note != null && note.length() > 1000)
      throw new IllegalArgumentException("Resolution note is too long");
    var current = cases.findByIdForUpdate(id).orElseThrow(() -> new FraudCaseNotFoundException(id));
    var now = clock.instant();
    var action =
        new FraudCaseAction(
            UUID.randomUUID(),
            FraudCaseActionType.CASE_RESOLVED,
            actorId,
            now,
            correlationId,
            resolution,
            note);
    FraudCase updated;
    try {
      updated = current.resolve(resolution, note, now, action);
    } catch (IllegalStateException invalidTransition) {
      throw new FraudCaseConflictException(invalidTransition.getMessage(), invalidTransition);
    }
    cases.saveTransition(updated, action);
    resolutionEvents.recordResolved(updated, actorId, now, correlationId, UUID.randomUUID());
    metrics.counter("fraud_cases_resolved_total", "resolution", resolution.name()).increment();
    return updated;
  }

  public record FraudCaseDetails(FraudCase fraudCase, FraudAssessment assessment) {}
}

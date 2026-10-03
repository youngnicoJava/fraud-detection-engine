package com.frauddetection.fraudassessment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.fraudassessment.domain.AssessmentInput;
import com.frauddetection.fraudassessment.domain.FraudAssessment;
import com.frauddetection.fraudassessment.domain.LoanOriginationFraudPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;

@ApplicationScoped
public class AssessFraudService implements AssessFraudUseCase {
  private final FraudAssessmentRepository repository;
  private final LoanOriginationFraudPolicy policy;
  private final Clock clock;
  private final ObjectMapper mapper;
  private final MeterRegistry metrics;
  private final FraudCaseService cases;

  @Inject
  public AssessFraudService(
      FraudAssessmentRepository r,
      LoanOriginationFraudPolicy p,
      Clock c,
      ObjectMapper m,
      MeterRegistry metrics,
      FraudCaseService cases) {
    repository = r;
    policy = p;
    clock = c;
    mapper = m;
    this.metrics = metrics;
    this.cases = cases;
  }

  @Override
  @Transactional
  public FraudAssessment assess(AssessmentInput input) {
    return metrics.timer("fraud_assessment_processing_seconds").record(() -> assessOnce(input));
  }

  private FraudAssessment assessOnce(AssessmentInput input) {
    String hash = hash(input);
    var previous = repository.findByRequestId(input.assessmentRequestId());
    if (previous.isPresent()) {
      if (!hash(previous.get().input()).equals(hash)) {
        metrics.counter("fraud_assessment_idempotency_total", "result", "conflict").increment();
        throw new AssessmentRequestConflictException();
      }
      metrics.counter("fraud_assessment_idempotency_total", "result", "replay").increment();
      return previous.get();
    }
    var result =
        policy.evaluate(
            input, clock.instant(), repository.recentByCustomer(input.customerReference(), 200));
    repository.saveWithResultOutbox(result, hash);
    cases.openForAssessment(result);
    metrics.counter("fraud_assessments_total", "decision", result.decision().name()).increment();
    metrics
        .counter("fraud_assessments_by_risk_level_total", "risk_level", result.riskLevel().name())
        .increment();
    metrics.summary("fraud_assessment_score").record(result.fraudScore());
    metrics.summary("fraud_assessment_triggered_rules_count").record(result.contributions().size());
    return result;
  }

  private String hash(AssessmentInput input) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(mapper.writeValueAsBytes(new RequestFingerprint(input))));
    } catch (Exception e) {
      throw new IllegalStateException("Could not hash assessment request", e);
    }
  }

  private record RequestFingerprint(
      java.util.UUID assessmentRequestId,
      java.util.UUID loanApplicationId,
      java.util.UUID customerReference,
      java.math.BigDecimal requestedAmount,
      String currency,
      int termMonths,
      String productType,
      java.math.BigDecimal monthlyIncome,
      java.math.BigDecimal existingMonthlyDebtObligations,
      String employmentStatus,
      int employmentTenureMonths) {
    RequestFingerprint(AssessmentInput input) {
      this(
          input.assessmentRequestId(),
          input.loanApplicationId(),
          input.customerReference(),
          input.requestedAmount().stripTrailingZeros(),
          input.currency(),
          input.termMonths(),
          input.productType(),
          input.monthlyIncome().stripTrailingZeros(),
          input.existingMonthlyDebtObligations().stripTrailingZeros(),
          input.employmentStatus(),
          input.employmentTenureMonths());
    }
  }
}

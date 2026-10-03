package com.frauddetection.fraudassessment.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class LoanOriginationFraudPolicy {
  public static final String RULESET_ID = "loan-origination-fraud";
  public static final String RULESET_VERSION = "1.0.0";
  private static final Duration VELOCITY_WINDOW = Duration.ofHours(24);

  public FraudAssessment evaluate(
      AssessmentInput input, Instant now, List<AssessmentHistory> history) {
    List<AssessmentHistory> recent =
        history.stream()
            .filter(h -> !h.assessmentRequestId().equals(input.assessmentRequestId()))
            .filter(h -> h.customerReference().equals(input.customerReference()))
            .filter(
                h ->
                    !h.evaluatedAt().isBefore(now.minus(VELOCITY_WINDOW))
                        && !h.evaluatedAt().isAfter(now))
            .sorted(Comparator.comparing(AssessmentHistory::evaluatedAt).reversed())
            .toList();
    List<FraudSignal> signals = new ArrayList<>();
    List<ScoreContribution> contributions = new ArrayList<>();
    int score = 0;
    long attempts = recent.size() + 1;
    if (attempts >= 3) {
      signals.add(
          new FraudSignal(
              "HIGH_APPLICATION_VELOCITY",
              attempts + " applications from one customer reference in 24 hours"));
      contributions.add(
          new ScoreContribution(
              "HIGH_APPLICATION_VELOCITY",
              35,
              "Three or more applications in the demonstration 24-hour window"));
      score += 35;
    }
    AssessmentHistory latest = recent.stream().findFirst().orElse(null);
    boolean amountEscalation =
        latest != null
            && input
                    .requestedAmount()
                    .compareTo(latest.requestedAmount().multiply(new java.math.BigDecimal("1.50")))
                > 0;
    if (amountEscalation) {
      signals.add(
          new FraudSignal(
              "APPLICATION_AMOUNT_ESCALATION",
              "Requested amount increased by more than 50% from the latest application"));
      contributions.add(
          new ScoreContribution(
              "APPLICATION_AMOUNT_ESCALATION", 20, "Material request amount escalation"));
      score += 20;
    }
    boolean priorBlock = recent.stream().anyMatch(h -> h.decision() == FraudDecision.BLOCK);
    boolean extremeVelocityEscalation = attempts >= 6 && amountEscalation;
    boolean hardBlock = priorBlock || extremeVelocityEscalation;
    if (priorBlock) {
      signals.add(
          new FraudSignal(
              "RAPID_RESUBMISSION_AFTER_BLOCK",
              "A previous assessment for this customer reference was blocked within 24 hours"));
      contributions.add(
          new ScoreContribution(
              "RAPID_RESUBMISSION_AFTER_BLOCK",
              60,
              "Repeated application shortly after a BLOCK decision"));
      score += 60;
    }
    if (extremeVelocityEscalation) {
      signals.add(
          new FraudSignal(
              "EXTREME_VELOCITY_WITH_AMOUNT_ESCALATION",
              "Six or more applications in 24 hours include a request more than 50% above the latest"));
      contributions.add(
          new ScoreContribution(
              "EXTREME_VELOCITY_WITH_AMOUNT_ESCALATION",
              60,
              "High request velocity combined with a material increase in requested principal"));
      score += 60;
    }
    if (latest != null && changedByMoreThanHalf(latest.monthlyIncome(), input.monthlyIncome())) {
      signals.add(
          new FraudSignal(
              "MATERIAL_INCOME_CHANGE",
              "Declared monthly income changed by more than 50% from the latest application"));
      contributions.add(
          new ScoreContribution(
              "MATERIAL_INCOME_CHANGE", 20, "Declared profile change is a review signal only"));
      score += 20;
    }
    if (latest != null
        && changedByMoreThanHalf(latest.existingDebt(), input.existingMonthlyDebtObligations())) {
      signals.add(
          new FraudSignal(
              "MATERIAL_DEBT_CHANGE",
              "Declared monthly debt changed by more than 50% from the latest application"));
      contributions.add(
          new ScoreContribution(
              "MATERIAL_DEBT_CHANGE", 15, "Declared profile change is a review signal only"));
      score += 15;
    }
    score = Math.max(0, Math.min(100, score));
    FraudRiskLevel level =
        score < 25 ? FraudRiskLevel.LOW : score < 60 ? FraudRiskLevel.MEDIUM : FraudRiskLevel.HIGH;
    FraudDecision decision =
        hardBlock ? FraudDecision.BLOCK : score < 25 ? FraudDecision.PASS : FraudDecision.REVIEW;
    List<String> reasons = signals.stream().map(FraudSignal::code).distinct().toList();
    if (reasons.isEmpty()) reasons = List.of("NO_MATERIAL_FRAUD_SIGNALS");
    return new FraudAssessment(
        UUID.randomUUID(),
        input,
        now,
        RULESET_ID,
        RULESET_VERSION,
        score,
        level,
        decision,
        signals,
        contributions,
        reasons);
  }

  private static boolean changedByMoreThanHalf(
      java.math.BigDecimal oldValue, java.math.BigDecimal newValue) {
    if (oldValue.signum() == 0) return newValue.signum() > 0;
    return newValue
            .subtract(oldValue)
            .abs()
            .compareTo(oldValue.abs().multiply(new java.math.BigDecimal("0.50")))
        > 0;
  }
}

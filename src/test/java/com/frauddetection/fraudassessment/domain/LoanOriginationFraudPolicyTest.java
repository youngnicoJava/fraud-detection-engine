package com.frauddetection.fraudassessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoanOriginationFraudPolicyTest {
  private final LoanOriginationFraudPolicy policy = new LoanOriginationFraudPolicy();
  private final Instant now = Instant.parse("2026-01-01T12:00:00Z");
  private final UUID customer = UUID.fromString("c0000000-0000-0000-0000-000000000001");

  @Test
  void firstAttemptPassesAndIsDeterministic() {
    var first = policy.evaluate(input(1000), now, List.of());
    var second = policy.evaluate(input(1000), now, List.of());
    assertThat(first.fraudScore()).isZero();
    assertThat(first.decision()).isEqualTo(FraudDecision.PASS);
    assertThat(first.reasonCodes()).containsExactly("NO_MATERIAL_FRAUD_SIGNALS");
    assertThat(first.rulesetVersion()).isEqualTo("1.0.0");
    assertThat(first.signals()).isEqualTo(second.signals());
    assertThat(first.contributions()).isEqualTo(second.contributions());
  }

  @Test
  void velocityThresholdAndInclusiveTimeBoundaryTriggerReview() {
    var history =
        List.of(history(2000, now.minusSeconds(86400)), history(3000, now.minusSeconds(100)));
    var result = policy.evaluate(input(4000), now, history);
    assertThat(result.signals())
        .extracting(FraudSignal::code)
        .contains("HIGH_APPLICATION_VELOCITY");
    assertThat(result.decision()).isEqualTo(FraudDecision.REVIEW);
    assertThat(
            policy
                .evaluate(input(5000), now, List.of(history(6000, now.minusSeconds(86401))))
                .decision())
        .isEqualTo(FraudDecision.PASS);
  }

  @Test
  void recentBlockedResubmissionTriggersBlock() {
    var history =
        List.of(
            new AssessmentHistory(
                UUID.randomUUID(),
                customer,
                new BigDecimal("1000"),
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                now.minusSeconds(60),
                FraudDecision.BLOCK));
    var result = policy.evaluate(input(1000), now, history);
    assertThat(result.decision()).isEqualTo(FraudDecision.BLOCK);
    assertThat(result.reasonCodes()).contains("RAPID_RESUBMISSION_AFTER_BLOCK");
  }

  @Test
  void extremeVelocityCombinedWithAmountEscalationCanProduceInitialBlock() {
    var history =
        java.util.stream.IntStream.range(0, 5)
            .mapToObj(i -> history(1000, now.minusSeconds(60L * (i + 1))))
            .toList();
    var result = policy.evaluate(input(2000), now, history);
    assertThat(result.decision()).isEqualTo(FraudDecision.BLOCK);
    assertThat(result.reasonCodes())
        .contains(
            "HIGH_APPLICATION_VELOCITY",
            "APPLICATION_AMOUNT_ESCALATION",
            "EXTREME_VELOCITY_WITH_AMOUNT_ESCALATION");
    assertThat(result.fraudScore()).isEqualTo(100);
  }

  @Test
  void amountAndProfileChangesAreExplainedAndScoreClampsAt100() {
    var history =
        List.of(
            new AssessmentHistory(
                UUID.randomUUID(),
                customer,
                new BigDecimal("1000"),
                new BigDecimal("1000"),
                new BigDecimal("100"),
                now.minusSeconds(30),
                FraudDecision.BLOCK),
            history(400, now.minusSeconds(60)),
            history(500, now.minusSeconds(90)));
    var result = policy.evaluate(input(2000), now, history);
    assertThat(result.reasonCodes())
        .contains(
            "HIGH_APPLICATION_VELOCITY",
            "APPLICATION_AMOUNT_ESCALATION",
            "MATERIAL_INCOME_CHANGE",
            "MATERIAL_DEBT_CHANGE",
            "RAPID_RESUBMISSION_AFTER_BLOCK");
    assertThat(result.fraudScore()).isEqualTo(100);
    assertThat(result.contributions()).hasSize(5);
  }

  private AssessmentInput input(int amount) {
    return new AssessmentInput(
        UUID.randomUUID(),
        UUID.randomUUID(),
        customer,
        new BigDecimal(amount),
        "ARS",
        24,
        "PERSONAL",
        new BigDecimal("2000"),
        new BigDecimal("300"),
        "PERMANENT",
        24,
        "correlation-test");
  }

  private AssessmentHistory history(int amount, Instant at) {
    return new AssessmentHistory(
        UUID.randomUUID(),
        customer,
        new BigDecimal(amount),
        new BigDecimal("2000"),
        new BigDecimal("300"),
        at,
        FraudDecision.PASS);
  }
}

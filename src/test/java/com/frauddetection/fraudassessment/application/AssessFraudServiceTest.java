package com.frauddetection.fraudassessment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.fraudassessment.domain.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;

class AssessFraudServiceTest {
  @Test
  void sameMaterialRequestReplaysAndDifferentRequestConflictsEvenAcrossCorrelationIds() {
    var store = new InMemoryRepository();
    var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    var metrics = new SimpleMeterRegistry();
    var caseService =
        new FraudCaseService(
            new InMemoryCases(),
            store,
            (fraudCase, actor, occurredAt, correlationId, eventId) -> {},
            clock,
            metrics);
    var service =
        new AssessFraudService(
            store,
            new LoanOriginationFraudPolicy(),
            clock,
            new ObjectMapper(),
            metrics,
            caseService);
    var input = input("corr-a");
    var first = service.assess(input);
    var replay = service.assess(input("corr-b"));
    assertThat(replay.id()).isEqualTo(first.id());
    assertThat(store.saves).isEqualTo(1);
    assertThatThrownBy(
            () ->
                service.assess(
                    new AssessmentInput(
                        input.assessmentRequestId(),
                        input.loanApplicationId(),
                        input.customerReference(),
                        new BigDecimal("120"),
                        input.currency(),
                        input.termMonths(),
                        input.productType(),
                        input.monthlyIncome(),
                        input.existingMonthlyDebtObligations(),
                        input.employmentStatus(),
                        input.employmentTenureMonths(),
                        "corr-c")))
        .isInstanceOf(AssessmentRequestConflictException.class);
  }

  private AssessmentInput input(String correlation) {
    return new AssessmentInput(
        UUID.fromString("10000000-0000-0000-0000-000000000001"),
        UUID.fromString("20000000-0000-0000-0000-000000000001"),
        UUID.fromString("30000000-0000-0000-0000-000000000001"),
        new BigDecimal("100"),
        "ARS",
        12,
        "PERSONAL",
        new BigDecimal("1000"),
        new BigDecimal("100"),
        "PERMANENT",
        12,
        correlation);
  }

  static class InMemoryRepository implements FraudAssessmentRepository {
    FraudAssessment saved;
    int saves;

    public Optional<FraudAssessment> findByRequestId(UUID id) {
      return saved == null ? Optional.empty() : Optional.of(saved);
    }

    public Optional<FraudAssessment> findById(UUID id) {
      return saved != null && saved.id().equals(id) ? Optional.of(saved) : Optional.empty();
    }

    public AssessmentPage<FraudAssessmentSummary> list(AssessmentQuery query) {
      return new AssessmentPage<>(List.of(), query.page(), query.size(), 0, 0);
    }

    public List<AssessmentHistory> recentByCustomer(UUID id, int limit) {
      return List.of();
    }

    public void saveWithResultOutbox(FraudAssessment a, String hash) {
      saved = a;
      saves++;
    }
  }

  static class InMemoryCases implements FraudCaseRepository {
    public Optional<FraudCase> findById(UUID id) {
      return Optional.empty();
    }

    public Optional<FraudCase> findByIdForUpdate(UUID id) {
      return Optional.empty();
    }

    public Optional<FraudCase> findByAssessmentId(UUID id) {
      return Optional.empty();
    }

    public AssessmentPage<FraudCaseSummary> list(FraudCaseQuery query) {
      return new AssessmentPage<>(List.of(), query.page(), query.size(), 0, 0);
    }

    public FraudCase create(FraudCase fraudCase, FraudCaseAction action) {
      return fraudCase;
    }

    public void saveTransition(FraudCase fraudCase, FraudCaseAction action) {}
  }
}

package com.frauddetection.fraudassessment.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.fraudassessment.application.FraudAssessmentRepository;
import com.frauddetection.fraudassessment.domain.AssessmentHistory;
import com.frauddetection.fraudassessment.domain.FraudAssessment;
import com.frauddetection.outbox.persistence.FraudOutboxEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FraudAssessmentPersistenceAdapter implements FraudAssessmentRepository {
  private final FraudAssessmentPanacheRepository assessments;
  private final ObjectMapper mapper;

  @Inject
  public FraudAssessmentPersistenceAdapter(FraudAssessmentPanacheRepository a, ObjectMapper m) {
    assessments = a;
    mapper = m;
  }

  public Optional<FraudAssessment> findByRequestId(UUID id) {
    return assessments.find("requestId", id).firstResultOptional().map(e -> e.toDomain(mapper));
  }

  public Optional<FraudAssessment> findById(UUID id) {
    return assessments.findByIdOptional(id).map(e -> e.toDomain(mapper));
  }

  public List<AssessmentHistory> recentByCustomer(UUID id, int limit) {
    return assessments.recent(id, limit).stream()
        .map(
            e ->
                new AssessmentHistory(
                    e.requestId,
                    e.customerReference,
                    e.requestedAmount,
                    extract(e, "monthlyIncome"),
                    extract(e, "existingMonthlyDebtObligations"),
                    e.evaluatedAt,
                    com.frauddetection.fraudassessment.domain.FraudDecision.valueOf(e.decision)))
        .toList();
  }

  private java.math.BigDecimal extract(FraudAssessmentEntity e, String field) {
    try {
      return mapper.readTree(e.assessmentPayload).path("input").path(field).decimalValue();
    } catch (Exception ex) {
      throw new IllegalStateException("Could not read persisted fraud history", ex);
    }
  }

  @Transactional
  public void saveWithResultOutbox(FraudAssessment a, String hash) {
    var assessment = FraudAssessmentEntity.from(a, hash, mapper);
    assessments.persist(assessment);
    try {
      var p =
          mapper
              .createObjectNode()
              .put("assessmentRequestId", a.input().assessmentRequestId().toString())
              .put("loanApplicationId", a.input().loanApplicationId().toString())
              .put("fraudAssessmentId", a.id().toString())
              .put("decision", a.decision().name())
              .put("fraudScore", a.fraudScore())
              .put("riskLevel", a.riskLevel().name())
              .put("rulesetId", a.rulesetId())
              .put("rulesetVersion", a.rulesetVersion())
              .put("evaluatedAt", a.evaluatedAt().toString());
      p.set("reasonCodes", mapper.valueToTree(a.reasonCodes()));
      var outbox = new FraudOutboxEntity();
      outbox.id = UUID.randomUUID();
      outbox.eventId =
          UUID.nameUUIDFromBytes(
              ("fraud-assessment:" + a.input().assessmentRequestId())
                  .getBytes(StandardCharsets.UTF_8));
      outbox.eventType = "fraud.assessment.completed";
      outbox.eventVersion = 1;
      outbox.aggregateId = a.id();
      outbox.correlationId = a.input().correlationId();
      outbox.payload = mapper.writeValueAsString(p);
      outbox.occurredAt = a.evaluatedAt();
      outbox.status = "PENDING";
      outbox.attemptCount = 0;
      FraudOutboxEntity.persist(outbox);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not create fraud result outbox event", ex);
    }
  }
}

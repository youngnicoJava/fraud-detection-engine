package com.frauddetection.fraudassessment.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.fraudassessment.domain.FraudAssessment;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "fraud_assessments")
public class FraudAssessmentEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "assessment_request_id", nullable = false, unique = true)
  public UUID requestId;

  @Column(name = "loan_application_id", nullable = false)
  public UUID loanApplicationId;

  @Column(name = "customer_reference", nullable = false)
  public UUID customerReference;

  @Column(name = "requested_amount", nullable = false)
  public java.math.BigDecimal requestedAmount;

  @Column(nullable = false, length = 3)
  public String currency;

  @Column(name = "evaluated_at", nullable = false)
  public java.time.Instant evaluatedAt;

  @Column(name = "ruleset_id", nullable = false, length = 80)
  public String rulesetId;

  @Column(name = "ruleset_version", nullable = false, length = 40)
  public String rulesetVersion;

  @Column(name = "fraud_score", nullable = false)
  public int score;

  @Column(name = "risk_level", nullable = false, length = 10)
  public String riskLevel;

  @Column(nullable = false, length = 10)
  public String decision;

  @Column(name = "correlation_id", nullable = false, length = 128)
  public String correlationId;

  @Column(name = "request_hash", nullable = false, length = 64)
  public String requestHash;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "assessment_payload", nullable = false, columnDefinition = "jsonb")
  public String assessmentPayload;

  public static FraudAssessmentEntity from(FraudAssessment a, String hash, ObjectMapper mapper) {
    try {
      var e = new FraudAssessmentEntity();
      e.id = a.id();
      e.requestId = a.input().assessmentRequestId();
      e.loanApplicationId = a.input().loanApplicationId();
      e.customerReference = a.input().customerReference();
      e.requestedAmount = a.input().requestedAmount();
      e.currency = a.input().currency();
      e.evaluatedAt = a.evaluatedAt();
      e.rulesetId = a.rulesetId();
      e.rulesetVersion = a.rulesetVersion();
      e.score = a.fraudScore();
      e.riskLevel = a.riskLevel().name();
      e.decision = a.decision().name();
      e.correlationId = a.input().correlationId();
      e.requestHash = hash;
      e.assessmentPayload = mapper.writeValueAsString(a);
      return e;
    } catch (Exception ex) {
      throw new IllegalStateException("Could not encode fraud assessment", ex);
    }
  }

  public FraudAssessment toDomain(ObjectMapper mapper) {
    try {
      return mapper.readValue(assessmentPayload, FraudAssessment.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not decode fraud assessment", ex);
    }
  }
}

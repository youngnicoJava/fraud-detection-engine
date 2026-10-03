package com.frauddetection.fraudassessment.persistence;

import com.frauddetection.fraudassessment.domain.FraudCase;
import com.frauddetection.fraudassessment.domain.FraudCaseAction;
import com.frauddetection.fraudassessment.domain.FraudCaseResolution;
import com.frauddetection.fraudassessment.domain.FraudCaseStatus;
import com.frauddetection.fraudassessment.domain.FraudDecision;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_cases")
public class FraudCaseEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "fraud_assessment_id", nullable = false, unique = true)
  public UUID assessmentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fraud_assessment_id", insertable = false, updatable = false)
  public FraudAssessmentEntity assessment;

  @Column(name = "loan_application_id", nullable = false)
  public UUID loanApplicationId;

  @Column(name = "automated_decision", nullable = false, length = 10)
  public String automatedDecision;

  @Column(nullable = false, length = 20)
  public String status;

  @Column(length = 24)
  public String resolution;

  @Column(name = "resolution_note", length = 1000)
  public String resolutionNote;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  @Column(name = "resolved_at")
  public Instant resolvedAt;

  @Column(name = "correlation_id", nullable = false, length = 128)
  public String correlationId;

  public FraudCase toDomain(List<FraudCaseAction> history) {
    return new FraudCase(
        id,
        assessmentId,
        loanApplicationId,
        FraudDecision.valueOf(automatedDecision),
        FraudCaseStatus.valueOf(status),
        resolution == null ? null : FraudCaseResolution.valueOf(resolution),
        resolutionNote,
        createdAt,
        updatedAt,
        resolvedAt,
        correlationId,
        history);
  }

  public static FraudCaseEntity from(FraudCase fraudCase) {
    var entity = new FraudCaseEntity();
    entity.id = fraudCase.id();
    entity.assessmentId = fraudCase.assessmentId();
    entity.loanApplicationId = fraudCase.loanApplicationId();
    entity.automatedDecision = fraudCase.automatedDecision().name();
    entity.status = fraudCase.status().name();
    entity.resolution = fraudCase.resolution() == null ? null : fraudCase.resolution().name();
    entity.resolutionNote = fraudCase.resolutionNote();
    entity.createdAt = fraudCase.createdAt();
    entity.updatedAt = fraudCase.updatedAt();
    entity.resolvedAt = fraudCase.resolvedAt();
    entity.correlationId = fraudCase.correlationId();
    return entity;
  }
}

package com.frauddetection.fraudassessment.persistence;

import com.frauddetection.fraudassessment.domain.FraudCaseAction;
import com.frauddetection.fraudassessment.domain.FraudCaseActionType;
import com.frauddetection.fraudassessment.domain.FraudCaseResolution;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_case_actions")
public class FraudCaseActionEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "fraud_case_id", nullable = false)
  public UUID fraudCaseId;

  @Column(nullable = false, length = 24)
  public String action;

  @Column(name = "actor_id", nullable = false, length = 200)
  public String actorId;

  @Column(name = "occurred_at", nullable = false)
  public Instant occurredAt;

  @Column(name = "correlation_id", nullable = false, length = 128)
  public String correlationId;

  @Column(length = 24)
  public String resolution;

  @Column(length = 1000)
  public String note;

  public FraudCaseAction toDomain() {
    return new FraudCaseAction(
        id,
        FraudCaseActionType.valueOf(action),
        actorId,
        occurredAt,
        correlationId,
        resolution == null ? null : FraudCaseResolution.valueOf(resolution),
        note);
  }

  public static FraudCaseActionEntity from(UUID caseId, FraudCaseAction action) {
    var entity = new FraudCaseActionEntity();
    entity.id = action.id();
    entity.fraudCaseId = caseId;
    entity.action = action.action().name();
    entity.actorId = action.actorId();
    entity.occurredAt = action.occurredAt();
    entity.correlationId = action.correlationId();
    entity.resolution = action.resolution() == null ? null : action.resolution().name();
    entity.note = action.note();
    return entity;
  }
}

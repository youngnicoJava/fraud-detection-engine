package com.frauddetection.outbox.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.fraudassessment.application.FraudCaseResolutionEventPort;
import com.frauddetection.fraudassessment.domain.FraudCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class FraudCaseResolutionOutboxAdapter implements FraudCaseResolutionEventPort {
  private final ObjectMapper mapper;

  @Inject
  public FraudCaseResolutionOutboxAdapter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void recordResolved(
      FraudCase fraudCase, String actorId, Instant occurredAt, String correlationId, UUID eventId) {
    try {
      var payload =
          mapper
              .createObjectNode()
              .put("fraudCaseId", fraudCase.id().toString())
              .put("fraudAssessmentId", fraudCase.assessmentId().toString())
              .put("loanApplicationId", fraudCase.loanApplicationId().toString())
              .put("resolution", fraudCase.resolution().name())
              .put("resolvedAt", fraudCase.resolvedAt().toString());
      var event = new FraudOutboxEntity();
      event.id = UUID.randomUUID();
      event.eventId = eventId;
      event.eventType = "fraud.case.resolved";
      event.eventVersion = 1;
      event.aggregateId = fraudCase.id();
      event.correlationId = correlationId;
      event.payload = mapper.writeValueAsString(payload);
      event.occurredAt = occurredAt;
      event.status = "PENDING";
      event.attemptCount = 0;
      FraudOutboxEntity.persist(event);
    } catch (Exception failure) {
      throw new IllegalStateException(
          "Could not create fraud case resolution outbox event", failure);
    }
  }
}

package com.frauddetection.fraudassessment.application;

import com.frauddetection.fraudassessment.domain.FraudCase;
import java.time.Instant;
import java.util.UUID;

public interface FraudCaseResolutionEventPort {
  void recordResolved(
      FraudCase fraudCase, String actorId, Instant occurredAt, String correlationId, UUID eventId);
}

package com.frauddetection.outbox.application;

import java.util.List;
import java.util.UUID;

public interface AssessmentResultPublisher {
  List<PendingFraudEvent> claimBatch();

  void markPublished(UUID id);

  void releaseForRetry(UUID id);

  boolean publish(String json);

  long pendingCount();
}

package com.frauddetection.outbox.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.outbox.application.AssessmentResultPublisher;
import com.frauddetection.outbox.application.PendingFraudEvent;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class KafkaOutboxAdapter implements AssessmentResultPublisher {
  private final FraudOutboxPanacheRepository repo;
  private final Emitter<Record<String, String>> emitter;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final Config config;

  @Inject
  public KafkaOutboxAdapter(
      FraudOutboxPanacheRepository r,
      @Channel("fraud-assessment-results") Emitter<Record<String, String>> e,
      ObjectMapper m,
      Clock c,
      Config cfg) {
    repo = r;
    emitter = e;
    mapper = m;
    clock = c;
    config = cfg;
  }

  @Override
  @Transactional
  public List<PendingFraudEvent> claimBatch() {
    if (!config.getOptionalValue("fraud.kafka.enabled", Boolean.class).orElse(false))
      return List.of();
    var now = clock.instant();
    var rows =
        repo.find(
                "status = 'PENDING' or (status = 'PROCESSING' and lockedAt < ?1)",
                now.minusSeconds(120))
            .withLock(LockModeType.PESSIMISTIC_WRITE)
            .page(0, 50)
            .list();
    for (var row : rows) {
      row.status = "PROCESSING";
      row.lockedAt = now;
      row.attemptCount++;
    }
    return rows.stream()
        .map(
            row ->
                new PendingFraudEvent(
                    row.id,
                    row.eventId,
                    row.eventType,
                    row.eventVersion,
                    row.aggregateId,
                    row.correlationId,
                    row.payload,
                    row.occurredAt))
        .toList();
  }

  @Override
  public boolean publish(String json) {
    if (!config.getOptionalValue("fraud.kafka.enabled", Boolean.class).orElse(false)) return false;
    try {
      var root = mapper.readTree(json);
      emitter.send(Record.of(root.path("aggregateId").asText(), json)).toCompletableFuture().join();
      return true;
    } catch (Exception ex) {
      throw new IllegalStateException("Could not publish fraud result event", ex);
    }
  }

  @Override
  public long pendingCount() {
    return repo.pendingCount();
  }

  @Override
  @Transactional
  public void markPublished(UUID id) {
    var e = repo.findById(id);
    e.status = "PUBLISHED";
    e.publishedAt = clock.instant();
    e.lockedAt = null;
  }

  @Override
  @Transactional
  public void releaseForRetry(UUID id) {
    var e = repo.findById(id);
    e.status = "PENDING";
    e.lockedAt = null;
  }
}

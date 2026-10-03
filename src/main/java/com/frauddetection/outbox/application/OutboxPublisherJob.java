package com.frauddetection.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OutboxPublisherJob {
  private static final Logger LOG = Logger.getLogger(OutboxPublisherJob.class);
  private final AssessmentResultPublisher publisher;
  private final ObjectMapper mapper;
  private final MeterRegistry metrics;

  @Inject
  public OutboxPublisherJob(AssessmentResultPublisher p, ObjectMapper m, MeterRegistry metrics) {
    publisher = p;
    mapper = m;
    this.metrics = metrics;
  }

  @jakarta.annotation.PostConstruct
  void registerPendingGauge() {
    metrics.gauge("fraud_outbox_pending", publisher, AssessmentResultPublisher::pendingCount);
  }

  @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
  void publishPending() {
    for (var row : publisher.claimBatch()) {
      try {
        var event =
            mapper
                .createObjectNode()
                .put("eventId", row.eventId().toString())
                .put("eventType", row.eventType() + ".v" + row.eventVersion())
                .put("eventVersion", row.eventVersion())
                .put("occurredAt", row.occurredAt().toString())
                .put(
                    "aggregateType",
                    row.eventType().startsWith("fraud.case.") ? "FraudCase" : "FraudAssessment")
                .put("aggregateId", row.aggregateId().toString())
                .put("correlationId", row.correlationId());
        event.set("payload", mapper.readTree(row.payload()));
        if (publisher.publish(mapper.writeValueAsString(event))) {
          publisher.markPublished(row.id());
          metrics.counter("fraud_outbox_published_total").increment();
        } else publisher.releaseForRetry(row.id());
      } catch (Exception ex) {
        publisher.releaseForRetry(row.id());
        metrics.counter("fraud_outbox_failures_total").increment();
        LOG.warnf(
            "Fraud outbox publication failed eventId=%s aggregateId=%s",
            row.eventId(), row.aggregateId());
      }
    }
  }
}

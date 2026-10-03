package com.frauddetection.outbox.application;

import java.time.Instant;
import java.util.UUID;

public record PendingFraudEvent(
    UUID id,
    UUID eventId,
    String eventType,
    int eventVersion,
    UUID aggregateId,
    String correlationId,
    String payload,
    Instant occurredAt) {}

package com.frauddetection.fraudassessment.domain;

import java.time.Instant;
import java.util.UUID;

public record FraudCaseAction(
    UUID id,
    FraudCaseActionType action,
    String actorId,
    Instant occurredAt,
    String correlationId,
    FraudCaseResolution resolution,
    String note) {}

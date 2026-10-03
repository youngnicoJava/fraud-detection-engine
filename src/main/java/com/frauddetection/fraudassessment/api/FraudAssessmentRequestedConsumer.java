package com.frauddetection.fraudassessment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.fraudassessment.application.AssessFraudUseCase;
import com.frauddetection.fraudassessment.domain.AssessmentInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class FraudAssessmentRequestedConsumer {
  private final ObjectMapper mapper;
  private final AssessFraudUseCase assessments;

  @Inject
  public FraudAssessmentRequestedConsumer(ObjectMapper m, AssessFraudUseCase a) {
    mapper = m;
    assessments = a;
  }

  @Incoming("fraud-assessment-requests")
  @Retry(maxRetries = 3, delay = 1, delayUnit = java.time.temporal.ChronoUnit.SECONDS)
  public void consume(String json) {
    try {
      JsonNode root = mapper.readTree(json);
      if (!"loan.fraud-assessment.requested.v1".equals(root.path("eventType").asText())
          || root.path("eventVersion").asInt() != 1)
        throw new IllegalArgumentException("Unsupported loan fraud assessment request contract");
      JsonNode p = root.path("payload");
      String correlation = root.path("correlationId").asText();
      if (!correlation.matches("[A-Za-z0-9._:-]{1,128}"))
        throw new IllegalArgumentException("Invalid correlation ID");
      assessments.assess(
          new AssessmentInput(
              UUID.fromString(p.path("assessmentRequestId").asText()),
              UUID.fromString(p.path("loanApplicationId").asText()),
              UUID.fromString(p.path("customerReference").asText()),
              new BigDecimal(p.path("requestedAmount").asText()),
              p.path("currency").asText(),
              p.path("termMonths").asInt(),
              p.path("productType").asText(),
              new BigDecimal(p.path("monthlyIncome").asText()),
              new BigDecimal(p.path("existingMonthlyDebtObligations").asText()),
              p.path("employmentStatus").asText(),
              p.path("employmentTenureMonths").asInt(),
              correlation));
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Malformed or unsupported loan fraud assessment request v1", ex);
    }
  }
}

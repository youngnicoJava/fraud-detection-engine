package com.frauddetection.fraudassessment.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record AssessmentInput(
    UUID assessmentRequestId,
    UUID loanApplicationId,
    UUID customerReference,
    BigDecimal requestedAmount,
    String currency,
    int termMonths,
    String productType,
    BigDecimal monthlyIncome,
    BigDecimal existingMonthlyDebtObligations,
    String employmentStatus,
    int employmentTenureMonths,
    String correlationId) {
  public AssessmentInput {
    Objects.requireNonNull(assessmentRequestId);
    Objects.requireNonNull(loanApplicationId);
    Objects.requireNonNull(customerReference);
    Objects.requireNonNull(requestedAmount);
    Objects.requireNonNull(currency);
    Objects.requireNonNull(productType);
    Objects.requireNonNull(monthlyIncome);
    Objects.requireNonNull(existingMonthlyDebtObligations);
    Objects.requireNonNull(employmentStatus);
    Objects.requireNonNull(correlationId);
    if (requestedAmount.signum() <= 0
        || monthlyIncome.signum() <= 0
        || existingMonthlyDebtObligations.signum() < 0
        || termMonths < 1
        || termMonths > 600
        || employmentTenureMonths < 0)
      throw new IllegalArgumentException("Invalid fraud assessment inputs");
    if (!currency.matches("[A-Z]{3}") || productType.isBlank() || correlationId.isBlank())
      throw new IllegalArgumentException("Invalid currency, product type or correlation ID");
  }
}

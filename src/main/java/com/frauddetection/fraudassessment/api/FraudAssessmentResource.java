package com.frauddetection.fraudassessment.api;

import com.frauddetection.fraudassessment.application.AssessFraudUseCase;
import com.frauddetection.fraudassessment.application.FraudAssessmentRepository;
import com.frauddetection.fraudassessment.domain.AssessmentInput;
import com.frauddetection.fraudassessment.domain.FraudAssessment;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/fraud-assessments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Fraud Assessments")
public class FraudAssessmentResource {
  private final AssessFraudUseCase assess;
  private final FraudAssessmentRepository assessments;

  @Inject
  public FraudAssessmentResource(AssessFraudUseCase a, FraudAssessmentRepository r) {
    assess = a;
    assessments = r;
  }

  @POST
  @RolesAllowed({"FRAUD_ANALYST", "ADMIN"})
  @Operation(
      summary = "Evaluate loan origination fraud signals",
      description =
          "Applies deterministic rules to declared application data and recent history. The score is not a calibrated probability.")
  public FraudAssessmentResponse create(@Valid FraudAssessmentRequest r) {
    return FraudAssessmentResponse.from(
        assess.assess(
            new AssessmentInput(
                r.assessmentRequestId(),
                r.loanApplicationId(),
                r.customerReference(),
                r.requestedAmount(),
                r.currency(),
                r.termMonths(),
                r.productType(),
                r.monthlyIncome(),
                r.existingMonthlyDebtObligations(),
                r.employmentStatus(),
                r.employmentTenureMonths(),
                CorrelationContext.currentOr(r.correlationId()))));
  }

  @GET
  @Path("/{id}")
  @RolesAllowed({"FRAUD_ANALYST", "ADMIN"})
  @Operation(summary = "Retrieve a fraud assessment and its explanation")
  public FraudAssessmentResponse get(@PathParam("id") UUID id) {
    return assessments
        .findById(id)
        .map(FraudAssessmentResponse::from)
        .orElseThrow(NotFoundException::new);
  }

  public record FraudAssessmentRequest(
      @NotNull UUID assessmentRequestId,
      @NotNull UUID loanApplicationId,
      @NotNull UUID customerReference,
      @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal requestedAmount,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @Min(1) @Max(600) int termMonths,
      @NotBlank @Size(max = 50) String productType,
      @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal monthlyIncome,
      @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
          BigDecimal existingMonthlyDebtObligations,
      @NotBlank @Size(max = 40) String employmentStatus,
      @Min(0) @Max(600) int employmentTenureMonths,
      @Size(max = 128) String correlationId) {}

  public record FraudAssessmentResponse(
      UUID fraudAssessmentId,
      UUID assessmentRequestId,
      UUID loanApplicationId,
      String decision,
      int fraudScore,
      String riskLevel,
      String rulesetId,
      String rulesetVersion,
      java.time.Instant evaluatedAt,
      String correlationId,
      java.util.List<com.frauddetection.fraudassessment.domain.FraudSignal> signals,
      java.util.List<com.frauddetection.fraudassessment.domain.ScoreContribution> contributions,
      java.util.List<String> reasonCodes) {
    static FraudAssessmentResponse from(FraudAssessment a) {
      return new FraudAssessmentResponse(
          a.id(),
          a.input().assessmentRequestId(),
          a.input().loanApplicationId(),
          a.decision().name(),
          a.fraudScore(),
          a.riskLevel().name(),
          a.rulesetId(),
          a.rulesetVersion(),
          a.evaluatedAt(),
          a.input().correlationId(),
          a.signals(),
          a.contributions(),
          a.reasonCodes());
    }
  }
}

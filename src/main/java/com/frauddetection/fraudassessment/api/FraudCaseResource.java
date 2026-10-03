package com.frauddetection.fraudassessment.api;

import com.frauddetection.fraudassessment.application.AssessmentPage;
import com.frauddetection.fraudassessment.application.FraudAssessmentRepository;
import com.frauddetection.fraudassessment.application.FraudCaseQuery;
import com.frauddetection.fraudassessment.application.FraudCaseService;
import com.frauddetection.fraudassessment.domain.FraudCase;
import com.frauddetection.fraudassessment.domain.FraudCaseResolution;
import com.frauddetection.fraudassessment.domain.FraudCaseStatus;
import com.frauddetection.fraudassessment.domain.FraudCaseSummary;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/fraud-cases")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"FRAUD_ANALYST", "ADMIN"})
@Tag(name = "Fraud Cases")
public class FraudCaseResource {
  private final FraudCaseService cases;
  private final FraudAssessmentRepository assessments;
  private final SecurityIdentity identity;

  @Inject
  public FraudCaseResource(
      FraudCaseService cases, FraudAssessmentRepository assessments, SecurityIdentity identity) {
    this.cases = cases;
    this.assessments = assessments;
    this.identity = identity;
  }

  @GET
  @Operation(
      summary = "Search the investigation queue",
      description = "Cases are newest first; page is zero-based and size is limited to 100.")
  public CaseListResponse list(
      @DefaultValue("0") @QueryParam("page") int page,
      @DefaultValue("20") @QueryParam("size") int size,
      @QueryParam("status") FraudCaseStatus status,
      @QueryParam("resolution") FraudCaseResolution resolution) {
    return CaseListResponse.from(cases.list(new FraudCaseQuery(page, size, status, resolution)));
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Inspect a fraud case and its original assessment")
  public FraudCaseDetailResponse get(@PathParam("id") UUID id) {
    var details = cases.get(id);
    return new FraudCaseDetailResponse(
        details.fraudCase(),
        FraudAssessmentResource.FraudAssessmentResponse.from(details.assessment()));
  }

  @POST
  @Path("/{id}/start-review")
  @Operation(summary = "Start investigating an open case")
  public FraudCase startReview(@PathParam("id") UUID id) {
    return cases.startReview(
        id, identity.getPrincipal().getName(), CorrelationContext.currentOr(null));
  }

  @POST
  @Path("/{id}/resolve")
  @Operation(summary = "Resolve a case under review")
  public FraudCase resolve(@PathParam("id") UUID id, @Valid ResolveCaseRequest request) {
    return cases.resolve(
        id,
        request.resolution(),
        request.note(),
        identity.getPrincipal().getName(),
        CorrelationContext.currentOr(null));
  }

  public record ResolveCaseRequest(
      @NotNull FraudCaseResolution resolution, @Size(max = 1000) String note) {}

  public record CaseListResponse(
      List<FraudCaseSummary> items, int page, int size, long totalElements, int totalPages) {
    static CaseListResponse from(AssessmentPage<FraudCaseSummary> page) {
      return new CaseListResponse(
          page.items(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
  }

  public record FraudCaseDetailResponse(
      FraudCase fraudCase, FraudAssessmentResource.FraudAssessmentResponse assessment) {}
}

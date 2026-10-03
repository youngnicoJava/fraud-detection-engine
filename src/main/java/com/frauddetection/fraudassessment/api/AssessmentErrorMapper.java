package com.frauddetection.fraudassessment.api;

import com.frauddetection.fraudassessment.application.AssessmentRequestConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;

@Provider
public class AssessmentErrorMapper implements ExceptionMapper<AssessmentRequestConflictException> {
  @Override
  public Response toResponse(AssessmentRequestConflictException e) {
    return Response.status(409)
        .entity(
            new ErrorBody(
                "ASSESSMENT_REQUEST_ID_REUSED_WITH_DIFFERENT_REQUEST",
                e.getMessage(),
                Instant.now(),
                CorrelationContext.currentOr(null)))
        .build();
  }

  public record ErrorBody(String code, String message, Instant timestamp, String correlationId) {}
}

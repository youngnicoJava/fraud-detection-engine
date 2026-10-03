package com.frauddetection.shared;

import com.frauddetection.fraudassessment.api.CorrelationContext;
import com.frauddetection.fraudassessment.application.FraudCaseConflictException;
import com.frauddetection.fraudassessment.application.FraudCaseNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;

@Provider
public class ApiErrorMapper implements ExceptionMapper<Throwable> {
  @Override
  public Response toResponse(Throwable failure) {
    int status = 500;
    String code = "INTERNAL_ERROR";
    String message = "An unexpected error occurred";
    if (failure instanceof WebApplicationException http) {
      status = http.getResponse().getStatus();
      code =
          status == 404
              ? "NOT_FOUND"
              : status == 401 ? "UNAUTHORIZED" : status == 403 ? "FORBIDDEN" : "HTTP_ERROR";
      message = status >= 500 ? "An unexpected error occurred" : http.getMessage();
    } else if (failure instanceof FraudCaseConflictException) {
      status = 409;
      code = "INVALID_CASE_STATE";
      message = failure.getMessage();
    } else if (failure instanceof FraudCaseNotFoundException) {
      status = 404;
      code = "FRAUD_CASE_NOT_FOUND";
      message = "The requested fraud case was not found";
    } else if (failure instanceof ConstraintViolationException
        || failure instanceof IllegalArgumentException
        || failure instanceof BadRequestException) {
      status = 400;
      code = "INVALID_REQUEST";
      message = "The request is invalid";
    } else if (failure instanceof NotFoundException) {
      status = 404;
      code = "NOT_FOUND";
      message = "The requested resource was not found";
    }
    return Response.status(status)
        .entity(new ApiError(code, message, Instant.now(), CorrelationContext.currentOr(null)))
        .build();
  }

  public record ApiError(String code, String message, Instant timestamp, String correlationId) {}
}

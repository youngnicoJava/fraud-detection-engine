package com.frauddetection.fraudassessment.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;
import org.jboss.logging.MDC;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext request) throws IOException {
    String supplied = request.getHeaderString("X-Correlation-ID");
    String value =
        supplied != null && supplied.matches("[A-Za-z0-9._:-]{1,128}")
            ? supplied
            : UUID.randomUUID().toString();
    CorrelationContext.set(value);
    MDC.put("correlationId", value);
    request.setProperty("correlationId", value);
  }

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response)
      throws IOException {
    response.getHeaders().putSingle("X-Correlation-ID", request.getProperty("correlationId"));
    CorrelationContext.clear();
    MDC.remove("correlationId");
  }
}

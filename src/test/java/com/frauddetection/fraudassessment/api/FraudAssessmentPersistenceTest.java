package com.frauddetection.fraudassessment.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FraudAssessmentPersistenceTest {
  @Inject EntityManager entityManager;

  @Test
  @TestSecurity(user = "fraud-analyst", roles = "FRAUD_ANALYST")
  void historyAndIdempotencySurviveRequestsAndOutboxIsWrittenOncePerAssessment() {
    long before =
        ((Number)
                entityManager
                    .createNativeQuery("select count(*) from fraud_outbox_events")
                    .getSingleResult())
            .longValue();
    UUID customer = UUID.randomUUID();
    String firstRequest = request(UUID.randomUUID(), UUID.randomUUID(), customer, "2000");
    String firstId = post(firstRequest, "corr-first").jsonPath().getString("fraudAssessmentId");
    post(request(UUID.randomUUID(), UUID.randomUUID(), customer, "2000"), "corr-second");
    var third = post(request(UUID.randomUUID(), UUID.randomUUID(), customer, "2000"), "corr-third");
    assertThat(third.jsonPath().getString("decision")).isEqualTo("REVIEW");
    assertThat(third.jsonPath().getList("reasonCodes", String.class))
        .contains("HIGH_APPLICATION_VELOCITY");

    var replay = post(firstRequest, "corr-retry");
    assertThat(replay.jsonPath().getString("fraudAssessmentId")).isEqualTo(firstId);
    assertThat(replay.jsonPath().getString("correlationId")).isEqualTo("corr-first");
    long after =
        ((Number)
                entityManager
                    .createNativeQuery("select count(*) from fraud_outbox_events")
                    .getSingleResult())
            .longValue();
    assertThat(after - before).isEqualTo(3);
  }

  private io.restassured.response.ExtractableResponse<io.restassured.response.Response> post(
      String body, String correlationId) {
    return given()
        .contentType(ContentType.JSON)
        .header("X-Correlation-ID", correlationId)
        .body(body)
        .when()
        .post("/api/v1/fraud-assessments")
        .then()
        .statusCode(200)
        .extract();
  }

  private String request(UUID requestId, UUID applicationId, UUID customerId, String amount) {
    return """
        {
          "assessmentRequestId":"%s",
          "loanApplicationId":"%s",
          "customerReference":"%s",
          "requestedAmount":%s,
          "currency":"ARS",
          "termMonths":24,
          "productType":"PERSONAL",
          "monthlyIncome":3000,
          "existingMonthlyDebtObligations":500,
          "employmentStatus":"PERMANENT",
          "employmentTenureMonths":24
        }
        """
        .formatted(requestId, applicationId, customerId, amount);
  }
}

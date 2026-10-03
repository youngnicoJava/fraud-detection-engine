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

    var filtered =
        given()
            .queryParam("page", 0)
            .queryParam("size", 1)
            .queryParam("decision", "REVIEW")
            .queryParam("loanApplicationId", third.jsonPath().getString("loanApplicationId"))
            .when()
            .get("/api/v1/fraud-assessments")
            .then()
            .statusCode(200)
            .extract();
    assertThat(filtered.jsonPath().getLong("totalElements")).isEqualTo(1);
    assertThat(filtered.jsonPath().getString("items[0].fraudAssessmentId"))
        .isEqualTo(third.jsonPath().getString("fraudAssessmentId"));

    var caseId =
        (UUID)
            entityManager
                .createNativeQuery(
                    "select id from fraud_cases where loan_application_id = :applicationId")
                .setParameter(
                    "applicationId",
                    UUID.fromString(third.jsonPath().getString("loanApplicationId")))
                .getSingleResult();
    var opened =
        given().when().get("/api/v1/fraud-cases/" + caseId).then().statusCode(200).extract();
    assertThat(opened.jsonPath().getString("fraudCase.status")).isEqualTo("OPEN");
    assertThat(opened.jsonPath().getString("assessment.decision")).isEqualTo("REVIEW");

    given()
        .contentType(ContentType.JSON)
        .body("{\"resolution\":\"CLEARED\"}")
        .when()
        .post("/api/v1/fraud-cases/" + caseId + "/resolve")
        .then()
        .statusCode(409);
    given()
        .contentType(ContentType.JSON)
        .when()
        .post("/api/v1/fraud-cases/" + caseId + "/start-review")
        .then()
        .statusCode(200)
        .body("status", org.hamcrest.Matchers.equalTo("UNDER_REVIEW"));
    var resolved =
        given()
            .contentType(ContentType.JSON)
            .body("{\"resolution\":\"CLEARED\",\"note\":\"Reviewed signals\"}")
            .when()
            .post("/api/v1/fraud-cases/" + caseId + "/resolve")
            .then()
            .statusCode(200)
            .extract();
    assertThat(resolved.jsonPath().getString("status")).isEqualTo("RESOLVED");
    assertThat(resolved.jsonPath().getString("resolution")).isEqualTo("CLEARED");
    assertThat(resolved.jsonPath().getList("history")).hasSize(3);
    assertThat(
            ((Number)
                    entityManager
                        .createNativeQuery(
                            "select count(*) from fraud_outbox_events where event_type = 'fraud.case.resolved' and aggregate_id = :caseId")
                        .setParameter("caseId", caseId)
                        .getSingleResult())
                .longValue())
        .isEqualTo(1);

    given()
        .when()
        .get("/api/v1/fraud-assessments/" + third.jsonPath().getString("fraudAssessmentId"))
        .then()
        .statusCode(200)
        .body("decision", org.hamcrest.Matchers.equalTo("REVIEW"));
    given()
        .contentType(ContentType.JSON)
        .body("{\"resolution\":\"CONFIRMED_FRAUD\"}")
        .when()
        .post("/api/v1/fraud-cases/" + caseId + "/resolve")
        .then()
        .statusCode(409);

    var replay = post(firstRequest, "corr-retry");
    assertThat(replay.jsonPath().getString("fraudAssessmentId")).isEqualTo(firstId);
    assertThat(replay.jsonPath().getString("correlationId")).isEqualTo("corr-first");
    long after =
        ((Number)
                entityManager
                    .createNativeQuery("select count(*) from fraud_outbox_events")
                    .getSingleResult())
            .longValue();
    assertThat(after - before).isEqualTo(4); // Tres evaluaciones y un evento de resolución de caso.
  }

  @Test
  @TestSecurity(user = "viewer", roles = "CUSTOMER")
  void analystEndpointsRejectNonAnalystRoles() {
    given().when().get("/api/v1/fraud-assessments").then().statusCode(403);
    given().when().get("/api/v1/fraud-cases").then().statusCode(403);
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

package com.frauddetection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FraudDetectionBootTest {
  @Test
  void readinessConfirmsFlywayAndHibernateStarted() {
    given().when().get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"));
  }
}

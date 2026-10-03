package com.frauddetection.shared;

import com.frauddetection.fraudassessment.domain.LoanOriginationFraudPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class FraudPolicyProducer {
  @Produces
  @ApplicationScoped
  LoanOriginationFraudPolicy fraudPolicy() {
    return new LoanOriginationFraudPolicy();
  }
}

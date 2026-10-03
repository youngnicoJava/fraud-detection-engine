package com.frauddetection.fraudassessment.application;

import java.util.UUID;

public class FraudCaseNotFoundException extends RuntimeException {
  public FraudCaseNotFoundException(UUID id) {
    super("Fraud case not found: " + id);
  }
}

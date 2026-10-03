package com.frauddetection.fraudassessment.application;

import com.frauddetection.fraudassessment.domain.FraudCaseResolution;
import com.frauddetection.fraudassessment.domain.FraudCaseStatus;

public record FraudCaseQuery(
    int page, int size, FraudCaseStatus status, FraudCaseResolution resolution) {
  public FraudCaseQuery {
    if (page < 0 || size < 1 || size > 100)
      throw new IllegalArgumentException("Invalid page or size");
  }
}

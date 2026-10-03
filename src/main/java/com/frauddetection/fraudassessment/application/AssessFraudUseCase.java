package com.frauddetection.fraudassessment.application;

import com.frauddetection.fraudassessment.domain.AssessmentInput;
import com.frauddetection.fraudassessment.domain.FraudAssessment;

public interface AssessFraudUseCase {
  FraudAssessment assess(AssessmentInput input);
}

package com.frauddetection.fraudassessment.application;

import com.frauddetection.fraudassessment.domain.FraudCase;
import com.frauddetection.fraudassessment.domain.FraudCaseAction;
import com.frauddetection.fraudassessment.domain.FraudCaseSummary;
import java.util.Optional;
import java.util.UUID;

public interface FraudCaseRepository {
  Optional<FraudCase> findById(UUID id);

  Optional<FraudCase> findByIdForUpdate(UUID id);

  Optional<FraudCase> findByAssessmentId(UUID assessmentId);

  AssessmentPage<FraudCaseSummary> list(FraudCaseQuery query);

  FraudCase create(FraudCase fraudCase, FraudCaseAction openedAction);

  void saveTransition(FraudCase fraudCase, FraudCaseAction action);
}

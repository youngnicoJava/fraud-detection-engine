package com.frauddetection.fraudassessment.application;

import com.frauddetection.fraudassessment.domain.AssessmentHistory;
import com.frauddetection.fraudassessment.domain.FraudAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudAssessmentRepository {
  Optional<FraudAssessment> findByRequestId(UUID requestId);

  Optional<FraudAssessment> findById(UUID id);

  List<AssessmentHistory> recentByCustomer(UUID customerReference, int limit);

  void saveWithResultOutbox(FraudAssessment assessment, String requestHash);
}

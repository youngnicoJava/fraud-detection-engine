package com.frauddetection.fraudassessment.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class FraudAssessmentPanacheRepository
    implements PanacheRepositoryBase<FraudAssessmentEntity, UUID> {
  public List<FraudAssessmentEntity> recent(UUID customerReference, int limit) {
    return find("customerReference = ?1 order by evaluatedAt desc", customerReference)
        .page(0, limit)
        .list();
  }

  public List<FraudAssessmentEntity> search(
      String filter, Map<String, Object> parameters, int page, int size) {
    return find(filter + " order by evaluatedAt desc, id desc", parameters).page(page, size).list();
  }

  public long countMatching(String filter, Map<String, Object> parameters) {
    return find(filter, parameters).count();
  }
}

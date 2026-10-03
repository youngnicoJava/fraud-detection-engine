package com.frauddetection.fraudassessment.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FraudCasePanacheRepository implements PanacheRepositoryBase<FraudCaseEntity, UUID> {
  public java.util.Optional<FraudCaseEntity> findForUpdate(UUID id) {
    return find("id", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
  }

  public List<FraudCaseActionEntity> history(UUID id) {
    return FraudCaseActionEntity.list("fraudCaseId = ?1 order by occurredAt asc, id asc", id);
  }
}

package com.frauddetection.outbox.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class FraudOutboxPanacheRepository
    implements PanacheRepositoryBase<FraudOutboxEntity, UUID> {
  public long pendingCount() {
    return count("status = 'PENDING'");
  }
}

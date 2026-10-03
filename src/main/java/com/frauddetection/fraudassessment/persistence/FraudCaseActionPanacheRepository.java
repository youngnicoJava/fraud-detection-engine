package com.frauddetection.fraudassessment.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class FraudCaseActionPanacheRepository
    implements PanacheRepositoryBase<FraudCaseActionEntity, UUID> {}

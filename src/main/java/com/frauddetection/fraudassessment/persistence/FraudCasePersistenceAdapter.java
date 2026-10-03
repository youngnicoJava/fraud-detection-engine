package com.frauddetection.fraudassessment.persistence;

import com.frauddetection.fraudassessment.application.AssessmentPage;
import com.frauddetection.fraudassessment.application.FraudCaseQuery;
import com.frauddetection.fraudassessment.application.FraudCaseRepository;
import com.frauddetection.fraudassessment.domain.FraudCase;
import com.frauddetection.fraudassessment.domain.FraudCaseAction;
import com.frauddetection.fraudassessment.domain.FraudCaseSummary;
import com.frauddetection.fraudassessment.domain.FraudDecision;
import com.frauddetection.fraudassessment.domain.FraudRiskLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FraudCasePersistenceAdapter implements FraudCaseRepository {
  private final FraudCasePanacheRepository cases;
  private final FraudCaseActionPanacheRepository actions;

  @Inject
  public FraudCasePersistenceAdapter(
      FraudCasePanacheRepository cases, FraudCaseActionPanacheRepository actions) {
    this.cases = cases;
    this.actions = actions;
  }

  @Override
  public Optional<FraudCase> findById(UUID id) {
    return cases.findByIdOptional(id).map(this::toDomain);
  }

  @Override
  public Optional<FraudCase> findByIdForUpdate(UUID id) {
    return cases.findForUpdate(id).map(this::toDomain);
  }

  @Override
  public Optional<FraudCase> findByAssessmentId(UUID assessmentId) {
    return cases.find("assessmentId", assessmentId).firstResultOptional().map(this::toDomain);
  }

  @Override
  public AssessmentPage<FraudCaseSummary> list(FraudCaseQuery query) {
    StringBuilder criteria = new StringBuilder("1 = 1");
    Map<String, Object> parameters = new HashMap<>();
    if (query.status() != null) add(criteria, parameters, "status", query.status().name());
    if (query.resolution() != null)
      add(criteria, parameters, "resolution", query.resolution().name());
    String filter = criteria.toString();
    long total = cases.count(filter, parameters);
    List<FraudCaseSummary> items =
        cases
            .find(
                "select c from FraudCaseEntity c join fetch c.assessment where "
                    + filter
                    + " order by c.createdAt desc, c.id desc",
                parameters)
            .page(query.page(), query.size())
            .list()
            .stream()
            .map(
                c ->
                    new FraudCaseSummary(
                        c.id,
                        c.assessmentId,
                        c.loanApplicationId,
                        FraudDecision.valueOf(c.automatedDecision),
                        c.assessment.score,
                        FraudRiskLevel.valueOf(c.assessment.riskLevel),
                        com.frauddetection.fraudassessment.domain.FraudCaseStatus.valueOf(c.status),
                        c.resolution == null
                            ? null
                            : com.frauddetection.fraudassessment.domain.FraudCaseResolution.valueOf(
                                c.resolution),
                        c.createdAt,
                        c.updatedAt))
            .toList();
    int pages = total == 0 ? 0 : (int) ((total + query.size() - 1) / query.size());
    return new AssessmentPage<>(items, query.page(), query.size(), total, pages);
  }

  @Override
  @Transactional
  public FraudCase create(FraudCase fraudCase, FraudCaseAction openedAction) {
    cases.persist(FraudCaseEntity.from(fraudCase));
    actions.persist(FraudCaseActionEntity.from(fraudCase.id(), openedAction));
    return fraudCase;
  }

  @Override
  @Transactional
  public void saveTransition(FraudCase fraudCase, FraudCaseAction action) {
    var entity = cases.findById(fraudCase.id());
    entity.status = fraudCase.status().name();
    entity.resolution = fraudCase.resolution() == null ? null : fraudCase.resolution().name();
    entity.resolutionNote = fraudCase.resolutionNote();
    entity.updatedAt = fraudCase.updatedAt();
    entity.resolvedAt = fraudCase.resolvedAt();
    actions.persist(FraudCaseActionEntity.from(fraudCase.id(), action));
  }

  private FraudCase toDomain(FraudCaseEntity entity) {
    return entity.toDomain(
        cases.history(entity.id).stream().map(FraudCaseActionEntity::toDomain).toList());
  }

  private void add(
      StringBuilder query, Map<String, Object> parameters, String field, Object value) {
    String name = "filter" + parameters.size();
    query.append(" and ").append(field).append(" = :").append(name);
    parameters.put(name, value);
  }
}

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../shared/api/client';
import {
  ErrorDetails,
  Loading,
  PageHeader,
  StatusBadge,
  formatDate,
  shortId,
} from '../../shared/components/Primitives';
import type {
  AssessmentSummary,
  FraudDecision,
  FraudRiskLevel,
  Page,
} from '../../shared/types/api';
import styles from './AssessmentsPage.module.css';

const pageSize = 10;

export function AssessmentsPage() {
  const [page, setPage] = useState(0);
  const [decision, setDecision] = useState<FraudDecision | ''>('');
  const [risk, setRisk] = useState<FraudRiskLevel | ''>('');
  const [data, setData] = useState<Page<AssessmentSummary>>();
  const [error, setError] = useState<unknown>();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(undefined);
    api
      .assessments({
        page,
        size: pageSize,
        decision: decision || undefined,
        riskLevel: risk || undefined,
      })
      .then((result) => {
        if (current) setData(result);
      })
      .catch((reason: unknown) => {
        if (current) setError(reason);
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => {
      current = false;
    };
  }, [page, decision, risk]);

  const changeDecision = (value: string) => {
    setDecision(value as FraudDecision | '');
    setPage(0);
  };
  const changeRisk = (value: string) => {
    setRisk(value as FraudRiskLevel | '');
    setPage(0);
  };

  return (
    <>
      <PageHeader
        eyebrow="Portfolio monitoring"
        title="Assessment history"
        description="Review the automated decision, score and ruleset recorded for each loan application."
      />
      <section className={styles.panel}>
        <div className={styles.toolbar}>
          <label>
            Decision
            <select value={decision} onChange={(event) => changeDecision(event.target.value)}>
              <option value="">All decisions</option>
              <option value="PASS">Pass</option>
              <option value="REVIEW">Review</option>
              <option value="BLOCK">Block</option>
            </select>
          </label>
          <label>
            Risk level
            <select value={risk} onChange={(event) => changeRisk(event.target.value)}>
              <option value="">All levels</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </label>
          <div className={styles.count}>{data?.totalElements ?? '—'} assessments</div>
        </div>
        {loading ? (
          <Loading label="Loading assessments…" />
        ) : error ? (
          <ErrorDetails error={error} />
        ) : data?.items.length ? (
          <>
            <div className={styles.scroll}>
              <table>
                <thead>
                  <tr>
                    <th>Evaluated</th>
                    <th>Application</th>
                    <th>Decision</th>
                    <th>Score</th>
                    <th>Risk</th>
                    <th>Ruleset</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((item) => (
                    <tr key={item.fraudAssessmentId}>
                      <td>{formatDate(item.evaluatedAt)}</td>
                      <td>
                        <Link
                          className={styles.reference}
                          to={`/assessments/${item.fraudAssessmentId}`}
                        >
                          {shortId(item.loanApplicationId)}
                        </Link>
                      </td>
                      <td>
                        <StatusBadge value={item.decision} />
                      </td>
                      <td>
                        <strong>{item.fraudScore}</strong>
                        <span className={styles.scoreSuffix}> / 100</span>
                      </td>
                      <td>
                        <StatusBadge value={item.riskLevel} />
                      </td>
                      <td>
                        <span>{item.rulesetId}</span>
                        <small>{item.rulesetVersion}</small>
                      </td>
                      <td>
                        <Link className={styles.open} to={`/assessments/${item.fraudAssessmentId}`}>
                          Inspect
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
          </>
        ) : (
          <div className={styles.empty}>
            <strong>No assessments found</strong>
            <span>New results will appear here as Loan Origination requests them.</span>
          </div>
        )}
      </section>
    </>
  );
}

function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  return (
    <div className={styles.pagination}>
      <span>
        Page {totalPages ? page + 1 : 0} of {totalPages}
      </span>
      <div>
        <button disabled={page <= 0} onClick={() => onChange(page - 1)}>
          Previous
        </button>
        <button disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          Next
        </button>
      </div>
    </div>
  );
}

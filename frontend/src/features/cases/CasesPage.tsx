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
import type { FraudCaseStatus, FraudCaseSummary, Page } from '../../shared/types/api';
import styles from './CasesPage.module.css';

const pageSize = 10;

export function CasesPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<FraudCaseStatus | ''>('');
  const [data, setData] = useState<Page<FraudCaseSummary>>();
  const [error, setError] = useState<unknown>();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(undefined);
    api
      .cases({ page, size: pageSize, status: status || undefined })
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
  }, [page, status]);

  return (
    <>
      <PageHeader
        eyebrow="Manual investigation"
        title="Case queue"
        description="Review cases opened for automated REVIEW and BLOCK decisions. Each case preserves its original assessment and records later analyst actions."
      />
      <section className={styles.panel}>
        <div className={styles.toolbar}>
          <label htmlFor="case-status">Case status</label>
          <select
            id="case-status"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as FraudCaseStatus | '');
              setPage(0);
            }}
          >
            <option value="">All cases</option>
            <option value="OPEN">Open</option>
            <option value="UNDER_REVIEW">Under review</option>
            <option value="RESOLVED">Resolved</option>
          </select>
          <span>{data?.totalElements ?? '—'} cases</span>
        </div>
        {loading ? (
          <Loading label="Loading investigation queue…" />
        ) : error ? (
          <ErrorDetails error={error} />
        ) : data?.items.length ? (
          <>
            <div className={styles.scroll}>
              <table>
                <thead>
                  <tr>
                    <th>Created</th>
                    <th>Application</th>
                    <th>Automated result</th>
                    <th>Score</th>
                    <th>Risk</th>
                    <th>Case status</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((item) => (
                    <tr key={item.id}>
                      <td>{formatDate(item.createdAt)}</td>
                      <td>
                        <Link className={styles.reference} to={`/cases/${item.id}`}>
                          {shortId(item.loanApplicationId)}
                        </Link>
                      </td>
                      <td>
                        <StatusBadge value={item.automatedDecision} />
                      </td>
                      <td>
                        <strong>{item.fraudScore}</strong>
                        <span className={styles.max}> / 100</span>
                      </td>
                      <td>
                        <StatusBadge value={item.riskLevel} />
                      </td>
                      <td>
                        <StatusBadge value={item.status} />
                      </td>
                      <td>
                        <Link className={styles.inspect} to={`/cases/${item.id}`}>
                          Investigate
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.pagination}>
              <span>
                Page {page + 1} of {data.totalPages}
              </span>
              <div>
                <button disabled={page <= 0} onClick={() => setPage(page - 1)}>
                  Previous
                </button>
                <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
                  Next
                </button>
              </div>
            </div>
          </>
        ) : (
          <div className={styles.empty}>
            <strong>No cases in this queue</strong>
            <span>
              Cases open automatically for REVIEW and BLOCK outcomes. PASS assessments remain in
              assessment history only.
            </span>
          </div>
        )}
      </section>
    </>
  );
}

import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../shared/api/client';
import {
  ErrorDetails,
  Loading,
  Message,
  PageHeader,
  StatusBadge,
  formatDate,
  shortId,
} from '../../shared/components/Primitives';
import type { FraudAssessment } from '../../shared/types/api';
import styles from './AssessmentPage.module.css';

export function AssessmentPage() {
  const { id = '' } = useParams();
  const [assessment, setAssessment] = useState<FraudAssessment>();
  const [error, setError] = useState<unknown>();
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let current = true;
    setLoading(true);
    api
      .assessment(id)
      .then((result) => {
        if (current) setAssessment(result);
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
  }, [id]);

  if (loading) return <Loading label="Loading assessment explanation…" />;
  if (error) return <ErrorDetails error={error} />;
  if (!assessment) return <Message kind="error">Assessment was not found.</Message>;

  const copyCorrelation = async () => {
    await navigator.clipboard.writeText(assessment.correlationId);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  return (
    <>
      <div className={styles.back}>
        <Link to="/assessments">← Assessment history</Link>
      </div>
      <PageHeader
        eyebrow="Automated evaluation"
        title="Assessment explanation"
        description={`Application ${shortId(assessment.loanApplicationId)} · evaluated ${formatDate(assessment.evaluatedAt)}`}
        action={<StatusBadge value={assessment.decision} />}
      />
      <section className={styles.summary}>
        <div className={styles.scoreBlock}>
          <div className={styles.label}>FRAUD RISK SCORE</div>
          <div className={styles.score}>
            {assessment.fraudScore}
            <span> / 100</span>
          </div>
          <div
            className={styles.track}
            role="img"
            aria-label={`Fraud risk score ${assessment.fraudScore} out of 100`}
          >
            <span style={{ width: `${assessment.fraudScore}%` }} />
          </div>
          <div className={styles.scale}>
            <span>LOW</span>
            <span>MEDIUM</span>
            <span>HIGH</span>
          </div>
          <div className={styles.disclaimer}>
            A deterministic risk score, not a probability of fraud.
          </div>
        </div>
        <div className={styles.meta}>
          <Field label="Risk level">
            <StatusBadge value={assessment.riskLevel} />
          </Field>
          <Field label="Ruleset">
            {assessment.rulesetId} <small>v{assessment.rulesetVersion}</small>
          </Field>
          <Field label="Requested amount">
            {new Intl.NumberFormat(undefined, {
              style: 'currency',
              currency: assessment.currency,
            }).format(assessment.requestedAmount)}
          </Field>
          <Field label="Product / term">
            {assessment.productType} · {assessment.termMonths} months
          </Field>
          <Field label="Application reference">
            <code>{assessment.loanApplicationId}</code>
          </Field>
          <Field label="Assessment reference">
            <code>{assessment.fraudAssessmentId}</code>
          </Field>
          <Field label="Correlation ID">
            <div className={styles.correlation}>
              <code>{assessment.correlationId}</code>
              <button onClick={copyCorrelation}>{copied ? 'Copied' : 'Copy'}</button>
            </div>
          </Field>
        </div>
      </section>
      {assessment.decision !== 'PASS' && (
        <div className={styles.caseLink}>
          <span>This assessment has an investigation case.</span>
          <Link to="/cases">Open case queue →</Link>
        </div>
      )}
      <div className={styles.columns}>
        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <div className={styles.label}>OBSERVED SIGNALS</div>
              <h2>What the rules detected</h2>
            </div>
            <span>{assessment.signals.length} signals</span>
          </div>
          {assessment.signals.length ? (
            <ul className={styles.signalList}>
              {assessment.signals.map((signal) => (
                <li key={signal.code}>
                  <StatusBadge value={assessment.riskLevel} />
                  <div>
                    <strong>{humanize(signal.code)}</strong>
                    <p>{signal.explanation}</p>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <div className={styles.noSignals}>
              No material fraud signals were observed for this request.
            </div>
          )}
        </section>
        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <div className={styles.label}>SCORE BREAKDOWN</div>
              <h2>Rule contributions</h2>
            </div>
          </div>
          {assessment.contributions.length ? (
            <ul className={styles.contributions}>
              {assessment.contributions.map((item, index) => (
                <li key={`${item.code}-${index}`}>
                  <div>
                    <strong>{humanize(item.code)}</strong>
                    <p>{item.explanation}</p>
                    <code>{item.code}</code>
                  </div>
                  <b>+{item.points}</b>
                </li>
              ))}
            </ul>
          ) : (
            <div className={styles.noSignals}>No rules contributed points.</div>
          )}
          <div className={styles.reasons}>
            <div className={styles.label}>REASON CODES</div>
            <div>
              {assessment.reasonCodes.map((code) => (
                <code key={code}>{code}</code>
              ))}
            </div>
          </div>
        </section>
      </div>
    </>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className={styles.field}>
      <span>{label}</span>
      <div>{children}</div>
    </div>
  );
}

function humanize(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

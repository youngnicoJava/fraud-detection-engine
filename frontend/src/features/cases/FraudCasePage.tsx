import { useCallback, useEffect, useState } from 'react';
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
import type { FraudCaseDetail, FraudCaseResolution } from '../../shared/types/api';
import styles from './FraudCasePage.module.css';

export function FraudCasePage() {
  const { id = '' } = useParams();
  const [data, setData] = useState<FraudCaseDetail>();
  const [error, setError] = useState<unknown>();
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [resolution, setResolution] = useState<FraudCaseResolution>('CLEARED');
  const [note, setNote] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [notice, setNotice] = useState('');

  const refresh = useCallback(async () => {
    const result = await api.fraudCase(id);
    setData(result);
  }, [id]);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(undefined);
    api
      .fraudCase(id)
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
  }, [id]);

  const startReview = async () => {
    setBusy(true);
    setError(undefined);
    try {
      await api.startReview(id);
      await refresh();
      setNotice('Investigation started. The action is recorded in the case history.');
    } catch (reason) {
      setError(reason);
    } finally {
      setBusy(false);
    }
  };

  const resolve = async () => {
    setBusy(true);
    setError(undefined);
    try {
      await api.resolveCase(id, resolution, note);
      await refresh();
      setConfirming(false);
      setNotice(
        `Case resolved: ${resolution === 'CLEARED' ? 'cleared' : 'confirmed fraud'}. Automated assessment history remains unchanged.`,
      );
    } catch (reason) {
      setError(reason);
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Loading label="Loading case file…" />;
  if (error && !data) return <ErrorDetails error={error} />;
  if (!data) return <Message kind="error">Fraud case was not found.</Message>;

  const { fraudCase, assessment } = data;
  return (
    <>
      <div className={styles.back}>
        <Link to="/cases">← Case queue</Link>
      </div>
      <PageHeader
        eyebrow="Investigation file"
        title="Fraud case"
        description={`Case ${shortId(fraudCase.id)} · application ${shortId(fraudCase.loanApplicationId)}`}
        action={<StatusBadge value={fraudCase.status} />}
      />
      {notice && (
        <div className={styles.notice}>
          <Message kind="success">{notice}</Message>
        </div>
      )}
      {error && (
        <div className={styles.notice}>
          <ErrorDetails error={error} />
        </div>
      )}
      <section className={styles.overview}>
        <div>
          <div className={styles.kicker}>AUTOMATED ASSESSMENT</div>
          <div className={styles.outcome}>
            <StatusBadge value={assessment.decision} />
            <span>
              {assessment.fraudScore}
              <small> / 100</small>
            </span>
            <StatusBadge value={assessment.riskLevel} />
          </div>
          <p>
            Ruleset {assessment.rulesetId} · version {assessment.rulesetVersion}
          </p>
          <p>Evaluated {formatDate(assessment.evaluatedAt)}</p>
          <p>
            Application {assessment.productType} · {assessment.termMonths} months ·{' '}
            {new Intl.NumberFormat(undefined, {
              style: 'currency',
              currency: assessment.currency,
            }).format(assessment.requestedAmount)}
          </p>
          <div className={styles.disclaimer}>
            The automated score is a risk indicator, not a probability of fraud.
          </div>
          <Link
            className={styles.assessmentLink}
            to={`/assessments/${assessment.fraudAssessmentId}`}
          >
            Open full assessment explanation →
          </Link>
        </div>
        <div className={styles.caseMeta}>
          <Field label="Case status">
            <StatusBadge value={fraudCase.status} />
          </Field>
          <Field label="Opened">{formatDate(fraudCase.createdAt)}</Field>
          {fraudCase.resolution && (
            <Field label="Resolution">
              <StatusBadge value={fraudCase.resolution} />
            </Field>
          )}
          {fraudCase.resolvedAt && (
            <Field label="Resolved">{formatDate(fraudCase.resolvedAt)}</Field>
          )}
          <Field label="Correlation ID">
            <code>{fraudCase.correlationId}</code>
          </Field>
          <Field label="Automated assessment">
            <code>{shortId(fraudCase.assessmentId)}</code>
          </Field>
        </div>
      </section>

      <div className={styles.columns}>
        <section className={styles.panel}>
          <PanelTitle eyebrow="INVESTIGATION" title="Case history" />
          <ol className={styles.timeline}>
            {fraudCase.history.map((action) => (
              <li key={action.id}>
                <span className={styles.marker} />
                <div className={styles.event}>
                  <div className={styles.eventTop}>
                    <strong>{actionLabel(action.action)}</strong>
                    <time>{formatDate(action.occurredAt)}</time>
                  </div>
                  <div className={styles.actor}>
                    Actor <code>{action.actorId}</code>
                  </div>
                  {action.resolution && (
                    <p>
                      Resolution: <StatusBadge value={action.resolution} />
                    </p>
                  )}
                  {action.note && <blockquote>{action.note}</blockquote>}
                  <div className={styles.correlation}>
                    <span>Correlation</span>
                    <code>{action.correlationId}</code>
                  </div>
                </div>
              </li>
            ))}
          </ol>
        </section>
        <section className={styles.panel}>
          <PanelTitle eyebrow="ANALYST ACTIONS" title="Investigation controls" />
          {fraudCase.status === 'OPEN' && (
            <div className={styles.controls}>
              <p>Start the case review to record that an analyst has begun investigating.</p>
              <button className={styles.secondary} disabled={busy} onClick={startReview}>
                {busy ? 'Saving…' : 'Start review'}
              </button>
            </div>
          )}
          {fraudCase.status === 'UNDER_REVIEW' && (
            <div className={styles.controls}>
              <p>
                Record a final analyst outcome. This does not change the original automated decision
                or score.
              </p>
              <label htmlFor="resolution">Resolution</label>
              <select
                id="resolution"
                value={resolution}
                onChange={(event) => setResolution(event.target.value as FraudCaseResolution)}
              >
                <option value="CLEARED">Cleared</option>
                <option value="CONFIRMED_FRAUD">Confirmed fraud</option>
              </select>
              <label htmlFor="resolution-note">
                Resolution note <span>(optional, 1,000 characters max)</span>
              </label>
              <textarea
                id="resolution-note"
                maxLength={1000}
                rows={4}
                value={note}
                onChange={(event) => setNote(event.target.value)}
              />
              <div className={styles.noteCount}>{note.length} / 1000</div>
              <button
                className={styles.primary}
                disabled={busy}
                onClick={() => setConfirming(true)}
              >
                Review resolution
              </button>
            </div>
          )}
          {fraudCase.status === 'RESOLVED' && (
            <div className={styles.resolved}>
              <StatusBadge value={fraudCase.resolution!} />
              <p>This case is final. A resolved case cannot be changed.</p>
              {fraudCase.resolutionNote && <blockquote>{fraudCase.resolutionNote}</blockquote>}
              {fraudCase.resolution === 'CLEARED' && (
                <Message kind="success">
                  Loan Origination will receive a versioned manual-clearance event. The original
                  assessment stays {assessment.decision}.
                </Message>
              )}
              {fraudCase.resolution === 'CONFIRMED_FRAUD' && (
                <Message kind="error">
                  Loan Origination keeps the fraud gate blocked from approval and offer issuance.
                </Message>
              )}
            </div>
          )}
        </section>
      </div>

      <section className={styles.panel}>
        <PanelTitle eyebrow="ORIGINAL AUTOMATED RESULT" title="Signals and reasons" />
        <div className={styles.explanation}>
          {assessment.signals.length ? (
            assessment.signals.map((signal) => (
              <article key={signal.code}>
                <strong>{signal.code}</strong>
                <p>{signal.explanation}</p>
              </article>
            ))
          ) : (
            <p>No triggered signals were recorded.</p>
          )}
          <div className={styles.reasons}>
            {assessment.reasonCodes.map((reason) => (
              <code key={reason}>{reason}</code>
            ))}
          </div>
        </div>
      </section>

      {confirming && (
        <div className={styles.scrim}>
          <section
            className={styles.dialog}
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-title"
          >
            <div className={styles.kicker}>FINAL CASE ACTION</div>
            <h2 id="confirm-title">
              Confirm {resolution === 'CLEARED' ? 'clearance' : 'fraud finding'}?
            </h2>
            <p>
              This resolution is immutable and will be added to the investigation history. The
              automated assessment will remain {assessment.decision}.
            </p>
            <div className={styles.dialogActions}>
              <button
                className={styles.secondary}
                disabled={busy}
                onClick={() => setConfirming(false)}
              >
                Go back
              </button>
              <button className={styles.primary} disabled={busy} onClick={resolve}>
                {busy ? 'Saving…' : 'Confirm resolution'}
              </button>
            </div>
          </section>
        </div>
      )}
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

function PanelTitle({ eyebrow, title }: { eyebrow: string; title: string }) {
  return (
    <header className={styles.panelTitle}>
      <div>
        <div className={styles.kicker}>{eyebrow}</div>
        <h2>{title}</h2>
      </div>
    </header>
  );
}

function actionLabel(action: string) {
  if (action === 'CASE_OPENED') return 'Case opened automatically';
  if (action === 'REVIEW_STARTED') return 'Analyst started review';
  return 'Case resolved';
}

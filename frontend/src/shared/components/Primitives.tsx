import type { ReactNode } from 'react';
import { ApiRequestError } from '../api/errors';
import type {
  FraudCaseResolution,
  FraudCaseStatus,
  FraudDecision,
  FraudRiskLevel,
} from '../types/api';
import styles from './Primitives.module.css';

type Status = FraudDecision | FraudRiskLevel | FraudCaseStatus | FraudCaseResolution;

const labels: Record<Status, string> = {
  PASS: 'Pass',
  REVIEW: 'Review',
  BLOCK: 'Block',
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  OPEN: 'Open',
  UNDER_REVIEW: 'Under review',
  RESOLVED: 'Resolved',
  CLEARED: 'Cleared',
  CONFIRMED_FRAUD: 'Confirmed fraud',
};

export function StatusBadge({ value }: { value: Status }) {
  const tone = ['PASS', 'LOW', 'CLEARED'].includes(value)
    ? styles.good
    : ['REVIEW', 'MEDIUM', 'OPEN', 'UNDER_REVIEW'].includes(value)
      ? styles.warn
      : styles.bad;
  return <span className={`${styles.badge} ${tone}`}>{labels[value]}</span>;
}

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <header className={styles.header}>
      <div>
        <div className={styles.eyebrow}>{eyebrow}</div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action && <div>{action}</div>}
    </header>
  );
}

export function Message({
  kind = 'info',
  children,
}: {
  kind?: 'info' | 'error' | 'success';
  children: ReactNode;
}) {
  return (
    <div
      className={`${styles.message} ${styles[kind]}`}
      role={kind === 'error' ? 'alert' : 'status'}
    >
      {children}
    </div>
  );
}

export function Loading({ label = 'Loading data…' }: { label?: string }) {
  return (
    <div className={styles.loading} role="status">
      <span className={styles.dot} />
      {label}
    </div>
  );
}

export function ErrorDetails({ error }: { error: unknown }) {
  const value = error instanceof Error ? error : new Error('Unexpected service error');
  const correlationId = error instanceof ApiRequestError ? error.correlationId : undefined;
  return (
    <Message kind="error">
      <strong>{value.message}</strong>
      {correlationId && (
        <span>
          Correlation ID: <code>{correlationId}</code>
        </span>
      )}
    </Message>
  );
}

export function formatDate(value: string | null | undefined) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? '—'
    : new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(date);
}

export function shortId(value: string) {
  return `${value.slice(0, 8)}…${value.slice(-4)}`;
}

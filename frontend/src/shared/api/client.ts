import { getAccessToken } from '../../features/auth/oidc';
import { ApiRequestError } from './errors';
import type {
  AssessmentSummary,
  FraudAssessment,
  FraudCase,
  FraudCaseDetail,
  FraudCaseResolution,
  FraudCaseStatus,
  FraudCaseSummary,
  FraudDecision,
  FraudRiskLevel,
  Page,
} from '../types/api';

const baseUrl = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = await getAccessToken();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  headers.set('X-Correlation-ID', crypto.randomUUID());
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (init.body) headers.set('Content-Type', 'application/json');
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers });
  if (!response.ok) {
    const body = (await response.json().catch(() => undefined)) as
      { code?: string; message?: string; correlationId?: string } | undefined;
    throw new ApiRequestError(response.status, body, `Request failed (${response.status}).`);
  }
  return (await response.json()) as T;
}

export const api = {
  assessments(filters: {
    page: number;
    size: number;
    decision?: FraudDecision;
    riskLevel?: FraudRiskLevel;
  }) {
    const params = new URLSearchParams({ page: String(filters.page), size: String(filters.size) });
    if (filters.decision) params.set('decision', filters.decision);
    if (filters.riskLevel) params.set('riskLevel', filters.riskLevel);
    return request<Page<AssessmentSummary>>(`/api/v1/fraud-assessments?${params}`);
  },
  assessment(id: string) {
    return request<FraudAssessment>(`/api/v1/fraud-assessments/${encodeURIComponent(id)}`);
  },
  cases(filters: { page: number; size: number; status?: FraudCaseStatus }) {
    const params = new URLSearchParams({ page: String(filters.page), size: String(filters.size) });
    if (filters.status) params.set('status', filters.status);
    return request<Page<FraudCaseSummary>>(`/api/v1/fraud-cases?${params}`);
  },
  fraudCase(id: string) {
    return request<FraudCaseDetail>(`/api/v1/fraud-cases/${encodeURIComponent(id)}`);
  },
  startReview(id: string) {
    return request<FraudCase>(`/api/v1/fraud-cases/${encodeURIComponent(id)}/start-review`, {
      method: 'POST',
      body: JSON.stringify({}),
    });
  },
  resolveCase(id: string, resolution: FraudCaseResolution, note: string) {
    return request<FraudCase>(`/api/v1/fraud-cases/${encodeURIComponent(id)}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ resolution, note: note.trim() || null }),
    });
  },
};

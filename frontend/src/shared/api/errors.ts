import type { ApiErrorBody } from '../types/api';

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly correlationId?: string;

  constructor(status: number, body: Partial<ApiErrorBody> | undefined, fallback: string) {
    super(body?.message || fallback);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = body?.code;
    this.correlationId = body?.correlationId ?? undefined;
  }
}

export function presentError(error: unknown): { message: string; correlationId?: string } {
  if (error instanceof ApiRequestError) {
    return { message: error.message, correlationId: error.correlationId };
  }
  if (error instanceof Error) return { message: error.message };
  return { message: 'Unexpected error while contacting the service.' };
}

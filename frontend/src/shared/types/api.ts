export type FraudDecision = 'PASS' | 'REVIEW' | 'BLOCK';
export type FraudRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type FraudCaseStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED';
export type FraudCaseResolution = 'CLEARED' | 'CONFIRMED_FRAUD';

export type FraudSignal = { code: string; explanation: string };
export type ScoreContribution = { code: string; points: number; explanation: string };

export type FraudAssessment = {
  fraudAssessmentId: string;
  assessmentRequestId: string;
  loanApplicationId: string;
  decision: FraudDecision;
  fraudScore: number;
  riskLevel: FraudRiskLevel;
  requestedAmount: number;
  currency: string;
  termMonths: number;
  productType: string;
  rulesetId: string;
  rulesetVersion: string;
  evaluatedAt: string;
  correlationId: string;
  signals: FraudSignal[];
  contributions: ScoreContribution[];
  reasonCodes: string[];
};

export type AssessmentSummary = Pick<
  FraudAssessment,
  | 'fraudAssessmentId'
  | 'assessmentRequestId'
  | 'loanApplicationId'
  | 'decision'
  | 'fraudScore'
  | 'riskLevel'
  | 'evaluatedAt'
  | 'rulesetId'
  | 'rulesetVersion'
  | 'correlationId'
>;

export type FraudCaseAction = {
  id: string;
  action: 'CASE_OPENED' | 'REVIEW_STARTED' | 'CASE_RESOLVED';
  actorId: string;
  occurredAt: string;
  correlationId: string;
  resolution: FraudCaseResolution | null;
  note: string | null;
};

export type FraudCase = {
  id: string;
  assessmentId: string;
  loanApplicationId: string;
  automatedDecision: Exclude<FraudDecision, 'PASS'>;
  status: FraudCaseStatus;
  resolution: FraudCaseResolution | null;
  resolutionNote: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  correlationId: string;
  history: FraudCaseAction[];
};

export type FraudCaseSummary = Pick<
  FraudCase,
  | 'id'
  | 'assessmentId'
  | 'loanApplicationId'
  | 'automatedDecision'
  | 'status'
  | 'resolution'
  | 'createdAt'
  | 'updatedAt'
> &
  Pick<FraudAssessment, 'fraudScore' | 'riskLevel'>;

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type FraudCaseDetail = { fraudCase: FraudCase; assessment: FraudAssessment };

export type ApiErrorBody = {
  code: string;
  message: string;
  timestamp?: string;
  correlationId?: string | null;
};

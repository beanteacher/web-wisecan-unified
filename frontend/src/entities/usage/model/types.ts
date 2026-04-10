export interface SummaryResponse {
  totalCalls: number;
  successCalls: number;
  failCalls: number;
}

export interface HistoryItem {
  id: number;
  endpoint: string;
  status: 'SUCCESS' | 'FAIL';
  calledAt: string;
}

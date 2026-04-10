import { api } from '@/shared/api/client';
import type { SummaryResponse, HistoryItem } from '../model/types';

export async function getUsageSummary(): Promise<SummaryResponse> {
  return api.get('usage/summary').json<SummaryResponse>();
}

export async function getUsageHistory(): Promise<HistoryItem[]> {
  return api.get('usage/history').json<HistoryItem[]>();
}

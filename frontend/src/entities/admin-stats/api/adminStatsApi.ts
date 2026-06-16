import { api } from '@/shared/api/client';
import type {
  DashboardSummary,
  SendVolumeStats,
  ChannelBreakdown,
  MemberGrowthStats,
  SystemSetting,
  StatsPeriod,
} from '../model/types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export async function fetchDashboardSummary(): Promise<DashboardSummary> {
  const res = await api.get('admin/stats/dashboard').json<ApiResponse<DashboardSummary>>();
  return res.data;
}

export async function fetchSendStats(period: StatsPeriod = 'DAILY'): Promise<SendVolumeStats> {
  const res = await api
    .get('admin/stats/send', { searchParams: { period } })
    .json<ApiResponse<SendVolumeStats>>();
  return res.data;
}

export async function fetchChannelBreakdown(): Promise<ChannelBreakdown[]> {
  const res = await api.get('admin/stats/send/channels').json<ApiResponse<ChannelBreakdown[]>>();
  return res.data;
}

export async function fetchMemberStats(period: StatsPeriod = 'DAILY'): Promise<MemberGrowthStats> {
  const res = await api
    .get('admin/stats/members', { searchParams: { period } })
    .json<ApiResponse<MemberGrowthStats>>();
  return res.data;
}

export async function fetchSystemSettings(): Promise<SystemSetting[]> {
  const res = await api.get('admin/settings').json<ApiResponse<SystemSetting[]>>();
  return res.data;
}

export async function updateSystemSetting(key: string, value: string): Promise<SystemSetting> {
  const res = await api
    .put(`admin/settings/${key}`, { json: { value } })
    .json<ApiResponse<SystemSetting>>();
  return res.data;
}

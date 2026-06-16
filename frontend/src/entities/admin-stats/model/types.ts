export interface DashboardSummary {
  totalMembers: number;
  newMembersToday: number;
  totalSentToday: number;
  totalSentThisWeek: number;
  totalSentThisMonth: number;
  revenueToday: number;
  revenueThisWeek: number;
  revenueThisMonth: number;
  pendingSendRequests: number;
  failedSendRequests: number;
}

export interface SendVolumePoint {
  date: string;
  count: number;
  recipientCount: number;
  totalCost: number;
}

export interface SendVolumeStats {
  period: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  data: SendVolumePoint[];
}

export interface ChannelBreakdown {
  channel: string;
  count: number;
  recipientCount: number;
  totalCost: number;
}

export interface MemberGrowthPoint {
  date: string;
  newMembers: number;
  totalMembers: number;
}

export interface MemberGrowthStats {
  period: string;
  data: MemberGrowthPoint[];
}

export interface SystemSetting {
  key: string;
  value: string;
  description: string;
  updatedAt: string;
}

export type StatsPeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY';

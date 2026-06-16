import { useQuery } from '@tanstack/react-query';
import { fetchSendStats, fetchChannelBreakdown } from '@/entities/admin-stats';
import type { StatsPeriod } from '@/entities/admin-stats';

export function useSendStats(period: StatsPeriod = 'DAILY') {
  return useQuery({
    queryKey: ['admin', 'stats', 'send', period],
    queryFn: () => fetchSendStats(period),
    staleTime: 5 * 60 * 1000,
  });
}

export function useChannelBreakdown() {
  return useQuery({
    queryKey: ['admin', 'stats', 'channels'],
    queryFn: fetchChannelBreakdown,
    staleTime: 5 * 60 * 1000,
  });
}

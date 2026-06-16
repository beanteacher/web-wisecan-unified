import { useQuery } from '@tanstack/react-query';
import { fetchDashboardSummary } from '@/entities/admin-stats';

export function useDashboardSummary() {
  return useQuery({
    queryKey: ['admin', 'dashboard', 'summary'],
    queryFn: fetchDashboardSummary,
    staleTime: 5 * 60 * 1000, // 5분 — BE 캐시 TTL 과 동기화
    refetchInterval: 5 * 60 * 1000,
  });
}

'use client';

import { useQuery } from '@tanstack/react-query';
import { getUsageSummary } from '@/entities/usage';

export function useUsageSummary() {
  return useQuery({
    queryKey: ['usage', 'summary'],
    queryFn: getUsageSummary,
  });
}

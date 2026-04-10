'use client';

import { useQuery } from '@tanstack/react-query';
import { getUsageHistory } from '@/entities/usage';

export function useUsageHistory() {
  return useQuery({
    queryKey: ['usage', 'history'],
    queryFn: getUsageHistory,
  });
}

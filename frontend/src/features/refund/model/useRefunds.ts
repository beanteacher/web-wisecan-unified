'use client';

import { useQuery } from '@tanstack/react-query';
import { getRefunds } from '@/entities/refund';

export function useRefunds() {
  return useQuery({
    queryKey: ['billing', 'refunds'],
    queryFn: getRefunds,
  });
}

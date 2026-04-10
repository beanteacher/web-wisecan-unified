'use client';

import { useQuery } from '@tanstack/react-query';
import { listApiKeys } from '@/entities/api-key';

export function useApiKeys() {
  return useQuery({
    queryKey: ['api-keys'],
    queryFn: listApiKeys,
  });
}

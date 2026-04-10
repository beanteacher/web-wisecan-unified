'use client';

import { useQuery } from '@tanstack/react-query';
import { searchMessages } from '@/entities/message';
import type { MessageSearchParams } from '@/entities/message';

export function useSearchMessages(params: MessageSearchParams) {
  return useQuery({
    queryKey: ['messages-search', params],
    queryFn: () => searchMessages(params),
  });
}

'use client';

import { useQuery } from '@tanstack/react-query';
import { getMessageResult } from '@/entities/message';

export function useMessageResult(msgId: string | null) {
  return useQuery({
    queryKey: ['message-result', msgId],
    queryFn: () => getMessageResult(msgId!),
    enabled: !!msgId,
  });
}

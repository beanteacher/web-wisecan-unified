'use client';

import { useQuery } from '@tanstack/react-query';
import { fetchSendHistoryDetail } from '@/entities/send-history';

/**
 * 발송 이력 상세 쿼리 훅 (W-304).
 *
 * sendId가 없으면 쿼리를 비활성화한다.
 */
export function useSendHistoryDetail(sendId: string | null, scopeMember = false) {
  return useQuery({
    queryKey: ['send-history-detail', sendId, scopeMember],
    queryFn: () => fetchSendHistoryDetail(sendId!, scopeMember),
    enabled: !!sendId,
  });
}

'use client';

import { useQuery } from '@tanstack/react-query';
import { fetchSendHistoryList } from '@/entities/send-history';
import type { SendHistoryListParams } from '@/entities/send-history';

/**
 * 발송 이력 목록 쿼리 훅 (W-304).
 *
 * queryKey에 params 전체를 포함하여 필터·페이지 변경 시 자동 리페치.
 */
export function useSendHistoryList(params: SendHistoryListParams) {
  return useQuery({
    queryKey: ['send-history-list', params],
    queryFn: () => fetchSendHistoryList(params),
    placeholderData: (prev) => prev,
  });
}

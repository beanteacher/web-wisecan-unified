'use client';

import { useQuery } from '@tanstack/react-query';
import { getApiKeyScopeCatalog } from '@/entities/api-key';

export function useScopeCatalog() {
  return useQuery({
    queryKey: ['api-keys', 'scope-catalog'],
    queryFn: getApiKeyScopeCatalog,
    staleTime: 10 * 60 * 1000, // 카탈로그는 거의 변하지 않으므로 10분 캐시
  });
}

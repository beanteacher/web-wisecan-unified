import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ApiKeyList } from './ApiKeyList';
import * as useApiKeysModule from '../model/useApiKeys';
import type { ApiKey } from '@/entities/api-key';

function wrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

const mockKeys: ApiKey[] = [
  {
    id: 1,
    keyName: '테스트 키',
    keyPrefix: 'wsc_live_abcd',
    status: 'ACTIVE',
    lastUsedAt: '2026-04-01T00:00:00Z',
    createdAt: '2026-03-01T00:00:00Z',
  },
  {
    id: 2,
    keyName: '구 키',
    keyPrefix: 'wsc_live_efgh',
    status: 'REVOKED',
    lastUsedAt: null,
    createdAt: '2026-02-01T00:00:00Z',
  },
];

describe('ApiKeyList', () => {
  it('로딩 중 텍스트를 표시한다', () => {
    vi.spyOn(useApiKeysModule, 'useApiKeys').mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useApiKeysModule.useApiKeys>);

    render(<ApiKeyList />, { wrapper });
    expect(screen.getByText('불러오는 중...')).toBeInTheDocument();
  });

  it('데이터가 없으면 빈 상태 메시지를 표시한다', () => {
    vi.spyOn(useApiKeysModule, 'useApiKeys').mockReturnValue({
      data: [] as ApiKey[],
      isLoading: false,
    } as unknown as ReturnType<typeof useApiKeysModule.useApiKeys>);

    render(<ApiKeyList />, { wrapper });
    expect(screen.getByText('발급된 API 키가 없습니다.')).toBeInTheDocument();
  });

  it('API 키 목록을 테이블로 렌더링한다', () => {
    vi.spyOn(useApiKeysModule, 'useApiKeys').mockReturnValue({
      data: mockKeys,
      isLoading: false,
    } as unknown as ReturnType<typeof useApiKeysModule.useApiKeys>);

    render(<ApiKeyList />, { wrapper });
    expect(screen.getByText('테스트 키')).toBeInTheDocument();
    expect(screen.getByText('구 키')).toBeInTheDocument();
    expect(screen.getByText('활성')).toBeInTheDocument();
    expect(screen.getByText('비활성')).toBeInTheDocument();
  });
});

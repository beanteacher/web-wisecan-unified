import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MessageSearchPanel } from './MessageSearchPanel';
import * as useSearchMessagesModule from '../model/useSearchMessages';
import type { MessageSearchItem } from '@/entities/message';

function wrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

const mockItems: MessageSearchItem[] = [
  {
    messageId: 'msg-001',
    channel: 'EMAIL',
    recipient: 'test@example.com',
    status: 'SUCCESS',
    responseTimeMs: 123,
    sentAt: '2026-04-01T10:00:00Z',
  },
];

describe('MessageSearchPanel', () => {
  it('검색 결과가 없을 때 빈 상태 메시지를 표시한다', () => {
    vi.spyOn(useSearchMessagesModule, 'useSearchMessages').mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof useSearchMessagesModule.useSearchMessages>);

    render(<MessageSearchPanel />, { wrapper });
    expect(screen.getByText('검색 결과가 없습니다.')).toBeInTheDocument();
  });

  it('로딩 중 텍스트를 표시한다', () => {
    vi.spyOn(useSearchMessagesModule, 'useSearchMessages').mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useSearchMessagesModule.useSearchMessages>);

    render(<MessageSearchPanel />, { wrapper });
    expect(screen.getByText('검색 중...')).toBeInTheDocument();
  });

  it('검색 결과를 테이블로 렌더링한다', () => {
    vi.spyOn(useSearchMessagesModule, 'useSearchMessages').mockReturnValue({
      data: mockItems,
      isLoading: false,
    } as unknown as ReturnType<typeof useSearchMessagesModule.useSearchMessages>);

    render(<MessageSearchPanel />, { wrapper });
    expect(screen.getByText('msg-001')).toBeInTheDocument();
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
    // select option과 badge에 모두 "성공"이 존재하므로 getAllByText 사용
    expect(screen.getAllByText('성공').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('123ms')).toBeInTheDocument();
  });

  it('data가 undefined이면 결과 없음 메시지를 표시한다', () => {
    vi.spyOn(useSearchMessagesModule, 'useSearchMessages').mockReturnValue({
      data: undefined,
      isLoading: false,
    } as unknown as ReturnType<typeof useSearchMessagesModule.useSearchMessages>);

    render(<MessageSearchPanel />, { wrapper });
    expect(screen.getByText('검색 결과가 없습니다.')).toBeInTheDocument();
  });
});

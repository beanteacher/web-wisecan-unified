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
    channel: 'SMS',
    recipient: '01012345678',
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
    // PC와 Mobile 모두 렌더링되므로 getAllByText 사용
    expect(screen.getAllByText('검색 결과가 없습니다.').length).toBeGreaterThanOrEqual(1);
  });

  it('로딩 중 스켈레톤 UI를 표시한다', () => {
    vi.spyOn(useSearchMessagesModule, 'useSearchMessages').mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useSearchMessagesModule.useSearchMessages>);

    const { container } = render(<MessageSearchPanel />, { wrapper });
    expect(container.querySelector('.animate-pulse')).toBeInTheDocument();
  });

  it('검색 결과를 렌더링한다', () => {
    vi.spyOn(useSearchMessagesModule, 'useSearchMessages').mockReturnValue({
      data: mockItems,
      isLoading: false,
    } as unknown as ReturnType<typeof useSearchMessagesModule.useSearchMessages>);

    render(<MessageSearchPanel />, { wrapper });
    // msg-001 은 PC 테이블과 Mobile 카드 양쪽에 렌더링됨
    expect(screen.getAllByText('msg-001').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('01012345678').length).toBeGreaterThanOrEqual(1);
    // 성공 badge — PC + Mobile 각각 존재
    expect(screen.getAllByText('성공').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('123ms').length).toBeGreaterThanOrEqual(1);
  });

  it('data가 undefined이면 결과 없음 메시지를 표시한다', () => {
    vi.spyOn(useSearchMessagesModule, 'useSearchMessages').mockReturnValue({
      data: undefined,
      isLoading: false,
    } as unknown as ReturnType<typeof useSearchMessagesModule.useSearchMessages>);

    render(<MessageSearchPanel />, { wrapper });
    expect(screen.getAllByText('검색 결과가 없습니다.').length).toBeGreaterThanOrEqual(1);
  });
});

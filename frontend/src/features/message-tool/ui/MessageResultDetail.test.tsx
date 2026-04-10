import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MessageResultDetail } from './MessageResultDetail';
import * as useMessageResultModule from '../model/useMessageResult';
import type { MessageResult } from '@/entities/message';

function wrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

const mockResult: MessageResult = {
  messageId: 'msg-abc123',
  channel: 'SMS',
  recipient: '01012345678',
  content: '테스트 본문입니다.',
  status: 'SUCCESS',
  sentAt: '2026-04-10T10:00:00Z',
  deliveredAt: null,
};

describe('MessageResultDetail', () => {
  it('msgId가 없으면 안내 메시지를 표시한다', () => {
    render(<MessageResultDetail msgId={null} />, { wrapper });
    expect(
      screen.getByText('메시지 ID를 입력하거나 발송 후 결과를 확인하세요.'),
    ).toBeInTheDocument();
  });

  it('API 성공 시 결과 상세를 렌더링한다', () => {
    vi.spyOn(useMessageResultModule, 'useMessageResult').mockReturnValue({
      data: mockResult,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useMessageResultModule.useMessageResult>);

    render(<MessageResultDetail msgId="msg-abc123" />, { wrapper });

    // PC + Mobile 그리드 양쪽에 렌더링되므로 getAllByText 사용
    expect(screen.getAllByText('msg-abc123').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('성공').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('01012345678')).toBeInTheDocument();
    expect(screen.getByText('테스트 본문입니다.')).toBeInTheDocument();
  });

  it('API 실패 시 에러 메시지를 표시한다', () => {
    vi.spyOn(useMessageResultModule, 'useMessageResult').mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as unknown as ReturnType<typeof useMessageResultModule.useMessageResult>);

    render(<MessageResultDetail msgId="msg-invalid" />, { wrapper });

    expect(
      screen.getByText('결과를 불러오지 못했습니다. 메시지 ID를 확인하세요.'),
    ).toBeInTheDocument();
  });

  it('로딩 중에 스켈레톤을 표시한다', () => {
    vi.spyOn(useMessageResultModule, 'useMessageResult').mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    } as unknown as ReturnType<typeof useMessageResultModule.useMessageResult>);

    const { container } = render(<MessageResultDetail msgId="msg-abc123" />, { wrapper });

    expect(container.querySelector('.animate-pulse')).toBeInTheDocument();
  });
});

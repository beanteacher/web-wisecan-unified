import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SendMessageForm } from './SendMessageForm';
import * as useSendMessageModule from '../model/useSendMessage';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useSearchParams: () => ({ get: vi.fn() }),
}));

function wrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('SendMessageForm', () => {
  it('본문 미입력 시 유효성 에러를 표시한다', async () => {
    vi.spyOn(useSendMessageModule, 'useSendMessage').mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useSendMessageModule.useSendMessage>);

    render(<SendMessageForm />, { wrapper });

    // "발송" 버튼 클릭 (초기화 버튼 제외)
    const buttons = screen.getAllByRole('button');
    const sendButton = buttons.find((b) => b.textContent?.trim() === '발송');
    fireEvent.click(sendButton!);

    await waitFor(() => {
      expect(screen.getByText('수신자를 입력하세요')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('본문을 입력하세요')).toBeInTheDocument();
    });
  });

  it('발송 성공 시 mutateAsync를 호출한다', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue({ messageId: 'msg-001', status: 'SUCCESS', sentAt: '' });
    vi.spyOn(useSendMessageModule, 'useSendMessage').mockReturnValue({
      mutateAsync: mockMutateAsync,
      isPending: false,
    } as unknown as ReturnType<typeof useSendMessageModule.useSendMessage>);

    render(<SendMessageForm />, { wrapper });

    fireEvent.change(
      screen.getByPlaceholderText('예) user@example.com 또는 @slack-user'),
      { target: { value: 'test@example.com' } },
    );
    fireEvent.change(
      screen.getByPlaceholderText('발송할 메시지 내용을 입력하세요. 최대 2,000자까지 입력 가능합니다.'),
      { target: { value: '테스트 메시지 본문입니다.' } },
    );

    const buttons = screen.getAllByRole('button');
    const sendButton = buttons.find((b) => b.textContent?.trim() === '발송');
    fireEvent.click(sendButton!);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          channel: 'EMAIL',
          recipient: 'test@example.com',
          content: '테스트 메시지 본문입니다.',
        }),
      );
    });
  });

  it('isPending 상태에서 발송 버튼이 비활성화된다', () => {
    vi.spyOn(useSendMessageModule, 'useSendMessage').mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: true,
    } as unknown as ReturnType<typeof useSendMessageModule.useSendMessage>);

    render(<SendMessageForm />, { wrapper });

    const buttons = screen.getAllByRole('button');
    const sendButton = buttons.find((b) => b.textContent?.includes('발송'));
    expect(sendButton).toBeDisabled();
  });
});

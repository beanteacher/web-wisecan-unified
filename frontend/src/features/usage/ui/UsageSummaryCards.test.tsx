import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { UsageSummaryCards } from './UsageSummaryCards';
import * as useUsageSummaryModule from '../model/useUsageSummary';
import type { SummaryResponse } from '@/entities/usage';

function wrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('UsageSummaryCards', () => {
  it('로딩 중에는 스켈레톤 카드를 표시한다', () => {
    vi.spyOn(useUsageSummaryModule, 'useUsageSummary').mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useUsageSummaryModule.useUsageSummary>);

    const { container } = render(<UsageSummaryCards />, { wrapper });
    const skeletons = container.querySelectorAll('.animate-pulse');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('데이터를 카드에 렌더링한다', () => {
    const mockData: SummaryResponse = {
      totalCalls: 120,
      successCalls: 100,
      failCalls: 20,
    };

    vi.spyOn(useUsageSummaryModule, 'useUsageSummary').mockReturnValue({
      data: mockData,
      isLoading: false,
    } as unknown as ReturnType<typeof useUsageSummaryModule.useUsageSummary>);

    render(<UsageSummaryCards />, { wrapper });
    expect(screen.getByText('총 호출')).toBeInTheDocument();
    expect(screen.getByText('120')).toBeInTheDocument();
    expect(screen.getByText('성공')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('실패')).toBeInTheDocument();
    expect(screen.getByText('20')).toBeInTheDocument();
  });
});

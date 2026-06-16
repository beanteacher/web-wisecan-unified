'use client';

import { useState } from 'react';
import { trialSend, blockTrialBilling } from '../api/trialApi';
import { useTrialStore } from './trialStore';
import type { TrialSendRequest, TrialSendResponse, BillingBlockedResponse } from '../api/trialApi';

/**
 * 체험 발송·결제 차단 훅 (W-406).
 *
 * - 발송은 외부 송출 없이 가상 결과만 반환
 * - 결제는 즉시 차단 응답 반환
 */
export function useTrialSend() {
  const sessionToken = useTrialStore((s) => s.sessionToken);
  const [isPending, setIsPending] = useState(false);
  const [lastSendResult, setLastSendResult] = useState<TrialSendResponse | null>(null);
  const [lastBillingBlock, setLastBillingBlock] = useState<BillingBlockedResponse | null>(null);

  async function send(request: TrialSendRequest): Promise<TrialSendResponse> {
    if (!sessionToken) throw new Error('체험 세션이 없습니다.');
    setIsPending(true);
    try {
      const result = await trialSend(sessionToken, request);
      setLastSendResult(result);
      return result;
    } finally {
      setIsPending(false);
    }
  }

  async function attemptBilling(): Promise<BillingBlockedResponse> {
    if (!sessionToken) throw new Error('체험 세션이 없습니다.');
    const result = await blockTrialBilling(sessionToken);
    setLastBillingBlock(result);
    return result;
  }

  return { send, attemptBilling, isPending, lastSendResult, lastBillingBlock };
}

'use client';

import { useState } from 'react';
import { issueTrialSession, closeTrialSession } from '../api/trialApi';
import { useTrialStore } from './trialStore';

/**
 * 체험 세션 발급·종료 훅 (W-406).
 */
export function useTrialSession() {
  const { setTrial, clearTrial } = useTrialStore();
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function startTrial() {
    setIsPending(true);
    setError(null);
    try {
      const response = await issueTrialSession();
      setTrial(response.sessionToken, response.expiresAt, response.dummyContext);
      return response;
    } catch (e) {
      const message = e instanceof Error ? e.message : '체험 세션 발급에 실패했습니다.';
      setError(message);
      throw e;
    } finally {
      setIsPending(false);
    }
  }

  async function endTrial(sessionToken: string) {
    try {
      await closeTrialSession(sessionToken);
    } finally {
      clearTrial();
    }
  }

  return { startTrial, endTrial, isPending, error };
}

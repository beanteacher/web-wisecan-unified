import { create } from 'zustand';
import type { DummyContextSummary } from '../api/trialApi';

/**
 * 체험 모드 전용 격리 스토어 (W-406).
 *
 * - 운영 authStore 와 완전 분리 — 체험 세션 토큰은 이 스토어에만 존재
 * - 세션 종료/만료 시 clearTrial() 로 완전 초기화
 * - 체험 더미 컨텍스트(더미 발신번호·잔액·이력 등)도 여기에 보관
 */

interface TrialState {
  /** 체험 세션 토큰 (null이면 체험 모드 비활성) */
  sessionToken: string | null;
  /** 세션 만료 일시 */
  expiresAt: string | null;
  /** 더미 컨텍스트 (발신번호·API 키·잔액·이력 등) */
  dummyContext: DummyContextSummary | null;
  /** 체험 모드 활성 여부 */
  isTrialMode: boolean;

  setTrial: (sessionToken: string, expiresAt: string, dummyContext: DummyContextSummary) => void;
  clearTrial: () => void;
}

export const useTrialStore = create<TrialState>((set) => ({
  sessionToken: null,
  expiresAt: null,
  dummyContext: null,
  isTrialMode: false,

  setTrial: (sessionToken, expiresAt, dummyContext) =>
    set({ sessionToken, expiresAt, dummyContext, isTrialMode: true }),

  clearTrial: () =>
    set({ sessionToken: null, expiresAt: null, dummyContext: null, isTrialMode: false }),
}));

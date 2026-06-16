'use client';

import { useTrialStore } from '../model/trialStore';
import { useTrialSession } from '../model/useTrialSession';

/**
 * 체험 모드 워터마크 배너 (W-406).
 *
 * 체험 모드 활성 시 화면 상단에 고정 표시.
 * "체험 모드 — 실제 발송·결제는 일어나지 않습니다" 문구 상시 노출.
 */
export function TrialWatermark() {
  const { isTrialMode, sessionToken } = useTrialStore();
  const { endTrial } = useTrialSession();

  if (!isTrialMode) return null;

  function handleClose() {
    if (sessionToken) {
      endTrial(sessionToken);
    }
  }

  return (
    <div
      role="banner"
      aria-label="체험 모드 안내"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 9999,
        backgroundColor: '#f59e0b',
        color: '#1c1917',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '12px',
        padding: '10px 16px',
        fontSize: '13px',
        fontWeight: 600,
        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
      }}
    >
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <circle cx="8" cy="8" r="7" stroke="#92400e" strokeWidth="1.5" />
        <path d="M8 4.5v4" stroke="#92400e" strokeWidth="1.5" strokeLinecap="round" />
        <circle cx="8" cy="11" r="0.75" fill="#92400e" />
      </svg>
      <span>체험 모드 — 실제 발송·결제는 일어나지 않습니다</span>
      <button
        onClick={handleClose}
        aria-label="체험 모드 종료"
        style={{
          marginLeft: '8px',
          background: 'none',
          border: '1px solid #92400e',
          borderRadius: '4px',
          padding: '2px 8px',
          fontSize: '12px',
          fontWeight: 600,
          color: '#92400e',
          cursor: 'pointer',
        }}
      >
        종료
      </button>
    </div>
  );
}

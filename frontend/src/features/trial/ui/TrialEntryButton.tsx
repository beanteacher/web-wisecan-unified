'use client';

import { useRouter } from 'next/navigation';
import { useTrialSession } from '../model/useTrialSession';

/**
 * 체험하기 진입 버튼 (W-406).
 *
 * 메인 랜딩 페이지(/)에서 "체험하기" 클릭 시 체험 세션을 발급하고
 * 체험 대시보드로 이동한다.
 */
export function TrialEntryButton() {
  const { startTrial, isPending, error } = useTrialSession();
  const router = useRouter();

  async function handleClick() {
    try {
      await startTrial();
      router.push('/trial/dashboard');
    } catch {
      // error state는 useTrialSession 내부에서 관리됨
    }
  }

  return (
    <div>
      <button
        onClick={handleClick}
        disabled={isPending}
        aria-busy={isPending}
        style={{
          padding: '12px 28px',
          fontSize: '15px',
          fontWeight: 700,
          borderRadius: '8px',
          border: 'none',
          backgroundColor: isPending ? '#94a3b8' : '#2563eb',
          color: '#ffffff',
          cursor: isPending ? 'not-allowed' : 'pointer',
          transition: 'background-color 0.15s',
        }}
      >
        {isPending ? '세션 준비 중...' : '체험하기'}
      </button>
      {error && (
        <p
          role="alert"
          style={{ marginTop: '8px', fontSize: '13px', color: '#dc2626' }}
        >
          {error}
        </p>
      )}
    </div>
  );
}

'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useTrialStore } from '@/features/trial/model/trialStore';
import { useTrialSend } from '@/features/trial/model/useTrialSend';
import { TrialWatermark } from '@/features/trial/ui/TrialWatermark';

/**
 * 체험 모드 대시보드 (W-406).
 *
 * 더미 발신번호·API 키·잔액·발송 이력·카카오 템플릿·RCS 브랜드를
 * 회원과 동일한 UI로 표시한다.
 * 워터마크 "체험 모드 — 실제 발송·결제는 일어나지 않습니다" 상시 노출.
 */
export default function TrialDashboardPage() {
  const router = useRouter();
  const { isTrialMode, dummyContext, sessionToken } = useTrialStore();
  const { send, attemptBilling, isPending, lastSendResult, lastBillingBlock } = useTrialSend();

  // 체험 세션 없으면 메인으로 리다이렉트
  useEffect(() => {
    if (!isTrialMode) {
      router.replace('/');
    }
  }, [isTrialMode, router]);

  if (!isTrialMode || !dummyContext) return null;

  let sendHistory: Array<{ sendId: string; channel: string; status: string; sentAt: string }> = [];
  let kakaoTemplates: Array<{ templateCode: string; templateName: string; status: string }> = [];

  try {
    sendHistory = JSON.parse(dummyContext.dummySendHistoryJson ?? '[]');
    kakaoTemplates = JSON.parse(dummyContext.dummyKakaoTemplateJson ?? '[]');
  } catch {
    // JSON 파싱 실패 시 빈 배열 유지
  }

  async function handleTrialSend() {
    try {
      await send({
        channel: 'SMS',
        recipientNumber: '010-0000-0000',
        messageBody: '체험 발송 테스트 메시지입니다.',
      });
    } catch {
      // 오류는 훅 내부에서 관리
    }
  }

  async function handleTrialBilling() {
    try {
      await attemptBilling();
    } catch {
      // 오류는 훅 내부에서 관리
    }
  }

  return (
    <>
      <TrialWatermark />
      <div style={{ maxWidth: 900, margin: '0 auto', padding: '72px 24px 48px', fontFamily: 'Pretendard, sans-serif' }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 4 }}>체험 대시보드</h1>
        <p style={{ fontSize: 13, color: '#64748b', marginBottom: 32 }}>
          아래 데이터는 체험용 더미 데이터입니다. 실제 회원사 데이터와 무관합니다.
        </p>

        {/* 더미 계정 정보 */}
        <section style={cardStyle}>
          <h2 style={sectionTitle}>계정 정보 (더미)</h2>
          <dl style={dlStyle}>
            <dt style={dtStyle}>발신번호</dt>
            <dd style={ddStyle}>{dummyContext.dummyCallbackNumber}</dd>
            <dt style={dtStyle}>API 키</dt>
            <dd style={ddStyle}>{dummyContext.dummyApiKey}</dd>
            <dt style={dtStyle}>잔액</dt>
            <dd style={ddStyle}>{dummyContext.dummyBalance.toLocaleString()}원</dd>
          </dl>
        </section>

        {/* 발송 이력 */}
        <section style={cardStyle}>
          <h2 style={sectionTitle}>최근 발송 이력 (더미)</h2>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ background: '#f8fafc' }}>
                {['발송 ID', '채널', '상태', '발송 일시'].map((h) => (
                  <th key={h} style={thStyle}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {sendHistory.map((row) => (
                <tr key={row.sendId}>
                  <td style={tdStyle}>{row.sendId}</td>
                  <td style={tdStyle}>{row.channel}</td>
                  <td style={tdStyle}>
                    <span style={{
                      padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                      background: row.status === 'DELIVERED' ? '#dcfce7' : '#fee2e2',
                      color: row.status === 'DELIVERED' ? '#16a34a' : '#dc2626',
                    }}>
                      {row.status}
                    </span>
                  </td>
                  <td style={tdStyle}>{row.sentAt?.slice(0, 16).replace('T', ' ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* 카카오 알림톡 템플릿 */}
        <section style={cardStyle}>
          <h2 style={sectionTitle}>카카오 알림톡 템플릿 (더미)</h2>
          <ul style={{ padding: 0, margin: 0, listStyle: 'none' }}>
            {kakaoTemplates.map((t) => (
              <li key={t.templateCode} style={{ padding: '8px 0', borderBottom: '1px solid #f1f5f9', fontSize: 13 }}>
                <strong>{t.templateName}</strong>
                <span style={{ marginLeft: 12, fontSize: 11, color: '#64748b' }}>{t.templateCode}</span>
                <span style={{ marginLeft: 8, padding: '2px 6px', borderRadius: 4, fontSize: 11, background: '#dcfce7', color: '#16a34a' }}>
                  {t.status}
                </span>
              </li>
            ))}
          </ul>
        </section>

        {/* 체험 발송·결제 시도 버튼 */}
        <section style={cardStyle}>
          <h2 style={sectionTitle}>기능 체험</h2>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <button onClick={handleTrialSend} disabled={isPending} style={btnStyle('#2563eb')}>
              {isPending ? '처리 중...' : '발송 체험하기'}
            </button>
            <button onClick={handleTrialBilling} style={btnStyle('#7c3aed')}>
              충전 체험하기 (차단 확인)
            </button>
          </div>

          {lastSendResult && (
            <div style={{ marginTop: 16, padding: 12, background: '#f0fdf4', borderRadius: 8, fontSize: 13 }}>
              <strong>가상 발송 결과:</strong> {lastSendResult.virtualResultCode}
              <br />
              <span style={{ color: '#64748b' }}>{lastSendResult.message}</span>
            </div>
          )}

          {lastBillingBlock && (
            <div style={{ marginTop: 16, padding: 12, background: '#fef3c7', borderRadius: 8, fontSize: 13 }}>
              <strong>결제 차단:</strong> {lastBillingBlock.reason}
              <br />
              <span style={{ color: '#64748b' }}>{lastBillingBlock.message}</span>
            </div>
          )}
        </section>

        {/* 가입 전환 CTA */}
        <div style={{ textAlign: 'center', marginTop: 32 }}>
          <p style={{ fontSize: 13, color: '#64748b', marginBottom: 12 }}>
            실제 서비스를 이용하려면 회원가입이 필요합니다.
          </p>
          <a
            href="/register"
            style={{
              display: 'inline-block', padding: '12px 32px',
              background: '#0f172a', color: '#fff',
              borderRadius: 8, fontWeight: 700, fontSize: 14,
              textDecoration: 'none',
            }}
          >
            지금 가입하기
          </a>
        </div>
      </div>
    </>
  );
}

const cardStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #e8eaed',
  borderRadius: 12,
  padding: '20px 24px',
  marginBottom: 20,
};

const sectionTitle: React.CSSProperties = {
  fontSize: 14,
  fontWeight: 700,
  color: '#0f172a',
  marginBottom: 14,
  paddingBottom: 10,
  borderBottom: '1px solid #f1f5f9',
};

const dlStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '120px 1fr',
  gap: '8px 12px',
  fontSize: 13,
};

const dtStyle: React.CSSProperties = { color: '#64748b', fontWeight: 600 };
const ddStyle: React.CSSProperties = { color: '#1e2228', margin: 0 };

const thStyle: React.CSSProperties = {
  padding: '8px 12px', textAlign: 'left', fontWeight: 600,
  fontSize: 12, color: '#64748b', borderBottom: '1px solid #e8eaed',
};

const tdStyle: React.CSSProperties = {
  padding: '8px 12px', borderBottom: '1px solid #f1f5f9',
};

function btnStyle(bg: string): React.CSSProperties {
  return {
    padding: '10px 20px', fontSize: 13, fontWeight: 700,
    borderRadius: 8, border: 'none', background: bg,
    color: '#fff', cursor: 'pointer',
  };
}

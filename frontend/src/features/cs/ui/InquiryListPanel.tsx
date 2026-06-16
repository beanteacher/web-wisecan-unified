'use client';

import { useEffect, useState } from 'react';
import { MessageSquare, ChevronRight, Clock } from 'lucide-react';
import { csApi, type InquirySummary, type InquiryStatus } from '../api/csApi';

const STATUS_LABEL: Record<InquiryStatus, string> = {
  OPEN: '접수',
  IN_PROGRESS: '처리중',
  ANSWERED: '답변완료',
  CLOSED: '종료',
};

const STATUS_COLOR: Record<InquiryStatus, { bg: string; color: string }> = {
  OPEN: { bg: '#fef9c3', color: '#854d0e' },
  IN_PROGRESS: { bg: '#e0f2fe', color: '#0369a1' },
  ANSWERED: { bg: '#dcfce7', color: '#15803d' },
  CLOSED: { bg: '#f1f5f9', color: '#475569' },
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });
}

export function InquiryListPanel() {
  const [inquiries, setInquiries] = useState<InquirySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    csApi.myInquiries()
      .then((res) => setInquiries(res.content))
      .catch(() => setError('문의 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8', fontSize: '14px' }}>
        불러오는 중...
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#ef4444', fontSize: '14px' }}>
        {error}
      </div>
    );
  }

  if (inquiries.length === 0) {
    return (
      <div style={{ padding: '60px 24px', textAlign: 'center' }}>
        <MessageSquare size={40} style={{ color: '#cbd5e1', margin: '0 auto 12px' }} />
        <p style={{ fontSize: '15px', color: '#64748b' }}>접수된 문의가 없습니다.</p>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
      {inquiries.map((item) => {
        const statusStyle = STATUS_COLOR[item.status];
        return (
          <div
            key={item.id}
            style={{
              display: 'flex', alignItems: 'center', gap: '16px',
              padding: '16px 20px', borderBottom: '1px solid #f1f5f9',
              cursor: 'pointer', transition: 'background 0.1s',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.background = '#f8fafc')}
            onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
          >
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                <span style={{
                  display: 'inline-flex', alignItems: 'center',
                  padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: 600,
                  background: statusStyle.bg, color: statusStyle.color,
                }}>
                  {STATUS_LABEL[item.status]}
                </span>
                <p style={{
                  fontSize: '14px', fontWeight: 500, color: '#0f172a',
                  whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                }}>
                  {item.title}
                </p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '12px', color: '#94a3b8' }}>
                <span>{formatDate(item.createdAt)}</span>
                {item.answeredAt && (
                  <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Clock size={11} />
                    {item.answerMinutes < 60
                      ? `${item.answerMinutes}분 내 답변`
                      : `${Math.floor(item.answerMinutes / 60)}시간 내 답변`}
                  </span>
                )}
              </div>
            </div>
            <ChevronRight size={16} style={{ color: '#cbd5e1', flexShrink: 0 }} />
          </div>
        );
      })}
    </div>
  );
}

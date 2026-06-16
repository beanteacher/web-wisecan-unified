'use client';

import { useEffect, useState } from 'react';
import { Bell, Pin, ChevronDown, ChevronUp } from 'lucide-react';
import { csApi, type NoticeSummary, type NoticeDetail, type NoticeType } from '../api/csApi';

const TYPE_LABEL: Record<NoticeType, string> = {
  GENERAL: '일반',
  MAINTENANCE: '점검',
  IMPORTANT: '중요',
};

const TYPE_COLOR: Record<NoticeType, { bg: string; color: string }> = {
  GENERAL: { bg: '#f1f5f9', color: '#475569' },
  MAINTENANCE: { bg: '#fef9c3', color: '#854d0e' },
  IMPORTANT: { bg: '#fee2e2', color: '#991b1b' },
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
  });
}

export function NoticePanel() {
  const [notices, setNotices] = useState<NoticeSummary[]>([]);
  const [openId, setOpenId] = useState<number | null>(null);
  const [detail, setDetail] = useState<NoticeDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    csApi.listNotices()
      .then(setNotices)
      .finally(() => setLoading(false));
  }, []);

  const toggleDetail = async (id: number) => {
    if (openId === id) {
      setOpenId(null);
      setDetail(null);
      return;
    }
    setOpenId(id);
    setDetail(null);
    setDetailLoading(true);
    try {
      const d = await csApi.noticeDetail(id);
      setDetail(d);
    } finally {
      setDetailLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8', fontSize: '14px' }}>
        불러오는 중...
      </div>
    );
  }

  if (notices.length === 0) {
    return (
      <div style={{ padding: '60px 24px', textAlign: 'center' }}>
        <Bell size={40} style={{ color: '#cbd5e1', margin: '0 auto 12px' }} />
        <p style={{ fontSize: '15px', color: '#64748b' }}>등록된 공지사항이 없습니다.</p>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
      {notices.map((notice) => {
        const isOpen = openId === notice.id;
        const typeStyle = TYPE_COLOR[notice.type];
        return (
          <div key={notice.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
            <button
              onClick={() => toggleDetail(notice.id)}
              style={{
                width: '100%', display: 'flex', alignItems: 'center',
                padding: '16px 4px', background: 'transparent', border: 'none',
                cursor: 'pointer', textAlign: 'left', gap: '12px',
              }}
            >
              {/* 유형 뱃지 */}
              <span style={{
                display: 'inline-flex', alignItems: 'center', flexShrink: 0,
                padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: 600,
                background: typeStyle.bg, color: typeStyle.color,
              }}>
                {TYPE_LABEL[notice.type]}
              </span>

              {/* 핀 아이콘 */}
              {notice.pinned && <Pin size={13} style={{ color: '#f59e0b', flexShrink: 0 }} />}

              {/* 제목 */}
              <span style={{
                flex: 1, fontSize: '14px', fontWeight: notice.pinned ? 600 : 400,
                color: '#0f172a', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
              }}>
                {notice.title}
              </span>

              {/* 날짜 */}
              <span style={{ fontSize: '12px', color: '#94a3b8', flexShrink: 0 }}>
                {formatDate(notice.createdAt)}
              </span>

              {isOpen
                ? <ChevronUp size={16} style={{ color: '#94a3b8', flexShrink: 0 }} />
                : <ChevronDown size={16} style={{ color: '#94a3b8', flexShrink: 0 }} />}
            </button>

            {isOpen && (
              <div style={{
                padding: '0 4px 20px 16px',
                borderLeft: '3px solid #2563eb',
                marginLeft: '4px',
              }}>
                {detailLoading ? (
                  <p style={{ fontSize: '14px', color: '#94a3b8' }}>불러오는 중...</p>
                ) : detail ? (
                  <p style={{ fontSize: '14px', color: '#374151', lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>
                    {detail.content}
                  </p>
                ) : null}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

'use client';

import { useState } from 'react';
import { MessageSquare, HelpCircle, Bell, PenLine } from 'lucide-react';
import { InquiryForm, InquiryListPanel, FaqPanel, NoticePanel, ChatbotWidget } from '@/features/cs';

type Tab = 'inquiry' | 'new-inquiry' | 'faq' | 'notice';

const TABS: { id: Tab; label: string; icon: React.ReactNode }[] = [
  { id: 'notice', label: '공지사항', icon: <Bell size={15} /> },
  { id: 'faq', label: '자주 묻는 질문', icon: <HelpCircle size={15} /> },
  { id: 'inquiry', label: '내 문의', icon: <MessageSquare size={15} /> },
  { id: 'new-inquiry', label: '문의하기', icon: <PenLine size={15} /> },
];

export default function CsPage() {
  const [tab, setTab] = useState<Tab>('notice');

  return (
    <div style={{ maxWidth: '760px', margin: '0 auto', padding: '32px 20px 80px' }}>
      {/* 페이지 헤더 */}
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 700, color: '#0f172a', marginBottom: '6px' }}>
          고객센터
        </h1>
        <p style={{ fontSize: '14px', color: '#64748b' }}>
          공지사항, FAQ, 1:1 문의를 이용해 주세요. 영업일 기준 24시간 내 답변 드립니다.
        </p>
      </div>

      {/* 탭 */}
      <div style={{
        display: 'flex', gap: '0', borderBottom: '1px solid #e2e8f0', marginBottom: '28px',
      }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            style={{
              display: 'flex', alignItems: 'center', gap: '6px',
              padding: '10px 18px', border: 'none', background: 'transparent',
              fontSize: '14px', fontWeight: tab === t.id ? 600 : 400,
              color: tab === t.id ? '#2563eb' : '#64748b',
              borderBottom: tab === t.id ? '2px solid #2563eb' : '2px solid transparent',
              cursor: 'pointer', transition: 'all 0.15s',
              marginBottom: '-1px',
            }}
          >
            {t.icon}
            {t.label}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      <div style={{
        background: '#fff', border: '1px solid #e8eaed',
        borderRadius: '12px', padding: '24px 28px',
      }}>
        {tab === 'notice' && <NoticePanel />}
        {tab === 'faq' && <FaqPanel />}
        {tab === 'inquiry' && <InquiryListPanel />}
        {tab === 'new-inquiry' && (
          <InquiryForm onSuccess={() => setTab('inquiry')} />
        )}
      </div>

      {/* 챗봇 플로팅 위젯 */}
      <ChatbotWidget />
    </div>
  );
}

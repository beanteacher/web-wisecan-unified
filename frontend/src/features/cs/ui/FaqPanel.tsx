'use client';

import { useEffect, useState } from 'react';
import { ChevronDown, ChevronUp, HelpCircle } from 'lucide-react';
import { csApi, type FaqItem, type InquiryCategory } from '../api/csApi';

const CATEGORIES: { value: InquiryCategory | ''; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'ACCOUNT', label: '계정 / 인증' },
  { value: 'SEND', label: '발송 / API' },
  { value: 'BILLING', label: '결제 / 충전' },
  { value: 'TECHNICAL', label: '기술 / 장애' },
  { value: 'ETC', label: '기타' },
];

export function FaqPanel() {
  const [faqs, setFaqs] = useState<FaqItem[]>([]);
  const [category, setCategory] = useState<InquiryCategory | ''>('');
  const [openId, setOpenId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    csApi.listFaqs(category || undefined)
      .then(setFaqs)
      .finally(() => setLoading(false));
  }, [category]);

  return (
    <div>
      {/* 카테고리 탭 */}
      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '24px' }}>
        {CATEGORIES.map((c) => (
          <button
            key={c.value}
            onClick={() => { setCategory(c.value); setOpenId(null); }}
            style={{
              padding: '6px 14px', borderRadius: '20px', fontSize: '13px', fontWeight: 500,
              border: '1px solid',
              borderColor: category === c.value ? '#2563eb' : '#e2e8f0',
              background: category === c.value ? '#eff6ff' : '#fff',
              color: category === c.value ? '#2563eb' : '#64748b',
              cursor: 'pointer', transition: 'all 0.15s',
            }}
          >
            {c.label}
          </button>
        ))}
      </div>

      {/* FAQ 아코디언 */}
      {loading ? (
        <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8', fontSize: '14px' }}>불러오는 중...</div>
      ) : faqs.length === 0 ? (
        <div style={{ padding: '60px 24px', textAlign: 'center' }}>
          <HelpCircle size={40} style={{ color: '#cbd5e1', margin: '0 auto 12px' }} />
          <p style={{ fontSize: '15px', color: '#64748b' }}>해당 카테고리의 FAQ가 없습니다.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
          {faqs.map((faq) => {
            const isOpen = openId === faq.id;
            return (
              <div key={faq.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <button
                  onClick={() => setOpenId(isOpen ? null : faq.id)}
                  style={{
                    width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    padding: '16px 4px', background: 'transparent', border: 'none',
                    cursor: 'pointer', textAlign: 'left', gap: '12px',
                  }}
                >
                  <span style={{ fontSize: '14px', fontWeight: 500, color: '#0f172a', flex: 1 }}>
                    Q. {faq.question}
                  </span>
                  {isOpen
                    ? <ChevronUp size={16} style={{ color: '#94a3b8', flexShrink: 0 }} />
                    : <ChevronDown size={16} style={{ color: '#94a3b8', flexShrink: 0 }} />}
                </button>
                {isOpen && (
                  <div style={{
                    padding: '0 4px 16px 16px',
                    borderLeft: '3px solid #2563eb',
                    marginLeft: '4px',
                  }}>
                    <p style={{ fontSize: '14px', color: '#374151', lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                      {faq.answer}
                    </p>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

'use client';

import { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, MessageCircle, X } from 'lucide-react';
import { csApi, type ChatbotResponse } from '../api/csApi';

interface Message {
  id: number;
  role: 'bot' | 'user';
  text: string;
  fallback?: boolean;
}

const INITIAL_MESSAGE: Message = {
  id: 0,
  role: 'bot',
  text: '안녕하세요! Wisecan 고객센터 챗봇입니다. 궁금하신 점을 입력해 주세요.',
};

export function ChatbotWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([INITIAL_MESSAGE]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const nextId = useRef(1);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async () => {
    const question = input.trim();
    if (!question || loading) return;

    const userMsg: Message = { id: nextId.current++, role: 'user', text: question };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res: ChatbotResponse = await csApi.chatbotQuery(question);
      const botMsg: Message = {
        id: nextId.current++,
        role: 'bot',
        text: res.matched ? (res.answer ?? '') : (res.fallbackMessage ?? '답변을 찾지 못했습니다.'),
        fallback: !res.matched,
      };
      setMessages((prev) => [...prev, botMsg]);
    } catch {
      setMessages((prev) => [
        ...prev,
        { id: nextId.current++, role: 'bot', text: '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', fallback: true },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* 플로팅 버튼 */}
      <button
        onClick={() => setOpen((v) => !v)}
        aria-label="챗봇 열기"
        style={{
          position: 'fixed', bottom: '28px', right: '28px', zIndex: 50,
          width: '52px', height: '52px', borderRadius: '50%',
          background: '#2563eb', border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 4px 16px rgba(37,99,235,0.35)',
          transition: 'transform 0.15s',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.transform = 'scale(1.08)')}
        onMouseLeave={(e) => (e.currentTarget.style.transform = 'scale(1)')}
      >
        {open ? <X size={22} color="#fff" /> : <MessageCircle size={22} color="#fff" />}
      </button>

      {/* 채팅 패널 */}
      {open && (
        <div style={{
          position: 'fixed', bottom: '92px', right: '28px', zIndex: 50,
          width: '340px', maxHeight: '480px',
          background: '#fff', borderRadius: '16px',
          boxShadow: '0 8px 32px rgba(0,0,0,0.14)',
          display: 'flex', flexDirection: 'column',
          overflow: 'hidden',
        }}>
          {/* 헤더 */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: '10px',
            padding: '14px 18px', background: '#2563eb',
          }}>
            <Bot size={20} color="#fff" />
            <span style={{ fontSize: '14px', fontWeight: 700, color: '#fff' }}>Wisecan 챗봇</span>
            <span style={{
              marginLeft: 'auto', fontSize: '11px', color: '#bfdbfe',
              background: 'rgba(255,255,255,0.15)', padding: '2px 8px', borderRadius: '10px',
            }}>FAQ 기반</span>
          </div>

          {/* 메시지 영역 */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {messages.map((msg) => (
              <div key={msg.id} style={{
                display: 'flex', gap: '8px',
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                alignItems: 'flex-end',
              }}>
                <div style={{
                  width: '28px', height: '28px', borderRadius: '50%', flexShrink: 0,
                  background: msg.role === 'bot' ? '#eff6ff' : '#f1f5f9',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  {msg.role === 'bot'
                    ? <Bot size={14} style={{ color: '#2563eb' }} />
                    : <User size={14} style={{ color: '#64748b' }} />}
                </div>
                <div style={{
                  maxWidth: '220px', padding: '10px 13px', borderRadius: '12px',
                  fontSize: '13px', lineHeight: 1.6, whiteSpace: 'pre-wrap',
                  background: msg.role === 'user' ? '#2563eb' : (msg.fallback ? '#fff7ed' : '#f8fafc'),
                  color: msg.role === 'user' ? '#fff' : (msg.fallback ? '#92400e' : '#1e293b'),
                  border: msg.fallback ? '1px solid #fed7aa' : 'none',
                }}>
                  {msg.text}
                  {msg.fallback && (
                    <p style={{ fontSize: '11px', color: '#c2410c', marginTop: '6px' }}>
                      1:1 문의를 이용해 주세요.
                    </p>
                  )}
                </div>
              </div>
            ))}
            {loading && (
              <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
                <div style={{
                  width: '28px', height: '28px', borderRadius: '50%',
                  background: '#eff6ff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <Bot size={14} style={{ color: '#2563eb' }} />
                </div>
                <div style={{
                  padding: '10px 13px', borderRadius: '12px', background: '#f8fafc',
                  fontSize: '13px', color: '#94a3b8',
                }}>
                  답변을 찾는 중...
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          {/* 입력 영역 */}
          <div style={{
            display: 'flex', gap: '8px', padding: '12px 14px',
            borderTop: '1px solid #f1f5f9',
          }}>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
              placeholder="질문을 입력하세요"
              disabled={loading}
              style={{
                flex: 1, height: '36px', padding: '0 12px', borderRadius: '8px',
                border: '1px solid #e2e8f0', fontSize: '13px', outline: 'none',
                background: '#fff', color: '#0f172a',
              }}
            />
            <button
              onClick={send}
              disabled={loading || !input.trim()}
              style={{
                width: '36px', height: '36px', borderRadius: '8px',
                background: loading || !input.trim() ? '#e2e8f0' : '#2563eb',
                border: 'none', cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                transition: 'background 0.15s',
              }}
            >
              <Send size={15} color={loading || !input.trim() ? '#94a3b8' : '#fff'} />
            </button>
          </div>
        </div>
      )}
    </>
  );
}

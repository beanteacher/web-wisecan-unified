'use client';

import { useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { SendMessageForm } from '@/features/message-tool';
import { MessageSearchPanel } from '@/features/message-tool';

type Tab = 'send' | 'history';

const TABS: { key: Tab; label: string }[] = [
  { key: 'send', label: '메시지 발송' },
  { key: 'history', label: '발송 이력' },
];

export default function MessageToolsPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const initialTab = (searchParams.get('tab') as Tab) ?? 'send';
  const [tab, setTab] = useState<Tab>(initialTab);

  const handleTabChange = (next: Tab) => {
    setTab(next);
    router.replace(`/dashboard/message-tools?tab=${next}`);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">메시지 도구</h1>
        <p className="mt-1 text-sm text-slate-500">
          MCP 채널을 통해 메시지를 발송하고 결과를 확인하세요
        </p>
      </div>

      {/* 탭 */}
      <div className="border-b border-slate-200">
        <nav className="-mb-px flex gap-6">
          {TABS.map(({ key, label }) => (
            <button
              key={key}
              onClick={() => handleTabChange(key)}
              className={[
                'border-b-2 pb-3 text-sm font-medium transition-colors',
                tab === key
                  ? 'border-slate-900 text-slate-900'
                  : 'border-transparent text-slate-500 hover:text-slate-700',
              ].join(' ')}
            >
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* 탭 컨텐츠 */}
      {tab === 'send' && <SendMessageForm />}
      {tab === 'history' && <MessageSearchPanel />}
    </div>
  );
}

'use client';

import { useState } from 'react';
import {
  KakaoTemplatePanel,
  RcsTemplatePanel,
  TemplateTransferPanel,
} from '@/features/template';

const TABS = [
  { key: 'kakao',    label: '카카오 알림톡' },
  { key: 'rcs',      label: 'RCS' },
  { key: 'transfer', label: 'SMS17 이관' },
] as const;

type Tab = (typeof TABS)[number]['key'];

export default function TemplatesPage() {
  const [activeTab, setActiveTab] = useState<Tab>('kakao');
  const [showKakaoRegister, setShowKakaoRegister] = useState(false);

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900">템플릿 관리</h1>
        <p className="mt-1 text-sm text-slate-500">
          카카오 알림톡·친구톡, RCS 템플릿을 조회하고 SMS17 이관 신청을 관리합니다.
        </p>
      </div>

      {/* 탭 바 */}
      <div className="flex gap-1 border-b border-slate-200">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px ${
              activeTab === tab.key
                ? 'border-black text-slate-900'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      <div className="rounded-xl border border-slate-200 bg-white p-6">
        {activeTab === 'kakao' && (
          <KakaoTemplatePanel onRegisterClick={() => setShowKakaoRegister(true)} />
        )}
        {activeTab === 'rcs' && <RcsTemplatePanel />}
        {activeTab === 'transfer' && <TemplateTransferPanel />}
      </div>

      {/* 카카오 템플릿 등록 모달 (TODO: 등록 폼 구현 후 교체) */}
      {showKakaoRegister && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-semibold">카카오 템플릿 등록</h2>
              <button
                onClick={() => setShowKakaoRegister(false)}
                className="text-sm text-slate-400 hover:text-slate-600"
              >
                닫기 ×
              </button>
            </div>
            <p className="text-sm text-slate-500">
              템플릿 등록 기능은 카카오 비즈니스 채널 연동 이후 활성화됩니다.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

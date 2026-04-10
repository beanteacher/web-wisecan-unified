import { Zap, MessageSquare, Activity, FileText, Shield } from 'lucide-react';

interface FeatureItem {
  icon: React.ReactNode;
  text: string;
}

interface AuthBrandPanelProps {
  variant?: 'login' | 'register';
}

const loginFeatures: FeatureItem[] = [
  { icon: <MessageSquare size={18} />, text: 'MCP 메시지 발송 도구' },
  { icon: <Activity size={18} />, text: '에이전트 상태 진단' },
  { icon: <FileText size={18} />, text: '파일 형식 변환' },
];

const registerFeatures: FeatureItem[] = [
  { icon: <Shield size={18} />, text: '엔터프라이즈급 보안' },
  { icon: <Zap size={18} />, text: '즉시 사용 가능한 API' },
  { icon: <MessageSquare size={18} />, text: '전담 기술 지원' },
];

export function AuthBrandPanel({ variant = 'login' }: AuthBrandPanelProps) {
  const isLogin = variant === 'login';
  const features = isLogin ? loginFeatures : registerFeatures;
  const headline = isLogin
    ? 'MCP 도구로 연결하는\n새로운 비즈니스 방식'
    : '지금 바로 시작하세요';
  const subtext = isLogin
    ? '메시지 발송부터 파일 변환까지 — API 하나로 모든 MCP 도구를 연결하세요.'
    : '무료로 가입하고 Wisecan MCP 도구의 모든 기능을 체험하세요.';

  return (
    <div
      className="hidden lg:flex lg:flex-col lg:justify-between min-h-screen p-12 relative overflow-hidden"
      style={{
        background: 'linear-gradient(135deg, #2563EB 0%, #1D4ED8 60%, #1E40AF 100%)',
      }}
    >
      {/* 배경 장식 원 */}
      <div
        className="absolute -top-24 -right-24 w-96 h-96 rounded-full opacity-10"
        style={{ background: 'radial-gradient(circle, #FFFFFF 0%, transparent 70%)' }}
        aria-hidden="true"
      />
      <div
        className="absolute -bottom-32 -left-16 w-80 h-80 rounded-full opacity-10"
        style={{ background: 'radial-gradient(circle, #FFFFFF 0%, transparent 70%)' }}
        aria-hidden="true"
      />

      {/* 상단 로고 */}
      <div className="relative z-10 flex items-center gap-2">
        <Zap size={24} className="text-white" />
        <span
          className="text-white font-bold"
          style={{ fontSize: '24px', fontFamily: 'var(--font-pretendard)' }}
        >
          Wisecan
        </span>
      </div>

      {/* 중앙 히어로 콘텐츠 */}
      <div className="relative z-10 flex flex-col gap-6">
        <h1
          className="text-white font-extrabold leading-tight whitespace-pre-line"
          style={{
            fontSize: '36px',
            lineHeight: '44px',
            letterSpacing: '-0.02em',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          {headline}
        </h1>
        <p
          className="leading-relaxed"
          style={{
            fontSize: '16px',
            lineHeight: '24px',
            color: 'rgba(255,255,255,0.80)',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          {subtext}
        </p>

        {/* 기능 목록 */}
        <ul className="flex flex-col gap-3 mt-2">
          {features.map((item, idx) => (
            <li key={idx} className="flex items-center gap-3">
              <span style={{ color: 'rgba(255,255,255,0.70)' }}>{item.icon}</span>
              <span
                style={{
                  fontSize: '15px',
                  color: 'rgba(255,255,255,0.90)',
                  fontFamily: 'var(--font-pretendard)',
                }}
              >
                {item.text}
              </span>
            </li>
          ))}
        </ul>
      </div>

      {/* 하단 태그라인 */}
      <div className="relative z-10">
        <p
          style={{
            fontSize: '13px',
            color: 'rgba(255,255,255,0.50)',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          © 2026 Wisecan. All rights reserved.
        </p>
      </div>
    </div>
  );
}

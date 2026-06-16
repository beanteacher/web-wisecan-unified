'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { z } from 'zod';
import { businessApi } from '../api/businessApi';

const businessSchema = z.object({
  bizNumber: z
    .string()
    .min(1, '사업자번호를 입력해주세요')
    .regex(/^\d{3}-\d{2}-\d{5}$/, '올바른 형식으로 입력해주세요 (예: 123-45-67890)'),
  corpNumber: z.string().optional(),
  companyName: z.string().min(1, '회사명을 입력해주세요').max(200, '회사명은 200자 이하입니다'),
  ceoName: z.string().min(1, '대표자명을 입력해주세요').max(100, '대표자명은 100자 이하입니다'),
  ceoPhone: z.string().optional(),
});

type BusinessFormValues = z.infer<typeof businessSchema>;

const AlertCircleIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10" />
    <line x1="12" y1="8" x2="12" y2="12" />
    <line x1="12" y1="16" x2="12.01" y2="16" />
  </svg>
);

const BuildingIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <path d="M9 3v18M15 3v18M3 9h18M3 15h18" />
  </svg>
);

export function BusinessRegisterForm() {
  const router = useRouter();
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<BusinessFormValues>({
    resolver: zodResolver(businessSchema),
  });

  const onSubmit = async (values: BusinessFormValues) => {
    setServerError(null);
    try {
      await businessApi.submit({
        bizNumber: values.bizNumber,
        corpNumber: values.corpNumber || undefined,
        companyName: values.companyName,
        ceoName: values.ceoName,
        ceoPhone: values.ceoPhone || undefined,
      });
      setSubmitted(true);
    } catch {
      setServerError('사업자 전환 신청에 실패했습니다. 로그인 후 다시 시도해주세요.');
    }
  };

  const inputStyle = (hasError: boolean): React.CSSProperties => ({
    height: '40px',
    border: hasError ? '1px solid #EF4444' : '1px solid #E2E8F0',
    borderRadius: '8px',
    background: '#FFFFFF',
    color: '#0F172A',
    fontFamily: 'var(--font-pretendard)',
    boxShadow: hasError ? '0 0 0 2px rgba(252,165,165,0.4)' : 'none',
    width: '100%',
    padding: '0 12px',
    fontSize: '14px',
    outline: 'none',
  });

  const handleFocus = (e: React.FocusEvent<HTMLInputElement>, hasError: boolean) => {
    if (!hasError) {
      e.target.style.border = '1px solid transparent';
      e.target.style.boxShadow = '0 0 0 2px #2563EB';
    }
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>, hasError: boolean) => {
    if (!hasError) {
      e.target.style.border = '1px solid #E2E8F0';
      e.target.style.boxShadow = 'none';
    }
  };

  const labelStyle: React.CSSProperties = {
    fontSize: '14px',
    fontWeight: 500,
    color: '#0F172A',
    fontFamily: 'var(--font-pretendard)',
  };

  const errorStyle: React.CSSProperties = {
    fontSize: '12px',
    color: '#DC2626',
    fontFamily: 'var(--font-pretendard)',
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
  };

  if (submitted) {
    return (
      <div className="w-full text-center" style={{ maxWidth: '400px' }}>
        <div style={{ marginBottom: '24px' }}>
          <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="#15803d" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ margin: '0 auto 16px' }}>
            <circle cx="12" cy="12" r="10" />
            <polyline points="9 12 11 14 15 10" />
          </svg>
          <h2 style={{ fontSize: '22px', fontWeight: 700, color: '#0F172A', fontFamily: 'var(--font-pretendard)', marginBottom: '8px' }}>
            신청이 완료되었습니다
          </h2>
          <p style={{ fontSize: '14px', color: '#64748B', fontFamily: 'var(--font-pretendard)', lineHeight: 1.7 }}>
            운영자 심사 후 결과를 이메일로 안내드립니다.<br />
            심사는 영업일 기준 1~3일 소요됩니다.
          </p>
        </div>
        <button
          onClick={() => router.push('/dashboard')}
          style={{
            height: '44px',
            background: '#2563EB',
            color: '#FFFFFF',
            borderRadius: '8px',
            fontSize: '15px',
            fontFamily: 'var(--font-pretendard)',
            border: 'none',
            cursor: 'pointer',
            width: '100%',
            fontWeight: 500,
          }}
        >
          대시보드로 이동
        </button>
      </div>
    );
  }

  return (
    <div className="w-full" style={{ maxWidth: '400px' }}>
      {/* 모바일 전용 로고 */}
      <div className="flex items-center gap-2 mb-8 lg:hidden">
        <BuildingIcon />
        <span className="font-bold" style={{ fontSize: '22px', color: '#0F172A', fontFamily: 'var(--font-pretendard)' }}>
          Wisecan
        </span>
      </div>

      {/* 폼 헤더 */}
      <div className="mb-6">
        <h1 className="font-bold" style={{ fontSize: '26px', lineHeight: '34px', letterSpacing: '-0.01em', color: '#0F172A', fontFamily: 'var(--font-pretendard)' }}>
          사업자 전환 신청
        </h1>
        <p className="mt-2" style={{ fontSize: '14px', color: '#64748B', fontFamily: 'var(--font-pretendard)', lineHeight: 1.6 }}>
          사업자 정보를 입력하면 운영자 심사 후 사업자 회원으로 전환됩니다.
        </p>
      </div>

      {/* 안내 박스 */}
      <div
        style={{ background: '#EFF6FF', border: '1px solid #BFDBFE', borderRadius: '8px', padding: '12px 16px', marginBottom: '20px' }}
      >
        <p style={{ fontSize: '13px', color: '#1D4ED8', fontFamily: 'var(--font-pretendard)', lineHeight: 1.6 }}>
          사업자 등록증과 대표자 신분증 파일은 심사 과정에서 별도 요청드립니다.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
        {/* 사업자번호 */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="bizNumber" style={labelStyle}>사업자번호 *</label>
          <input
            id="bizNumber"
            type="text"
            placeholder="예) 123-45-67890"
            {...register('bizNumber')}
            style={inputStyle(!!errors.bizNumber)}
            onFocus={(e) => handleFocus(e, !!errors.bizNumber)}
            onBlur={(e) => handleBlur(e, !!errors.bizNumber)}
          />
          {errors.bizNumber && <p style={errorStyle}><AlertCircleIcon />{errors.bizNumber.message}</p>}
        </div>

        {/* 법인번호 (선택) */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="corpNumber" style={labelStyle}>
            법인등록번호 <span style={{ color: '#94A3B8', fontWeight: 400 }}>(선택 · 법인 사업자만)</span>
          </label>
          <input
            id="corpNumber"
            type="text"
            placeholder="예) 110111-1234567"
            {...register('corpNumber')}
            style={inputStyle(false)}
            onFocus={(e) => handleFocus(e, false)}
            onBlur={(e) => handleBlur(e, false)}
          />
        </div>

        {/* 회사명 */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="companyName" style={labelStyle}>회사명(상호) *</label>
          <input
            id="companyName"
            type="text"
            placeholder="예) 위즈캔 주식회사"
            {...register('companyName')}
            style={inputStyle(!!errors.companyName)}
            onFocus={(e) => handleFocus(e, !!errors.companyName)}
            onBlur={(e) => handleBlur(e, !!errors.companyName)}
          />
          {errors.companyName && <p style={errorStyle}><AlertCircleIcon />{errors.companyName.message}</p>}
        </div>

        {/* 대표자명 */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ceoName" style={labelStyle}>대표자명 *</label>
          <input
            id="ceoName"
            type="text"
            placeholder="예) 홍길동"
            {...register('ceoName')}
            style={inputStyle(!!errors.ceoName)}
            onFocus={(e) => handleFocus(e, !!errors.ceoName)}
            onBlur={(e) => handleBlur(e, !!errors.ceoName)}
          />
          {errors.ceoName && <p style={errorStyle}><AlertCircleIcon />{errors.ceoName.message}</p>}
        </div>

        {/* 대표자 연락처 (선택) */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ceoPhone" style={labelStyle}>
            대표자 연락처 <span style={{ color: '#94A3B8', fontWeight: 400 }}>(선택)</span>
          </label>
          <input
            id="ceoPhone"
            type="tel"
            placeholder="예) 010-1234-5678"
            {...register('ceoPhone')}
            style={inputStyle(false)}
            onFocus={(e) => handleFocus(e, false)}
            onBlur={(e) => handleBlur(e, false)}
          />
        </div>

        {/* 서버 에러 */}
        {serverError && (
          <div
            role="alert"
            aria-live="polite"
            className="flex items-center gap-3"
            style={{ background: '#FEF2F2', border: '1px solid #FECACA', borderRadius: '8px', padding: '12px 16px' }}
          >
            <span style={{ color: '#DC2626', flexShrink: 0 }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </span>
            <p style={{ fontSize: '14px', color: '#DC2626', fontFamily: 'var(--font-pretendard)' }}>{serverError}</p>
          </div>
        )}

        {/* 제출 버튼 */}
        <button
          type="submit"
          disabled={isSubmitting}
          aria-busy={isSubmitting}
          className="w-full font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50"
          style={{
            height: '44px',
            background: '#2563EB',
            color: '#FFFFFF',
            borderRadius: '8px',
            fontSize: '15px',
            fontFamily: 'var(--font-pretendard)',
            border: 'none',
            cursor: isSubmitting ? 'not-allowed' : 'pointer',
          }}
          onMouseEnter={(e) => { if (!isSubmitting) (e.currentTarget as HTMLButtonElement).style.background = '#1D4ED8'; }}
          onMouseLeave={(e) => { if (!isSubmitting) (e.currentTarget as HTMLButtonElement).style.background = '#2563EB'; }}
        >
          {isSubmitting ? '신청 중...' : '사업자 전환 신청'}
        </button>
      </form>

      <p className="mt-6 text-center" style={{ fontSize: '14px', color: '#64748B', fontFamily: 'var(--font-pretendard)' }}>
        <Link href="/dashboard" style={{ color: '#2563EB', fontWeight: 500, textDecoration: 'none' }}>
          대시보드로 돌아가기
        </Link>
      </p>
    </div>
  );
}

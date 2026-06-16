'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { registerSchema, type RegisterFormValues } from '../model/registerSchema';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../model/authStore';

const AlertCircleIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10" />
    <line x1="12" y1="8" x2="12" y2="12" />
    <line x1="12" y1="16" x2="12.01" y2="16" />
  </svg>
);

const ZapIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
  </svg>
);

const CheckIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="20 6 9 17 4 12" />
  </svg>
);

export function RegisterForm() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      agreeTerms: false,
      agreePrivacy: false,
      agreeMarketing: false,
    },
  });

  const agreeTerms = watch('agreeTerms');
  const agreePrivacy = watch('agreePrivacy');
  const agreeMarketing = watch('agreeMarketing');
  const allRequired = agreeTerms && agreePrivacy;

  const toggleAll = () => {
    const next = !allRequired;
    setValue('agreeTerms', next, { shouldValidate: true });
    setValue('agreePrivacy', next, { shouldValidate: true });
    setValue('agreeMarketing', next);
  };

  const onSubmit = async (values: RegisterFormValues) => {
    setServerError(null);
    try {
      const termAgreements = [
        { termCode: 'TOS', agreed: values.agreeTerms },
        { termCode: 'PRIVACY', agreed: values.agreePrivacy },
        { termCode: 'MARKETING', agreed: values.agreeMarketing ?? false },
      ];
      const result = await authApi.register(
        values.email,
        values.password,
        values.name,
        values.phone,
        termAgreements
      );
      setAuth(result.accessToken, { id: 0, email: result.email, name: result.name });
      router.push('/dashboard');
    } catch {
      setServerError('회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있습니다.');
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

  return (
    <div className="w-full" style={{ maxWidth: '400px' }}>
      {/* 모바일 전용 로고 */}
      <div className="flex items-center gap-2 mb-8 lg:hidden">
        <ZapIcon />
        <span className="font-bold" style={{ fontSize: '22px', color: '#0F172A', fontFamily: 'var(--font-pretendard)' }}>
          Wisecan
        </span>
      </div>

      {/* 폼 헤더 */}
      <div className="mb-8">
        <h1 className="font-bold" style={{ fontSize: '30px', lineHeight: '38px', letterSpacing: '-0.01em', color: '#0F172A', fontFamily: 'var(--font-pretendard)' }}>
          회원가입
        </h1>
        <p className="mt-1" style={{ fontSize: '15px', color: '#64748B', fontFamily: 'var(--font-pretendard)' }}>
          Wisecan 계정을 만들어보세요
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
        {/* 이름 */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="name" style={labelStyle}>이름 *</label>
          <input
            id="name"
            type="text"
            autoComplete="name"
            placeholder="예) 김위즈캔"
            {...register('name')}
            style={inputStyle(!!errors.name)}
            onFocus={(e) => handleFocus(e, !!errors.name)}
            onBlur={(e) => handleBlur(e, !!errors.name)}
          />
          {errors.name && <p style={errorStyle}><AlertCircleIcon />{errors.name.message}</p>}
        </div>

        {/* 이메일 */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="email" style={labelStyle}>이메일 *</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="예) user@wisecan.co.kr"
            {...register('email')}
            style={inputStyle(!!errors.email)}
            onFocus={(e) => handleFocus(e, !!errors.email)}
            onBlur={(e) => handleBlur(e, !!errors.email)}
          />
          {errors.email && <p style={errorStyle}><AlertCircleIcon />{errors.email.message}</p>}
        </div>

        {/* 휴대폰 (선택) */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="phone" style={labelStyle}>
            휴대폰 번호 <span style={{ color: '#94A3B8', fontWeight: 400 }}>(선택)</span>
          </label>
          <input
            id="phone"
            type="tel"
            autoComplete="tel"
            placeholder="예) 010-1234-5678"
            {...register('phone')}
            style={inputStyle(false)}
            onFocus={(e) => handleFocus(e, false)}
            onBlur={(e) => handleBlur(e, false)}
          />
        </div>

        {/* 비밀번호 */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" style={labelStyle}>비밀번호 *</label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder="8자 이상, 영문+숫자 조합"
            {...register('password')}
            style={inputStyle(!!errors.password)}
            onFocus={(e) => handleFocus(e, !!errors.password)}
            onBlur={(e) => handleBlur(e, !!errors.password)}
          />
          {errors.password ? (
            <p style={errorStyle}><AlertCircleIcon />{errors.password.message}</p>
          ) : (
            <p style={{ fontSize: '12px', color: '#94A3B8', fontFamily: 'var(--font-pretendard)' }}>
              영문, 숫자를 포함하여 8자 이상 입력하세요
            </p>
          )}
        </div>

        {/* 비밀번호 확인 */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="confirmPassword" style={labelStyle}>비밀번호 확인 *</label>
          <input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            placeholder="비밀번호를 다시 입력하세요"
            {...register('confirmPassword')}
            style={inputStyle(!!errors.confirmPassword)}
            onFocus={(e) => handleFocus(e, !!errors.confirmPassword)}
            onBlur={(e) => handleBlur(e, !!errors.confirmPassword)}
          />
          {errors.confirmPassword && <p style={errorStyle}><AlertCircleIcon />{errors.confirmPassword.message}</p>}
        </div>

        {/* 약관 동의 */}
        <div
          className="flex flex-col gap-2"
          style={{ border: '1px solid #E2E8F0', borderRadius: '8px', padding: '16px' }}
        >
          {/* 전체 동의 */}
          <button
            type="button"
            onClick={toggleAll}
            className="flex items-center gap-2 w-full text-left"
            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
          >
            <span
              style={{
                width: '18px',
                height: '18px',
                borderRadius: '4px',
                border: allRequired ? 'none' : '1.5px solid #CBD5E1',
                background: allRequired ? '#2563EB' : '#FFFFFF',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                color: '#FFFFFF',
              }}
            >
              {allRequired && <CheckIcon />}
            </span>
            <span style={{ fontSize: '14px', fontWeight: 600, color: '#0F172A', fontFamily: 'var(--font-pretendard)' }}>
              전체 동의
            </span>
          </button>

          <hr style={{ border: 'none', borderTop: '1px solid #F1F5F9', margin: '4px 0' }} />

          {/* 이용약관 (필수) */}
          <div className="flex items-center gap-2">
            <input
              id="agreeTerms"
              type="checkbox"
              {...register('agreeTerms')}
              style={{ width: '16px', height: '16px', accentColor: '#2563EB', cursor: 'pointer' }}
            />
            <label htmlFor="agreeTerms" style={{ fontSize: '13px', color: '#374151', fontFamily: 'var(--font-pretendard)', cursor: 'pointer', flex: 1 }}>
              <span style={{ color: '#2563EB', fontWeight: 600 }}>[필수]</span> 이용약관 동의
            </label>
            <Link href="/terms" style={{ fontSize: '12px', color: '#94A3B8', textDecoration: 'underline' }}>
              보기
            </Link>
          </div>
          {errors.agreeTerms && <p style={{ ...errorStyle, paddingLeft: '24px' }}><AlertCircleIcon />{errors.agreeTerms.message}</p>}

          {/* 개인정보처리방침 (필수) */}
          <div className="flex items-center gap-2">
            <input
              id="agreePrivacy"
              type="checkbox"
              {...register('agreePrivacy')}
              style={{ width: '16px', height: '16px', accentColor: '#2563EB', cursor: 'pointer' }}
            />
            <label htmlFor="agreePrivacy" style={{ fontSize: '13px', color: '#374151', fontFamily: 'var(--font-pretendard)', cursor: 'pointer', flex: 1 }}>
              <span style={{ color: '#2563EB', fontWeight: 600 }}>[필수]</span> 개인정보처리방침 동의
            </label>
            <Link href="/privacy" style={{ fontSize: '12px', color: '#94A3B8', textDecoration: 'underline' }}>
              보기
            </Link>
          </div>
          {errors.agreePrivacy && <p style={{ ...errorStyle, paddingLeft: '24px' }}><AlertCircleIcon />{errors.agreePrivacy.message}</p>}

          {/* 마케팅 수신 동의 (선택) */}
          <div className="flex items-center gap-2">
            <input
              id="agreeMarketing"
              type="checkbox"
              {...register('agreeMarketing')}
              style={{ width: '16px', height: '16px', accentColor: '#2563EB', cursor: 'pointer' }}
            />
            <label htmlFor="agreeMarketing" style={{ fontSize: '13px', color: '#374151', fontFamily: 'var(--font-pretendard)', cursor: 'pointer', flex: 1 }}>
              <span style={{ color: '#64748B', fontWeight: 600 }}>[선택]</span> 마케팅 수신 동의
            </label>
          </div>
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
          {isSubmitting ? '계정 생성 중...' : '회원가입'}
        </button>
      </form>

      {/* 사업자 전환 안내 */}
      <div
        className="mt-4"
        style={{ background: '#F8FAFC', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '12px 16px' }}
      >
        <p style={{ fontSize: '13px', color: '#475569', fontFamily: 'var(--font-pretendard)', lineHeight: '1.6' }}>
          사업자 회원으로 전환하려면 개인 회원 가입 후{' '}
          <Link href="/signup/business" style={{ color: '#2563EB', fontWeight: 500 }}>
            사업자 전환 신청
          </Link>
          을 진행해 주세요.
        </p>
      </div>

      {/* 로그인 링크 */}
      <p className="mt-6 text-center" style={{ fontSize: '14px', color: '#64748B', fontFamily: 'var(--font-pretendard)' }}>
        이미 계정이 있으신가요?{' '}
        <Link
          href="/login"
          style={{ color: '#2563EB', fontWeight: 500, textDecoration: 'none' }}
          onMouseEnter={(e) => (e.currentTarget.style.textDecoration = 'underline')}
          onMouseLeave={(e) => (e.currentTarget.style.textDecoration = 'none')}
        >
          로그인
        </Link>
      </p>
    </div>
  );
}

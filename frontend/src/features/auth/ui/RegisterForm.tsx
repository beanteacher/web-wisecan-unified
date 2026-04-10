'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { AlertCircle, Zap } from 'lucide-react';
import { registerSchema, type RegisterFormValues } from '../model/registerSchema';
import { authApi } from '../api/authApi';

export function RegisterForm() {
  const router = useRouter();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (values: RegisterFormValues) => {
    setServerError(null);
    try {
      await authApi.register(values.email, values.password, values.name);
      router.push('/login');
    } catch {
      setServerError('회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있습니다.');
    }
  };

  const inputStyle = (hasError: boolean) => ({
    height: '40px',
    border: hasError ? '1px solid #EF4444' : '1px solid #E2E8F0',
    borderRadius: '8px',
    background: '#FFFFFF',
    color: '#0F172A',
    fontFamily: 'var(--font-pretendard)',
    boxShadow: hasError ? '0 0 0 2px rgba(252,165,165,0.4)' : 'none',
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

  return (
    <div className="w-full" style={{ maxWidth: '400px' }}>
      {/* 모바일 전용 로고 헤더 */}
      <div className="flex items-center gap-2 mb-8 lg:hidden">
        <Zap size={22} style={{ color: '#2563EB' }} />
        <span
          className="font-bold"
          style={{
            fontSize: '22px',
            color: '#0F172A',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          Wisecan
        </span>
      </div>

      {/* 폼 헤더 */}
      <div className="mb-8">
        <h1
          className="font-bold"
          style={{
            fontSize: '30px',
            lineHeight: '38px',
            letterSpacing: '-0.01em',
            color: '#0F172A',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          회원가입
        </h1>
        <p
          className="mt-1"
          style={{
            fontSize: '15px',
            color: '#64748B',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          Wisecan 계정을 만들어보세요
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
        {/* 이름 필드 */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="name"
            style={{
              fontSize: '14px',
              fontWeight: 500,
              color: '#0F172A',
              fontFamily: 'var(--font-pretendard)',
            }}
          >
            이름 *
          </label>
          <input
            id="name"
            type="text"
            autoComplete="name"
            placeholder="예) 김위즈캔"
            {...register('name')}
            className="w-full px-3 text-sm transition-all outline-none"
            style={inputStyle(!!errors.name)}
            onFocus={(e) => handleFocus(e, !!errors.name)}
            onBlur={(e) => handleBlur(e, !!errors.name)}
          />
          {errors.name && (
            <p
              className="flex items-center gap-1"
              style={{ fontSize: '12px', color: '#DC2626', fontFamily: 'var(--font-pretendard)' }}
            >
              <AlertCircle size={12} />
              {errors.name.message}
            </p>
          )}
        </div>

        {/* 이메일 필드 */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="email"
            style={{
              fontSize: '14px',
              fontWeight: 500,
              color: '#0F172A',
              fontFamily: 'var(--font-pretendard)',
            }}
          >
            이메일 *
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="예) user@wisecan.co.kr"
            {...register('email')}
            className="w-full px-3 text-sm transition-all outline-none"
            style={inputStyle(!!errors.email)}
            onFocus={(e) => handleFocus(e, !!errors.email)}
            onBlur={(e) => handleBlur(e, !!errors.email)}
          />
          {errors.email && (
            <p
              className="flex items-center gap-1"
              style={{ fontSize: '12px', color: '#DC2626', fontFamily: 'var(--font-pretendard)' }}
            >
              <AlertCircle size={12} />
              {errors.email.message}
            </p>
          )}
        </div>

        {/* 비밀번호 필드 */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="password"
            style={{
              fontSize: '14px',
              fontWeight: 500,
              color: '#0F172A',
              fontFamily: 'var(--font-pretendard)',
            }}
          >
            비밀번호 *
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder="8자 이상, 영문+숫자 조합"
            {...register('password')}
            className="w-full px-3 text-sm transition-all outline-none"
            style={inputStyle(!!errors.password)}
            onFocus={(e) => handleFocus(e, !!errors.password)}
            onBlur={(e) => handleBlur(e, !!errors.password)}
          />
          {errors.password ? (
            <p
              className="flex items-center gap-1"
              style={{ fontSize: '12px', color: '#DC2626', fontFamily: 'var(--font-pretendard)' }}
            >
              <AlertCircle size={12} />
              {errors.password.message}
            </p>
          ) : (
            <p
              style={{
                fontSize: '12px',
                color: '#94A3B8',
                fontFamily: 'var(--font-pretendard)',
              }}
            >
              영문, 숫자를 포함하여 8자 이상 입력하세요
            </p>
          )}
        </div>

        {/* 비밀번호 확인 필드 */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="confirmPassword"
            style={{
              fontSize: '14px',
              fontWeight: 500,
              color: '#0F172A',
              fontFamily: 'var(--font-pretendard)',
            }}
          >
            비밀번호 확인 *
          </label>
          <input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            placeholder="비밀번호를 다시 입력하세요"
            {...register('confirmPassword')}
            className="w-full px-3 text-sm transition-all outline-none"
            style={inputStyle(!!errors.confirmPassword)}
            onFocus={(e) => handleFocus(e, !!errors.confirmPassword)}
            onBlur={(e) => handleBlur(e, !!errors.confirmPassword)}
          />
          {errors.confirmPassword && (
            <p
              className="flex items-center gap-1"
              style={{ fontSize: '12px', color: '#DC2626', fontFamily: 'var(--font-pretendard)' }}
            >
              <AlertCircle size={12} />
              {errors.confirmPassword.message}
            </p>
          )}
        </div>

        {/* 서버 에러 박스 */}
        {serverError && (
          <div
            className="flex items-center gap-3"
            role="alert"
            aria-live="polite"
            style={{
              background: '#FEF2F2',
              border: '1px solid #FECACA',
              borderRadius: '8px',
              padding: '12px 16px',
            }}
          >
            <AlertCircle size={16} style={{ color: '#DC2626', flexShrink: 0 }} />
            <p
              style={{
                fontSize: '14px',
                color: '#DC2626',
                fontFamily: 'var(--font-pretendard)',
              }}
            >
              {serverError}
            </p>
          </div>
        )}

        {/* 제출 버튼 */}
        <button
          type="submit"
          disabled={isSubmitting}
          aria-busy={isSubmitting}
          aria-label={isSubmitting ? '계정 생성 중' : '회원가입'}
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
          onMouseEnter={(e) => {
            if (!isSubmitting) (e.currentTarget as HTMLButtonElement).style.background = '#1D4ED8';
          }}
          onMouseLeave={(e) => {
            if (!isSubmitting) (e.currentTarget as HTMLButtonElement).style.background = '#2563EB';
          }}
        >
          {isSubmitting ? '계정 생성 중...' : '회원가입'}
        </button>

        {/* 약관 문구 */}
        <p
          className="text-center"
          style={{
            fontSize: '12px',
            color: '#94A3B8',
            lineHeight: '18px',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          가입하면 Wisecan의{' '}
          <Link
            href="/terms"
            style={{ color: '#64748B', textDecoration: 'underline' }}
          >
            이용약관
          </Link>{' '}
          및{' '}
          <Link
            href="/privacy"
            style={{ color: '#64748B', textDecoration: 'underline' }}
          >
            개인정보처리방침
          </Link>
          에 동의하는 것으로 간주됩니다.
        </p>
      </form>

      {/* 하단 로그인 링크 */}
      <p
        className="mt-6 text-center"
        style={{
          fontSize: '14px',
          color: '#64748B',
          fontFamily: 'var(--font-pretendard)',
        }}
      >
        이미 계정이 있으신가요?{' '}
        <Link
          href="/login"
          style={{
            color: '#2563EB',
            fontWeight: 500,
            textDecoration: 'none',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.textDecoration = 'underline')}
          onMouseLeave={(e) => (e.currentTarget.style.textDecoration = 'none')}
        >
          로그인
        </Link>
      </p>
    </div>
  );
}

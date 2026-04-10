'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { AlertCircle, Zap } from 'lucide-react';
import { loginSchema, type LoginFormValues } from '../model/loginSchema';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../model/authStore';

export function LoginForm() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (values: LoginFormValues) => {
    setServerError(null);
    try {
      const { accessToken, member } = await authApi.login(values.email, values.password);
      setAuth(accessToken, member);
      router.push('/dashboard');
    } catch {
      setServerError('이메일 또는 비밀번호가 일치하지 않습니다');
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
          로그인
        </h1>
        <p
          className="mt-1"
          style={{
            fontSize: '15px',
            color: '#64748B',
            fontFamily: 'var(--font-pretendard)',
          }}
        >
          Wisecan 계정에 로그인하세요
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
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
            style={{
              height: '40px',
              border: errors.email ? '1px solid #EF4444' : '1px solid #E2E8F0',
              borderRadius: '8px',
              background: '#FFFFFF',
              color: '#0F172A',
              fontFamily: 'var(--font-pretendard)',
              boxShadow: errors.email
                ? '0 0 0 2px rgba(252,165,165,0.4)'
                : 'none',
            }}
            onFocus={(e) => {
              if (!errors.email) {
                e.target.style.border = '1px solid transparent';
                e.target.style.boxShadow = '0 0 0 2px #2563EB';
              }
            }}
            onBlur={(e) => {
              if (!errors.email) {
                e.target.style.border = '1px solid #E2E8F0';
                e.target.style.boxShadow = 'none';
              }
            }}
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
          <div className="flex items-center justify-between">
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
            <Link
              href="/auth/forgot-password"
              style={{
                fontSize: '13px',
                color: '#2563EB',
                fontFamily: 'var(--font-pretendard)',
                textDecoration: 'none',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.textDecoration = 'underline')}
              onMouseLeave={(e) => (e.currentTarget.style.textDecoration = 'none')}
            >
              비밀번호 찾기
            </Link>
          </div>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            placeholder="비밀번호를 입력하세요"
            {...register('password')}
            className="w-full px-3 text-sm transition-all outline-none"
            style={{
              height: '40px',
              border: errors.password ? '1px solid #EF4444' : '1px solid #E2E8F0',
              borderRadius: '8px',
              background: '#FFFFFF',
              color: '#0F172A',
              fontFamily: 'var(--font-pretendard)',
              boxShadow: errors.password
                ? '0 0 0 2px rgba(252,165,165,0.4)'
                : 'none',
            }}
            onFocus={(e) => {
              if (!errors.password) {
                e.target.style.border = '1px solid transparent';
                e.target.style.boxShadow = '0 0 0 2px #2563EB';
              }
            }}
            onBlur={(e) => {
              if (!errors.password) {
                e.target.style.border = '1px solid #E2E8F0';
                e.target.style.boxShadow = 'none';
              }
            }}
          />
          {errors.password && (
            <p
              className="flex items-center gap-1"
              style={{ fontSize: '12px', color: '#DC2626', fontFamily: 'var(--font-pretendard)' }}
            >
              <AlertCircle size={12} />
              {errors.password.message}
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
          aria-label={isSubmitting ? '로그인 중' : '로그인'}
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
          {isSubmitting ? '로그인 중...' : '로그인'}
        </button>
      </form>

      {/* 하단 회원가입 링크 */}
      <p
        className="mt-6 text-center"
        style={{
          fontSize: '14px',
          color: '#64748B',
          fontFamily: 'var(--font-pretendard)',
        }}
      >
        계정이 없으신가요?{' '}
        <Link
          href="/register"
          style={{
            color: '#2563EB',
            fontWeight: 500,
            textDecoration: 'none',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.textDecoration = 'underline')}
          onMouseLeave={(e) => (e.currentTarget.style.textDecoration = 'none')}
        >
          회원가입
        </Link>
      </p>
    </div>
  );
}

'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import { csApi, type InquiryCategory } from '../api/csApi';

const CATEGORIES: { value: InquiryCategory; label: string }[] = [
  { value: 'ACCOUNT', label: '계정 / 인증' },
  { value: 'SEND', label: '발송 / API' },
  { value: 'BILLING', label: '결제 / 충전' },
  { value: 'TECHNICAL', label: '기술 / 장애' },
  { value: 'ETC', label: '기타' },
];

const schema = z.object({
  category: z.enum(['ACCOUNT', 'SEND', 'BILLING', 'TECHNICAL', 'ETC'] as const, {
    required_error: '카테고리를 선택해 주세요',
  }),
  title: z.string().min(1, '제목을 입력해 주세요').max(200, '200자 이하로 입력해 주세요'),
  content: z.string().min(1, '내용을 입력해 주세요'),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  onSuccess?: () => void;
}

export function InquiryForm({ onSuccess }: Props) {
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      await csApi.createInquiry(values);
      setSubmitted(true);
      onSuccess?.();
    } catch {
      setServerError('문의 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
    }
  };

  if (submitted) {
    return (
      <div
        style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          gap: '12px', padding: '48px 24px', textAlign: 'center',
        }}
      >
        <CheckCircle2 size={48} style={{ color: '#16a34a' }} />
        <p style={{ fontSize: '18px', fontWeight: 700, color: '#0f172a' }}>문의가 접수되었습니다</p>
        <p style={{ fontSize: '14px', color: '#64748b' }}>
          영업일 기준 24시간 내에 이메일로 답변을 드립니다.
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* 카테고리 */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <label style={{ fontSize: '14px', fontWeight: 500, color: '#0f172a' }}>
          문의 유형 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <select
          {...register('category')}
          style={{
            height: '40px', padding: '0 12px', borderRadius: '8px',
            border: errors.category ? '1px solid #ef4444' : '1px solid #e2e8f0',
            fontSize: '14px', color: '#0f172a', background: '#fff', outline: 'none',
          }}
        >
          <option value="">카테고리 선택</option>
          {CATEGORIES.map((c) => (
            <option key={c.value} value={c.value}>{c.label}</option>
          ))}
        </select>
        {errors.category && (
          <p style={{ fontSize: '12px', color: '#dc2626', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <AlertCircle size={12} />{errors.category.message}
          </p>
        )}
      </div>

      {/* 제목 */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <label style={{ fontSize: '14px', fontWeight: 500, color: '#0f172a' }}>
          제목 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <input
          {...register('title')}
          placeholder="문의 제목을 입력하세요"
          style={{
            height: '40px', padding: '0 12px', borderRadius: '8px',
            border: errors.title ? '1px solid #ef4444' : '1px solid #e2e8f0',
            fontSize: '14px', color: '#0f172a', background: '#fff', outline: 'none',
          }}
        />
        {errors.title && (
          <p style={{ fontSize: '12px', color: '#dc2626', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <AlertCircle size={12} />{errors.title.message}
          </p>
        )}
      </div>

      {/* 내용 */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <label style={{ fontSize: '14px', fontWeight: 500, color: '#0f172a' }}>
          내용 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <textarea
          {...register('content')}
          rows={6}
          placeholder="문의 내용을 상세히 입력해 주세요"
          style={{
            padding: '12px', borderRadius: '8px', resize: 'vertical',
            border: errors.content ? '1px solid #ef4444' : '1px solid #e2e8f0',
            fontSize: '14px', color: '#0f172a', background: '#fff',
            outline: 'none', fontFamily: 'inherit',
          }}
        />
        {errors.content && (
          <p style={{ fontSize: '12px', color: '#dc2626', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <AlertCircle size={12} />{errors.content.message}
          </p>
        )}
      </div>

      {serverError && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: '10px',
          background: '#fef2f2', border: '1px solid #fecaca',
          borderRadius: '8px', padding: '12px 16px',
        }}>
          <AlertCircle size={16} style={{ color: '#dc2626', flexShrink: 0 }} />
          <p style={{ fontSize: '14px', color: '#dc2626' }}>{serverError}</p>
        </div>
      )}

      <button
        type="submit"
        disabled={isSubmitting}
        style={{
          height: '44px', background: isSubmitting ? '#93c5fd' : '#2563eb',
          color: '#fff', border: 'none', borderRadius: '8px',
          fontSize: '15px', fontWeight: 600, cursor: isSubmitting ? 'not-allowed' : 'pointer',
          transition: 'background 0.15s',
        }}
      >
        {isSubmitting ? '접수 중...' : '문의 접수'}
      </button>
    </form>
  );
}

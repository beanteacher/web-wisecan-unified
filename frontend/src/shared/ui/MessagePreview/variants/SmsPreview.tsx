import type { MessagePreviewVariant } from '../MessagePreview';
import { cn } from '@/shared/lib/cn';

interface SmsPreviewProps {
  variant: MessagePreviewVariant;
  recipient?: string;
  content?: string;
  imageUrl?: string;
  imageName?: string;
}

const VARIANT_BADGE: Record<MessagePreviewVariant, { label: string; className: string }> = {
  sms: { label: 'SMS', className: 'bg-blue-50 text-blue-700' },
  lms: { label: 'LMS', className: 'bg-orange-50 text-orange-700' },
  mms: { label: 'MMS 첨부', className: 'bg-green-50 text-green-700' },
};

const VARIANT_FOOTER: Record<MessagePreviewVariant, string> = {
  sms: '발송 예정 · 문자 메시지(SMS)',
  lms: '발송 예정 · 장문 메시지(LMS)',
  mms: '발송 예정 · 멀티미디어(MMS)',
};

export function SmsPreview({ variant, recipient, content, imageUrl, imageName }: SmsPreviewProps) {
  const badge = VARIANT_BADGE[variant];
  const footer = VARIANT_FOOTER[variant];
  const displayRecipient = recipient || '01012345678';

  return (
    <div className="flex h-full flex-col overflow-hidden rounded-xl border border-slate-200 bg-white">
      {/* Chat header — iOS style */}
      <div className="flex shrink-0 items-center justify-between border-b border-slate-200 bg-white px-4 py-3">
        <div>
          <p className="text-[14px] font-semibold text-slate-900">{displayRecipient}</p>
          <p className="text-[11px] text-slate-400">수신자</p>
        </div>
        <span
          className={cn(
            'rounded-full px-3 py-1 text-[11px] font-medium',
            badge.className,
          )}
        >
          {badge.label}
        </span>
      </div>

      {/* Chat area */}
      <div className="flex flex-1 flex-col overflow-y-auto bg-white px-4 py-4">
        {/* Date separator */}
        <div className="mb-4 flex items-center gap-2">
          <div className="h-px flex-1 bg-slate-200" />
          <span className="text-[11px] text-slate-400">오늘</span>
          <div className="h-px flex-1 bg-slate-200" />
        </div>

        {/* Message bubble area — right aligned (sender) */}
        <div className="flex flex-col items-end gap-1">
          {/* MMS image thumbnail */}
          {variant === 'mms' && (
            <div
              className="w-[75%] overflow-hidden rounded-t-[18px] rounded-b-none"
              style={{ minHeight: 120 }}
            >
              {imageUrl ? (
                <img
                  src={imageUrl}
                  alt={imageName ?? '첨부 이미지'}
                  className="h-[120px] w-full object-cover"
                />
              ) : (
                <div className="flex h-[120px] w-full flex-col items-center justify-center bg-slate-200">
                  <span className="text-[13px] font-medium text-slate-500">이벤트 배너</span>
                  <span className="text-[11px] text-slate-400">JPG / PNG</span>
                </div>
              )}
              {imageName && (
                <p className="bg-[#007AFF] px-3 py-1 text-[11px] text-white/80">{imageName}</p>
              )}
            </div>
          )}

          {/* Blue bubble */}
          {content ? (
            <div
              className={cn(
                'relative max-w-[75%] rounded-[18px] bg-[#007AFF] px-3 py-2.5',
                variant === 'mms' && imageUrl
                  ? 'rounded-t-none'
                  : variant === 'mms'
                    ? 'rounded-t-none'
                    : '',
              )}
              aria-label="발신 메시지"
            >
              {/* Bubble tail (bottom-right) */}
              <div
                className="absolute -right-0 bottom-0 h-3 w-3 bg-[#007AFF]"
                style={{ borderRadius: '0 0 4px 0' }}
                aria-hidden="true"
              />
              <p
                className={cn(
                  'whitespace-pre-wrap text-[14px] leading-[1.5] text-white',
                  variant === 'lms' && 'text-[13px]',
                )}
              >
                {content}
              </p>
            </div>
          ) : (
            <div
              className="relative max-w-[75%] rounded-[18px] bg-[#007AFF] px-3 py-2.5"
              aria-label="발신 메시지"
            >
              <div
                className="absolute -right-0 bottom-0 h-3 w-3 bg-[#007AFF]"
                style={{ borderRadius: '0 0 4px 0' }}
                aria-hidden="true"
              />
              <p className="text-[14px] leading-[1.5] text-white/60 italic">본문을 입력하세요...</p>
            </div>
          )}

          {/* Timestamp + status */}
          <p className="text-[11px] text-slate-400">방금 전 &nbsp; 전송됨</p>
        </div>

        {/* LMS scroll hint */}
        {variant === 'lms' && (
          <p className="mt-3 text-center text-[11px] text-slate-400">↑ 스크롤하여 더 보기</p>
        )}
      </div>

      {/* Bottom status bar */}
      <div className="shrink-0 border-t border-slate-200 bg-slate-50 px-4 py-2 text-center">
        <p className="text-[11px] text-slate-400">{footer}</p>
      </div>
    </div>
  );
}

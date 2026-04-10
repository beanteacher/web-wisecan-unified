'use client';

import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useMessageResult } from '../model/useMessageResult';
import type { MessageStatus } from '@/entities/message';

const STATUS_MAP: Record<MessageStatus, { label: string; variant: 'success' | 'destructive' | 'warning' | 'neutral' }> = {
  SUCCESS: { label: '성공', variant: 'success' },
  FAILED: { label: '실패', variant: 'destructive' },
  PENDING: { label: '대기 중', variant: 'warning' },
  PARTIAL: { label: '부분 성공', variant: 'warning' },
};

const CHANNEL_LABEL: Record<string, string> = {
  SMS: 'SMS',
  LMS: 'LMS',
  MMS: 'MMS',
};

interface Props {
  msgId: string | null;
}

export function MessageResultDetail({ msgId }: Props) {
  const { data, isLoading, isError } = useMessageResult(msgId);

  if (!msgId) {
    return (
      <div className="flex items-center justify-center py-16 text-sm text-slate-400">
        메시지 ID를 입력하거나 발송 후 결과를 확인하세요.
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="space-y-3 animate-pulse">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-10 rounded-lg bg-slate-100" />
        ))}
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="flex items-center justify-center py-16 text-sm text-red-500">
        결과를 불러오지 못했습니다. 메시지 ID를 확인하세요.
      </div>
    );
  }

  const statusInfo = STATUS_MAP[data.status];
  const meta = data.metadata as Record<string, unknown> | null | undefined;
  const responseTimeMs = typeof meta?.responseTimeMs === 'number' ? meta.responseTimeMs : null;
  const errorCode = typeof meta?.errorCode === 'string' ? meta.errorCode : null;
  const errorMessage = typeof meta?.errorMessage === 'string' ? meta.errorMessage : null;

  return (
    <div className="space-y-4">
      {/* 상태 카드 — PC: 4분할 가로, Mobile: 2×2 그리드 */}
      <div className="rounded-xl border border-slate-200 bg-white">
        {/* PC */}
        <div className="hidden md:flex divide-x divide-slate-200">
          <StatCell label="메시지 ID">
            <span className="text-base font-bold text-slate-900">{data.messageId}</span>
          </StatCell>
          <StatCell label="발송 상태">
            <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
          </StatCell>
          <StatCell label="응답시간">
            <span className="text-base font-bold text-slate-900">
              {responseTimeMs !== null ? `${responseTimeMs}ms` : '-'}
            </span>
          </StatCell>
          <StatCell label="발송 시각">
            <span className="text-base font-bold text-slate-900">
              {format(new Date(data.sentAt), 'MM-dd HH:mm', { locale: ko })}
            </span>
          </StatCell>
        </div>

        {/* Mobile: 2×2 grid */}
        <div className="grid grid-cols-2 divide-x divide-y divide-slate-200 md:hidden">
          <div className="flex flex-col gap-1 px-4 py-3">
            <dt className="text-[10px] font-medium text-slate-500">메시지 ID</dt>
            <dd className="text-[13px] font-bold text-slate-900 truncate">{data.messageId}</dd>
          </div>
          <div className="flex flex-col gap-1 px-4 py-3">
            <dt className="text-[10px] font-medium text-slate-500">발송 상태</dt>
            <dd><Badge variant={statusInfo.variant}>{statusInfo.label}</Badge></dd>
          </div>
          <div className="flex flex-col gap-1 px-4 py-3">
            <dt className="text-[10px] font-medium text-slate-500">응답시간</dt>
            <dd className="text-[13px] font-bold text-slate-900">
              {responseTimeMs !== null ? `${responseTimeMs}ms` : '-'}
            </dd>
          </div>
          <div className="flex flex-col gap-1 px-4 py-3">
            <dt className="text-[10px] font-medium text-slate-500">발송 시각</dt>
            <dd className="text-[13px] font-bold text-slate-900">
              {format(new Date(data.sentAt), 'MM-dd HH:mm', { locale: ko })}
            </dd>
          </div>
        </div>
      </div>

      {/* 발송 정보 카드 */}
      <div className="rounded-xl border border-slate-200 bg-white p-4 md:p-6">
        <h3 className="mb-4 text-[14px] font-semibold text-slate-900 md:text-[15px]">발송 정보</h3>
        <dl className="space-y-4">
          <InfoRow label="채널">
            <Badge variant="info">{CHANNEL_LABEL[data.channel] ?? data.channel}</Badge>
          </InfoRow>
          <InfoRow label="수신자">
            <span className="text-[12px] text-slate-900 md:text-[13px]">{data.recipient}</span>
          </InfoRow>
          <InfoRow label="본문">
            <span className="whitespace-pre-wrap text-[12px] text-slate-900 md:text-[13px]">{data.content}</span>
          </InfoRow>
          {data.deliveredAt && (
            <InfoRow label="수신 시각">
              <span className="text-[12px] text-slate-900 md:text-[13px]">
                {format(new Date(data.deliveredAt), 'yyyy-MM-dd HH:mm:ss', { locale: ko })}
              </span>
            </InfoRow>
          )}
        </dl>
      </div>

      {/* 에러 카드 — FAILED 상태일 때만 표시 */}
      {data.status === 'FAILED' && (
        <div className="relative overflow-hidden rounded-xl border border-red-200 bg-red-50 p-4 md:p-6">
          <div className="absolute left-0 top-0 h-full w-1 bg-red-600 rounded-l-xl" />
          <h3 className="mb-3 pl-2 text-[13px] font-semibold text-red-600 md:text-[14px]">
            ! 에러 정보 (실패 시 표시)
          </h3>
          <dl className="space-y-3 pl-2">
            {errorCode && (
              <div>
                <dt className="text-[10px] font-medium text-red-600 md:text-[11px]">에러 코드</dt>
                <dd className="mt-0.5 text-[12px] font-medium text-slate-900">{errorCode}</dd>
              </div>
            )}
            {errorMessage && (
              <div>
                <dt className="text-[10px] font-medium text-red-600 md:text-[11px]">에러 메시지</dt>
                <dd className="mt-0.5 text-[12px] text-slate-900">{errorMessage}</dd>
              </div>
            )}
          </dl>
          <div className="mt-5 flex gap-2 pl-2">
            <Button size="sm" className="bg-red-600 text-white hover:bg-red-700 border-0">재발송</Button>
            <Button size="sm" variant="secondary" className="border border-red-600 text-red-600 bg-red-50 hover:bg-red-100">지원 문의</Button>
          </div>
        </div>
      )}
    </div>
  );
}

function StatCell({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-1 flex-col gap-1 px-6 py-4">
      <dt className="text-[11px] font-medium text-slate-500">{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

function InfoRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-4 md:gap-6">
      <dt className="w-[80px] shrink-0 text-[11px] font-medium text-slate-500 md:w-[120px] md:text-[12px]">{label}</dt>
      <dd className="flex-1 min-w-0">{children}</dd>
    </div>
  );
}

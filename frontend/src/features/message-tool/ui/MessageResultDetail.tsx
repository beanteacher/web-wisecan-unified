'use client';

import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { Badge } from '@/components/ui/badge';
import { useMessageResult } from '../model/useMessageResult';
import type { MessageStatus } from '@/entities/message';

const STATUS_MAP: Record<MessageStatus, { label: string; variant: 'success' | 'destructive' | 'warning' | 'neutral' }> = {
  SUCCESS: { label: '성공', variant: 'success' },
  FAILED: { label: '실패', variant: 'destructive' },
  PENDING: { label: '대기 중', variant: 'warning' },
  PARTIAL: { label: '부분 성공', variant: 'warning' },
};

const CHANNEL_LABEL: Record<string, string> = {
  EMAIL: '이메일',
  SMS: 'SMS',
  PUSH: '푸시 알림',
  SLACK: 'Slack',
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

  return (
    <div className="space-y-4">
      <dl className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white overflow-hidden">
        <Row label="메시지 ID">
          <span className="font-mono text-xs text-slate-700">{data.messageId}</span>
        </Row>
        <Row label="상태">
          <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
        </Row>
        <Row label="채널">
          <span className="text-sm text-slate-700">{CHANNEL_LABEL[data.channel] ?? data.channel}</span>
        </Row>
        <Row label="수신자">
          <span className="text-sm text-slate-700">{data.recipient}</span>
        </Row>
        <Row label="내용">
          <span className="whitespace-pre-wrap text-sm text-slate-700">{data.content}</span>
        </Row>
        {data.deliveredAt && (
          <Row label="수신 시각">
            <span className="text-sm text-slate-700">
              {format(new Date(data.deliveredAt), 'yyyy.MM.dd HH:mm:ss', { locale: ko })}
            </span>
          </Row>
        )}
        <Row label="발송 시각">
          <span className="text-sm text-slate-700">
            {format(new Date(data.sentAt), 'yyyy.MM.dd HH:mm:ss', { locale: ko })}
          </span>
        </Row>
      </dl>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-4 px-5 py-3.5">
      <dt className="w-28 shrink-0 text-xs font-medium text-slate-500 pt-0.5">{label}</dt>
      <dd className="flex-1 min-w-0">{children}</dd>
    </div>
  );
}

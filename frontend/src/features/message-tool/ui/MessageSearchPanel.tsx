'use client';

import { useState } from 'react';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  DataTable,
  DataTableHeader,
  DataTableHeaderCell,
  DataTableBody,
  DataTableRow,
  DataTableCell,
  DataTableFooter,
} from '@/components/ui/data-table';
import { useSearchMessages } from '../model/useSearchMessages';
import type { MessageChannel, MessageStatus } from '@/entities/message';

const STATUS_MAP: Record<MessageStatus, { label: string; variant: 'success' | 'destructive' | 'warning' | 'neutral' }> = {
  SUCCESS: { label: '성공', variant: 'success' },
  FAILED: { label: '실패', variant: 'destructive' },
  PENDING: { label: '대기 중', variant: 'warning' },
  PARTIAL: { label: '부분 성공', variant: 'warning' },
};

const CHANNEL_LABEL: Record<string, string> = {
  EMAIL: '이메일',
  SMS: 'SMS',
  PUSH: '푸시',
  SLACK: 'Slack',
};

const PAGE_SIZE = 20;

export function MessageSearchPanel() {
  const [channel, setChannel] = useState<MessageChannel | ''>('');
  const [status, setStatus] = useState<MessageStatus | ''>('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);

  const params = {
    ...(channel ? { channel } : {}),
    ...(status ? { status } : {}),
    ...(from ? { from } : {}),
    ...(to ? { to } : {}),
    page,
    size: PAGE_SIZE,
  };

  const { data, isLoading } = useSearchMessages(params);

  const handleSearch = () => setPage(0);

  const items = data ?? [];
  const hasNext = items.length === PAGE_SIZE;

  return (
    <div className="space-y-4">
      {/* 필터 */}
      <div className="rounded-xl border border-slate-200 bg-white p-4 space-y-3">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-slate-500">채널</label>
            <select
              value={channel}
              onChange={(e) => setChannel(e.target.value as MessageChannel | '')}
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400"
            >
              <option value="">전체</option>
              <option value="EMAIL">이메일</option>
              <option value="SMS">SMS</option>
              <option value="PUSH">푸시 알림</option>
              <option value="SLACK">Slack</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-slate-500">상태</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as MessageStatus | '')}
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400"
            >
              <option value="">전체</option>
              <option value="SUCCESS">성공</option>
              <option value="FAILED">실패</option>
              <option value="PENDING">대기 중</option>
              <option value="PARTIAL">부분 성공</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-slate-500">시작일</label>
            <Input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-slate-500">종료일</label>
            <Input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
        </div>

        <div className="flex justify-end">
          <Button size="sm" onClick={handleSearch}>
            검색
          </Button>
        </div>
      </div>

      {/* 결과 테이블 */}
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center py-12 text-sm text-slate-500">
            검색 중...
          </div>
        ) : items.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 py-16 text-sm text-slate-400">
            <span>검색 결과가 없습니다.</span>
            <span className="text-xs">필터 조건을 변경해보세요.</span>
          </div>
        ) : (
          <>
            <DataTable>
              <DataTableHeader>
                <tr>
                  <DataTableHeaderCell>메시지 ID</DataTableHeaderCell>
                  <DataTableHeaderCell>채널</DataTableHeaderCell>
                  <DataTableHeaderCell>수신자</DataTableHeaderCell>
                  <DataTableHeaderCell>상태</DataTableHeaderCell>
                  <DataTableHeaderCell>응답 시간</DataTableHeaderCell>
                  <DataTableHeaderCell>발송 시각</DataTableHeaderCell>
                </tr>
              </DataTableHeader>
              <DataTableBody>
                {items.map((msg) => {
                  const statusInfo = STATUS_MAP[msg.status];
                  return (
                    <DataTableRow key={msg.messageId}>
                      <DataTableCell className="font-mono text-xs text-left">
                        {msg.messageId}
                      </DataTableCell>
                      <DataTableCell>
                        {CHANNEL_LABEL[msg.channel] ?? msg.channel}
                      </DataTableCell>
                      <DataTableCell className="text-left">{msg.recipient}</DataTableCell>
                      <DataTableCell>
                        <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                      </DataTableCell>
                      <DataTableCell>
                        {msg.responseTimeMs !== undefined ? `${msg.responseTimeMs}ms` : '-'}
                      </DataTableCell>
                      <DataTableCell>
                        {format(new Date(msg.sentAt), 'yyyy.MM.dd HH:mm', { locale: ko })}
                      </DataTableCell>
                    </DataTableRow>
                  );
                })}
              </DataTableBody>
            </DataTable>

            <DataTableFooter className="px-4 py-3 border-t border-slate-100">
              <span className="text-xs text-slate-500">
                {items.length}건
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  이전
                </Button>
                <span className="text-xs text-slate-500">
                  {page + 1}페이지
                </span>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={!hasNext}
                  onClick={() => setPage((p) => p + 1)}
                >
                  다음
                </Button>
              </div>
            </DataTableFooter>
          </>
        )}
      </div>
    </div>
  );
}

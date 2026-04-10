'use client';

import { useState } from 'react';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Pagination } from '@/components/ui/pagination';
import {
  DataTable,
  DataTableHeader,
  DataTableHeaderCell,
  DataTableBody,
  DataTableRow,
  DataTableCell,
} from '@/components/ui/data-table';
import { useSearchMessages } from '../model/useSearchMessages';
import type { MessageChannel, MessageStatus, MessageSearchItem } from '@/entities/message';

// ── Constants ────────────────────────────────────────────────────────────────

const STATUS_MAP: Record<MessageStatus, { label: string; variant: 'success' | 'destructive' | 'warning' | 'neutral' }> = {
  SUCCESS: { label: '성공', variant: 'success' },
  FAILED: { label: '실패', variant: 'destructive' },
  PENDING: { label: '처리중', variant: 'warning' },
  PARTIAL: { label: '부분 성공', variant: 'warning' },
};

const CHANNEL_LABEL: Record<string, string> = {
  SMS: 'SMS',
  LMS: 'LMS',
  MMS: 'MMS',
};

const DATE_RANGE_OPTIONS = [
  { value: '7d', label: '최근 7일' },
  { value: '30d', label: '최근 30일' },
  { value: '90d', label: '최근 90일' },
  { value: 'all', label: '전체' },
];

const PAGE_SIZE = 10;

// ── Types ────────────────────────────────────────────────────────────────────

interface FilterState {
  dateRange: string;
  status: MessageStatus | '';
  channel: MessageChannel | '';
  recipient: string;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function buildDateParams(dateRange: string): { from?: string; to?: string } {
  if (dateRange === 'all') return {};
  const days = dateRange === '7d' ? 7 : dateRange === '30d' ? 30 : 90;
  const from = new Date();
  from.setDate(from.getDate() - days);
  return { from: from.toISOString().slice(0, 10) };
}

function getActiveFilters(filter: FilterState): { key: string; label: string }[] {
  const active: { key: string; label: string }[] = [];
  if (filter.dateRange !== 'all') {
    const opt = DATE_RANGE_OPTIONS.find((o) => o.value === filter.dateRange);
    if (opt) active.push({ key: 'dateRange', label: `기간: ${opt.label}` });
  }
  if (filter.status) {
    active.push({ key: 'status', label: `상태: ${STATUS_MAP[filter.status]?.label ?? filter.status}` });
  }
  if (filter.channel) {
    active.push({ key: 'channel', label: `채널: ${CHANNEL_LABEL[filter.channel] ?? filter.channel}` });
  }
  if (filter.recipient) {
    active.push({ key: 'recipient', label: `수신자: ${filter.recipient}` });
  }
  return active;
}

// ── Main Component ────────────────────────────────────────────────────────────

export function MessageSearchPanel() {
  const [filter, setFilter] = useState<FilterState>({
    dateRange: '7d',
    status: '',
    channel: '',
    recipient: '',
  });
  const [appliedFilter, setAppliedFilter] = useState<FilterState>(filter);
  const [page, setPage] = useState(1);

  const dateParams = buildDateParams(appliedFilter.dateRange);
  const params = {
    ...(appliedFilter.channel ? { channel: appliedFilter.channel } : {}),
    ...(appliedFilter.status ? { status: appliedFilter.status } : {}),
    ...dateParams,
    page: page - 1,
    size: PAGE_SIZE,
  };

  const { data, isLoading } = useSearchMessages(params);
  const items: MessageSearchItem[] = data ?? [];

  const handleSearch = () => {
    setAppliedFilter(filter);
    setPage(1);
  };

  const handleReset = () => {
    const defaultFilter: FilterState = { dateRange: '7d', status: '', channel: '', recipient: '' };
    setFilter(defaultFilter);
    setAppliedFilter(defaultFilter);
    setPage(1);
  };

  const handleRemoveFilter = (key: string) => {
    const next = { ...appliedFilter };
    if (key === 'dateRange') next.dateRange = 'all';
    if (key === 'status') next.status = '';
    if (key === 'channel') next.channel = '';
    if (key === 'recipient') next.recipient = '';
    setAppliedFilter(next);
    setFilter(next);
    setPage(1);
  };

  const activeFilters = getActiveFilters(appliedFilter);

  // Derive total from items length heuristic (real totalCount would come from API response)
  // For now use items length; totalPages is calculated from that
  const totalItems = items.length < PAGE_SIZE ? (page - 1) * PAGE_SIZE + items.length : page * PAGE_SIZE + 1;
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE));

  return (
    <div className="space-y-4">
      {/* ── Mobile filter accordion ── */}
      <div className="md:hidden">
        <div className="border border-slate-200 bg-white">
          {/* 검색 조건 header */}
          <div className="px-4 pt-4 pb-2">
            <h2 className="text-[13px] font-semibold text-slate-900">검색 조건</h2>
          </div>

          {/* Row 1: 기간 + 상태 */}
          <div className="flex gap-3 px-4 pb-3">
            <div className="flex-1 space-y-1">
              <label className="text-[11px] font-medium text-slate-500">기간</label>
              <select
                value={filter.dateRange}
                onChange={(e) => setFilter((f) => ({ ...f, dateRange: e.target.value }))}
                className="w-full rounded-md border border-slate-200 bg-slate-50 px-2 py-2 text-[12px] text-slate-900 outline-none"
              >
                {DATE_RANGE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
            <div className="flex-1 space-y-1">
              <label className="text-[11px] font-medium text-slate-500">상태</label>
              <select
                value={filter.status}
                onChange={(e) => setFilter((f) => ({ ...f, status: e.target.value as MessageStatus | '' }))}
                className="w-full rounded-md border border-slate-200 bg-slate-50 px-2 py-2 text-[12px] text-slate-900 outline-none"
              >
                <option value="">전체</option>
                <option value="SUCCESS">성공</option>
                <option value="FAILED">실패</option>
                <option value="PENDING">처리중</option>
                <option value="PARTIAL">부분 성공</option>
              </select>
            </div>
          </div>

          {/* Row 2: 채널 */}
          <div className="px-4 pb-3 space-y-1">
            <label className="text-[11px] font-medium text-slate-500">채널</label>
            <select
              value={filter.channel}
              onChange={(e) => setFilter((f) => ({ ...f, channel: e.target.value as MessageChannel | '' }))}
              className="w-full rounded-md border border-slate-200 bg-slate-50 px-2 py-2 text-[12px] text-slate-900 outline-none"
            >
              <option value="">전체</option>
              <option value="SMS">SMS</option>
              <option value="LMS">LMS</option>
              <option value="MMS">MMS</option>
            </select>
          </div>

          {/* Row 3: 버튼 */}
          <div className="flex gap-3 px-4 pb-4">
            <button
              onClick={handleSearch}
              className="flex-1 rounded-lg bg-blue-600 py-2 text-[13px] font-semibold text-white"
            >
              검색
            </button>
            <button
              onClick={handleReset}
              className="flex-1 rounded-lg border border-slate-200 bg-white py-2 text-[13px] text-slate-500"
            >
              초기화
            </button>
          </div>
        </div>

        {/* Mobile result count */}
        {!isLoading && (
          <p className="mt-3 px-1 text-[12px] text-slate-500">총 {items.length}건</p>
        )}

        {/* Mobile card list */}
        {isLoading ? (
          <MobileLoadingSkeleton />
        ) : items.length === 0 ? (
          <MobileEmptyState onReset={handleReset} />
        ) : (
          <div className="mt-2 space-y-3">
            {items.map((msg) => (
              <MobileMessageCard key={msg.messageId} msg={msg} />
            ))}
          </div>
        )}
      </div>

      {/* ── PC layout ── */}
      <div className="hidden md:block space-y-4">
        {/* Filter bar card */}
        <div className="rounded-xl border border-slate-200 bg-white px-6 py-4 space-y-3">
          {/* Filter row */}
          <div className="flex flex-wrap items-end gap-4">
            {/* 기간 */}
            <div className="space-y-1">
              <label className="text-[11px] font-medium text-slate-500">기간</label>
              <select
                value={filter.dateRange}
                onChange={(e) => setFilter((f) => ({ ...f, dateRange: e.target.value }))}
                className="w-[200px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 outline-none focus:border-blue-400"
              >
                {DATE_RANGE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>

            {/* 상태 */}
            <div className="space-y-1">
              <label className="text-[11px] font-medium text-slate-500">상태</label>
              <select
                value={filter.status}
                onChange={(e) => setFilter((f) => ({ ...f, status: e.target.value as MessageStatus | '' }))}
                className="w-[120px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 outline-none focus:border-blue-400"
              >
                <option value="">전체</option>
                <option value="SUCCESS">성공</option>
                <option value="FAILED">실패</option>
                <option value="PENDING">처리중</option>
                <option value="PARTIAL">부분 성공</option>
              </select>
            </div>

            {/* 채널 */}
            <div className="space-y-1">
              <label className="text-[11px] font-medium text-slate-500">채널</label>
              <select
                value={filter.channel}
                onChange={(e) => setFilter((f) => ({ ...f, channel: e.target.value as MessageChannel | '' }))}
                className="w-[120px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 outline-none focus:border-blue-400"
              >
                <option value="">전체</option>
                <option value="SMS">SMS</option>
                <option value="LMS">LMS</option>
                <option value="MMS">MMS</option>
              </select>
            </div>

            {/* 수신자 검색 */}
            <div className="space-y-1">
              <label className="text-[11px] font-medium text-slate-500">수신자 검색</label>
              <input
                type="text"
                placeholder="수신자 이메일 또는 ID"
                value={filter.recipient}
                onChange={(e) => setFilter((f) => ({ ...f, recipient: e.target.value }))}
                className="w-[180px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 placeholder:text-slate-400 outline-none focus:border-blue-400"
              />
            </div>

            {/* 검색 버튼 */}
            <Button size="sm" onClick={handleSearch} className="mb-0.5">
              검색
            </Button>
          </div>

          {/* Active filter chips */}
          {activeFilters.length > 0 && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-[11px] text-slate-400">적용된 필터:</span>
              {activeFilters.map((f) => (
                <button
                  key={f.key}
                  onClick={() => handleRemoveFilter(f.key)}
                  className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] text-blue-600 hover:bg-blue-100"
                >
                  {f.label} ×
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Table card */}
        <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
          {/* Table header info */}
          <div className="px-6 pt-5 pb-3">
            <h2 className="text-[14px] font-semibold text-slate-900">검색 결과</h2>
            {!isLoading && (
              <p className="mt-0.5 text-[12px] text-slate-500">총 {items.length}건</p>
            )}
          </div>

          {isLoading ? (
            <PcLoadingSkeleton />
          ) : items.length === 0 ? (
            <PcEmptyState onReset={handleReset} />
          ) : (
            <>
              <DataTable>
                <DataTableHeader>
                  <tr>
                    <DataTableHeaderCell className="text-left">메시지 ID</DataTableHeaderCell>
                    <DataTableHeaderCell>채널</DataTableHeaderCell>
                    <DataTableHeaderCell className="text-left">수신자</DataTableHeaderCell>
                    <DataTableHeaderCell>상태</DataTableHeaderCell>
                    <DataTableHeaderCell>응답시간</DataTableHeaderCell>
                    <DataTableHeaderCell>발송 시각</DataTableHeaderCell>
                    <DataTableHeaderCell>{/* 상세 */}</DataTableHeaderCell>
                  </tr>
                </DataTableHeader>
                <DataTableBody>
                  {items.map((msg) => {
                    const statusInfo = STATUS_MAP[msg.status];
                    return (
                      <DataTableRow key={msg.messageId}>
                        <DataTableCell className="text-left font-mono text-xs text-slate-500">
                          {msg.messageId}
                        </DataTableCell>
                        <DataTableCell>
                          <Badge variant="info">{CHANNEL_LABEL[msg.channel] ?? msg.channel}</Badge>
                        </DataTableCell>
                        <DataTableCell className="text-left text-[13px]">{msg.recipient}</DataTableCell>
                        <DataTableCell>
                          <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                        </DataTableCell>
                        <DataTableCell className="text-[13px]">
                          {msg.responseTimeMs !== undefined ? `${msg.responseTimeMs}ms` : '-'}
                        </DataTableCell>
                        <DataTableCell className="text-[12px] text-slate-500">
                          {format(new Date(msg.sentAt), 'MM-dd HH:mm:ss', { locale: ko })}
                        </DataTableCell>
                        <DataTableCell>
                          <button className="rounded-md border border-slate-200 bg-slate-50 px-3 py-1.5 text-[12px] text-slate-500 hover:bg-slate-100">
                            상세
                          </button>
                        </DataTableCell>
                      </DataTableRow>
                    );
                  })}
                </DataTableBody>
              </DataTable>

              {/* Pagination */}
              <div className="border-t border-slate-100 px-6 py-3">
                <Pagination
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={setPage}
                  totalCount={items.length}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Sub-components ────────────────────────────────────────────────────────────

function MobileMessageCard({ msg }: { msg: MessageSearchItem }) {
  const statusInfo = STATUS_MAP[msg.status];
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-3 py-3">
      {/* Top row: msgId + status badge */}
      <div className="flex items-center justify-between">
        <span className="font-mono text-[11px] text-slate-500">{msg.messageId}</span>
        <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
      </div>
      {/* Middle row: channel badge + recipient */}
      <div className="mt-2 flex items-center gap-2">
        <Badge variant="info">{CHANNEL_LABEL[msg.channel] ?? msg.channel}</Badge>
        <span className="text-[12px] text-slate-900">{msg.recipient}</span>
      </div>
      {/* Bottom row: sentAt + responseTime + 상세 link */}
      <div className="mt-2 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-[11px] text-slate-400">
            {format(new Date(msg.sentAt), 'MM-dd HH:mm', { locale: ko })}
          </span>
          <span className="text-[11px] text-slate-400">
            응답: {msg.responseTimeMs !== undefined ? `${msg.responseTimeMs}ms` : '-'}
          </span>
        </div>
        <span className="text-[11px] font-medium text-blue-600">상세 &gt;</span>
      </div>
    </div>
  );
}

function MobileLoadingSkeleton() {
  return (
    <div className="mt-2 space-y-3 animate-pulse">
      {[...Array(3)].map((_, i) => (
        <div key={i} className="h-20 rounded-xl bg-slate-100" />
      ))}
    </div>
  );
}

function MobileEmptyState({ onReset }: { onReset: () => void }) {
  return (
    <div className="mt-6 flex flex-col items-center gap-2 py-10 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-slate-100">
        <span className="text-xl text-slate-400">🔍</span>
      </div>
      <p className="text-[13px] text-slate-500">검색 결과가 없습니다.</p>
      <p className="text-[12px] text-slate-400">조건을 변경하거나 초기화 후 다시 검색해보세요</p>
      <button
        onClick={onReset}
        className="mt-2 rounded-lg border border-slate-200 px-4 py-2 text-[12px] text-slate-600 hover:bg-slate-50"
      >
        필터 초기화
      </button>
    </div>
  );
}

function PcLoadingSkeleton() {
  return (
    <div className="px-6 pb-6 space-y-3 animate-pulse">
      {[...Array(5)].map((_, i) => (
        <div key={i} className="h-14 rounded-lg bg-slate-100" />
      ))}
    </div>
  );
}

function PcEmptyState({ onReset }: { onReset: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 py-20 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-slate-100">
        <span className="text-2xl text-slate-400">🔍</span>
      </div>
      <p className="text-[14px] text-slate-500">검색 결과가 없습니다.</p>
      <p className="text-[13px] text-slate-400">조건을 변경하거나 초기화 후 다시 검색해보세요</p>
      <button
        onClick={onReset}
        className="mt-1 rounded-lg border border-slate-200 px-5 py-2 text-[13px] text-slate-600 hover:bg-slate-50"
      >
        필터 초기화
      </button>
    </div>
  );
}

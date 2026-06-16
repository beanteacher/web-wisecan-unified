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
import { useSendHistoryList } from '../model/useSendHistoryList';
import { useSendHistoryDetail } from '../model/useSendHistoryDetail';
import type {
  SendChannel,
  SendRequestStatus,
  SendHistoryListItem,
} from '@/entities/send-history';

// ── 상수 ────────────────────────────────────────────────────────────────────

const STATUS_MAP: Record<
  SendRequestStatus,
  { label: string; variant: 'success' | 'destructive' | 'warning' | 'neutral' }
> = {
  PENDING:   { label: '대기중',  variant: 'warning' },
  QUEUED:    { label: '전송됨',  variant: 'success' },
  FAILED:    { label: '실패',    variant: 'destructive' },
  CANCELLED: { label: '취소됨', variant: 'neutral' },
};

const CHANNEL_LABEL: Record<SendChannel, string> = {
  SMS:   'SMS',
  LMS:   'LMS',
  MMS:   'MMS',
  KAKAO: '카카오',
  RCS:   'RCS',
};

const DATE_RANGE_OPTIONS = [
  { value: '7d',  label: '최근 7일' },
  { value: '30d', label: '최근 30일' },
  { value: '90d', label: '최근 90일' },
  { value: 'all', label: '전체' },
];

const PAGE_SIZE = 20;

// ── 타입 ────────────────────────────────────────────────────────────────────

interface FilterState {
  dateRange: string;
  channel: SendChannel | '';
  status: SendRequestStatus | '';
  callbackNumber: string;
  recipientNumber: string;
  scopeMember: boolean;
}

// ── 헬퍼 ────────────────────────────────────────────────────────────────────

function buildDateParams(dateRange: string): { fromDate?: string; toDate?: string } {
  if (dateRange === 'all') return {};
  const days = dateRange === '7d' ? 7 : dateRange === '30d' ? 30 : 90;
  const from = new Date();
  from.setDate(from.getDate() - days);
  return { fromDate: from.toISOString().slice(0, 19) };
}

function formatCost(cost: number): string {
  return cost.toLocaleString('ko-KR') + '원';
}

// ── 메인 컴포넌트 ────────────────────────────────────────────────────────────

export function SendHistoryPanel() {
  const [filter, setFilter] = useState<FilterState>({
    dateRange: '7d',
    channel: '',
    status: '',
    callbackNumber: '',
    recipientNumber: '',
    scopeMember: false,
  });
  const [appliedFilter, setAppliedFilter] = useState<FilterState>(filter);
  const [page, setPage] = useState(0);
  const [selectedSendId, setSelectedSendId] = useState<string | null>(null);

  const dateParams = buildDateParams(appliedFilter.dateRange);
  const queryParams = {
    ...dateParams,
    ...(appliedFilter.channel ? { channel: appliedFilter.channel } : {}),
    ...(appliedFilter.status ? { status: appliedFilter.status } : {}),
    ...(appliedFilter.callbackNumber ? { callbackNumber: appliedFilter.callbackNumber } : {}),
    ...(appliedFilter.recipientNumber ? { recipientNumber: appliedFilter.recipientNumber } : {}),
    scopeMember: appliedFilter.scopeMember,
    page,
    size: PAGE_SIZE,
  };

  const { data, isLoading } = useSendHistoryList(queryParams);
  const items: SendHistoryListItem[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;
  const totalElements = data?.totalElements ?? 0;

  const { data: detail, isLoading: detailLoading } = useSendHistoryDetail(selectedSendId);

  const handleSearch = () => {
    setAppliedFilter(filter);
    setPage(0);
    setSelectedSendId(null);
  };

  const handleReset = () => {
    const def: FilterState = {
      dateRange: '7d', channel: '', status: '',
      callbackNumber: '', recipientNumber: '', scopeMember: false,
    };
    setFilter(def);
    setAppliedFilter(def);
    setPage(0);
    setSelectedSendId(null);
  };

  return (
    <div className="space-y-4">
      {/* ── 필터 영역 ── */}
      <div className="rounded-xl border border-slate-200 bg-white px-6 py-4 space-y-3">
        <div className="flex flex-wrap items-end gap-4">
          {/* 기간 */}
          <div className="space-y-1">
            <label className="text-[11px] font-medium text-slate-500">기간</label>
            <select
              value={filter.dateRange}
              onChange={(e) => setFilter((f) => ({ ...f, dateRange: e.target.value }))}
              className="w-[160px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 outline-none focus:border-blue-400"
            >
              {DATE_RANGE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </div>

          {/* 채널 */}
          <div className="space-y-1">
            <label className="text-[11px] font-medium text-slate-500">채널</label>
            <select
              value={filter.channel}
              onChange={(e) => setFilter((f) => ({ ...f, channel: e.target.value as SendChannel | '' }))}
              className="w-[120px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 outline-none focus:border-blue-400"
            >
              <option value="">전체</option>
              <option value="SMS">SMS</option>
              <option value="LMS">LMS</option>
              <option value="MMS">MMS</option>
              <option value="KAKAO">카카오</option>
              <option value="RCS">RCS</option>
            </select>
          </div>

          {/* 상태 */}
          <div className="space-y-1">
            <label className="text-[11px] font-medium text-slate-500">상태</label>
            <select
              value={filter.status}
              onChange={(e) => setFilter((f) => ({ ...f, status: e.target.value as SendRequestStatus | '' }))}
              className="w-[120px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 outline-none focus:border-blue-400"
            >
              <option value="">전체</option>
              <option value="QUEUED">전송됨</option>
              <option value="PENDING">대기중</option>
              <option value="FAILED">실패</option>
              <option value="CANCELLED">취소됨</option>
            </select>
          </div>

          {/* 발신번호 */}
          <div className="space-y-1">
            <label className="text-[11px] font-medium text-slate-500">발신번호</label>
            <input
              type="text"
              placeholder="01012345678"
              value={filter.callbackNumber}
              onChange={(e) => setFilter((f) => ({ ...f, callbackNumber: e.target.value }))}
              className="w-[140px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 placeholder:text-slate-400 outline-none focus:border-blue-400"
            />
          </div>

          {/* 수신번호 */}
          <div className="space-y-1">
            <label className="text-[11px] font-medium text-slate-500">수신번호 포함</label>
            <input
              type="text"
              placeholder="01099999999"
              value={filter.recipientNumber}
              onChange={(e) => setFilter((f) => ({ ...f, recipientNumber: e.target.value }))}
              className="w-[140px] rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] text-slate-900 placeholder:text-slate-400 outline-none focus:border-blue-400"
            />
          </div>

          {/* 검색 버튼 */}
          <Button size="sm" onClick={handleSearch} className="mb-0.5">
            검색
          </Button>
          <button
            onClick={handleReset}
            className="mb-0.5 rounded-md border border-slate-200 bg-white px-3 py-1.5 text-[12px] text-slate-500 hover:bg-slate-50"
          >
            초기화
          </button>
        </div>

        {/* scope:member 토글 */}
        <div className="flex items-center gap-2 pt-1 border-t border-slate-100">
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={filter.scopeMember}
              onChange={(e) => setFilter((f) => ({ ...f, scopeMember: e.target.checked }))}
              className="h-3.5 w-3.5 accent-blue-600"
            />
            <span className="text-[11px] text-slate-500">
              scope:member — 내 모든 키 이력 조회
              <span className="ml-1 text-[10px] text-amber-500">(history:read 스코프 필요)</span>
            </span>
          </label>
        </div>
      </div>

      {/* ── 목록 + 상세 레이아웃 ── */}
      <div className={selectedSendId ? 'flex gap-4' : ''}>
        {/* 목록 테이블 */}
        <div className={`rounded-xl border border-slate-200 bg-white overflow-hidden ${selectedSendId ? 'flex-1 min-w-0' : 'w-full'}`}>
          <div className="px-6 pt-5 pb-3 flex items-center justify-between">
            <div>
              <h2 className="text-[14px] font-semibold text-slate-900">발송 이력</h2>
              {!isLoading && (
                <p className="mt-0.5 text-[12px] text-slate-500">총 {totalElements.toLocaleString()}건</p>
              )}
            </div>
            {appliedFilter.scopeMember && (
              <span className="text-[11px] rounded-full bg-amber-50 px-2 py-0.5 text-amber-600 border border-amber-200">
                scope:member 적용 중
              </span>
            )}
          </div>

          {isLoading ? (
            <ListSkeleton />
          ) : items.length === 0 ? (
            <EmptyState onReset={handleReset} />
          ) : (
            <>
              <DataTable>
                <DataTableHeader>
                  <tr>
                    <DataTableHeaderCell className="text-left">발송 ID</DataTableHeaderCell>
                    <DataTableHeaderCell>채널</DataTableHeaderCell>
                    <DataTableHeaderCell className="text-left">발신번호</DataTableHeaderCell>
                    <DataTableHeaderCell>수신 수</DataTableHeaderCell>
                    <DataTableHeaderCell>상태</DataTableHeaderCell>
                    <DataTableHeaderCell>금액</DataTableHeaderCell>
                    <DataTableHeaderCell>발송 일시</DataTableHeaderCell>
                    <DataTableHeaderCell>{/* 상세 */}</DataTableHeaderCell>
                  </tr>
                </DataTableHeader>
                <DataTableBody>
                  {items.map((item) => {
                    const statusInfo = STATUS_MAP[item.status];
                    const isSelected = item.sendId === selectedSendId;
                    return (
                      <DataTableRow
                        key={item.sendId}
                        className={isSelected ? 'bg-blue-50' : undefined}
                      >
                        <DataTableCell className="text-left font-mono text-xs text-slate-500">
                          {item.sendId.slice(0, 12)}…
                        </DataTableCell>
                        <DataTableCell>
                          <Badge variant="info">{CHANNEL_LABEL[item.channel] ?? item.channel}</Badge>
                        </DataTableCell>
                        <DataTableCell className="text-left text-[13px]">
                          {item.callbackNumber}
                        </DataTableCell>
                        <DataTableCell className="text-[13px]">
                          {item.recipientCount.toLocaleString()}명
                        </DataTableCell>
                        <DataTableCell>
                          <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                        </DataTableCell>
                        <DataTableCell className="text-[13px]">
                          {formatCost(item.totalCost)}
                        </DataTableCell>
                        <DataTableCell className="text-[12px] text-slate-500">
                          {format(new Date(item.requestedAt), 'MM-dd HH:mm:ss', { locale: ko })}
                        </DataTableCell>
                        <DataTableCell>
                          <button
                            onClick={() =>
                              setSelectedSendId(isSelected ? null : item.sendId)
                            }
                            className={`rounded-md border px-3 py-1.5 text-[12px] transition-colors ${
                              isSelected
                                ? 'border-blue-300 bg-blue-50 text-blue-600'
                                : 'border-slate-200 bg-slate-50 text-slate-500 hover:bg-slate-100'
                            }`}
                          >
                            {isSelected ? '닫기' : '상세'}
                          </button>
                        </DataTableCell>
                      </DataTableRow>
                    );
                  })}
                </DataTableBody>
              </DataTable>

              <div className="border-t border-slate-100 px-6 py-3">
                <Pagination
                  currentPage={page + 1}
                  totalPages={totalPages}
                  onPageChange={(p) => { setPage(p - 1); setSelectedSendId(null); }}
                  totalCount={Number(totalElements)}
                />
              </div>
            </>
          )}
        </div>

        {/* 상세 패널 */}
        {selectedSendId && (
          <div className="w-80 shrink-0 rounded-xl border border-slate-200 bg-white p-5 self-start">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[13px] font-semibold text-slate-900">상세 정보</h3>
              <button
                onClick={() => setSelectedSendId(null)}
                className="text-[12px] text-slate-400 hover:text-slate-600"
              >
                닫기 ×
              </button>
            </div>

            {detailLoading ? (
              <DetailSkeleton />
            ) : detail ? (
              <DetailContent detail={detail} />
            ) : (
              <p className="text-[12px] text-slate-400">불러오는 중…</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ── 상세 컨텐츠 ──────────────────────────────────────────────────────────────

import type { SendHistoryDetail } from '@/entities/send-history';

function DetailContent({ detail }: { detail: SendHistoryDetail }) {
  const statusInfo = STATUS_MAP[detail.status];
  return (
    <dl className="space-y-3 text-[12px]">
      <DetailRow label="발송 ID">
        <span className="font-mono text-[11px] break-all text-slate-600">{detail.sendId}</span>
      </DetailRow>
      <DetailRow label="채널">
        <Badge variant="info">{CHANNEL_LABEL[detail.channel] ?? detail.channel}</Badge>
      </DetailRow>
      <DetailRow label="상태">
        <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
      </DetailRow>
      <DetailRow label="발신번호">{detail.callbackNumber}</DetailRow>
      <DetailRow label="수신자 수">{detail.recipientCount.toLocaleString()}명</DetailRow>
      {detail.subject && <DetailRow label="제목">{detail.subject}</DetailRow>}
      <DetailRow label="본문">
        <span className="whitespace-pre-wrap text-slate-600 text-[11px]">
          {detail.messageBody.length > 100
            ? detail.messageBody.slice(0, 100) + '…'
            : detail.messageBody}
        </span>
      </DetailRow>
      <DetailRow label="단가">{detail.unitCost.toLocaleString()}원</DetailRow>
      <DetailRow label="총 금액">{detail.totalCost.toLocaleString()}원</DetailRow>
      {detail.failReason && (
        <DetailRow label="실패 사유">
          <span className="text-red-500">{detail.failReason}</span>
        </DetailRow>
      )}
      <DetailRow label="발송 일시">
        {format(new Date(detail.requestedAt), 'yyyy-MM-dd HH:mm:ss', { locale: ko })}
      </DetailRow>
      <DetailRow label="적재 일시">
        {format(new Date(detail.createdAt), 'yyyy-MM-dd HH:mm:ss', { locale: ko })}
      </DetailRow>
    </dl>
  );
}

function DetailRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">{label}</dt>
      <dd className="text-slate-800">{children}</dd>
    </div>
  );
}

// ── 스켈레톤 / 빈 상태 ───────────────────────────────────────────────────────

function ListSkeleton() {
  return (
    <div className="px-6 pb-6 space-y-3 animate-pulse">
      {[...Array(5)].map((_, i) => (
        <div key={i} className="h-14 rounded-lg bg-slate-100" />
      ))}
    </div>
  );
}

function DetailSkeleton() {
  return (
    <div className="space-y-3 animate-pulse">
      {[...Array(6)].map((_, i) => (
        <div key={i} className="h-8 rounded bg-slate-100" />
      ))}
    </div>
  );
}

function EmptyState({ onReset }: { onReset: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 py-20 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-slate-100">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="1.5">
          <path d="M9 12h6M9 16h6M9 8h6M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" strokeLinecap="round"/>
        </svg>
      </div>
      <p className="text-[14px] text-slate-500">발송 이력이 없습니다.</p>
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

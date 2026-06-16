'use client';

import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { Spinner } from '@/components/ui/spinner';
import {
  DataTable,
  DataTableHeader,
  DataTableHeaderCell,
  DataTableBody,
  DataTableRow,
  DataTableCell,
} from '@/components/ui/data-table';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { useMyTransfers, useRequestTransfer, useCancelTransfer } from '../model/useTemplateTransfer';
import type { TransferStatus } from '@/entities/template';

// ── 상수 ────────────────────────────────────────────────────────

const STATUS_MAP: Record<
  TransferStatus,
  { label: string; variant: 'success' | 'destructive' | 'warning' | 'neutral' }
> = {
  PENDING:     { label: '대기',     variant: 'warning' },
  IN_PROGRESS: { label: '처리중',   variant: 'warning' },
  COMPLETED:   { label: '완료',     variant: 'success' },
  REJECTED:    { label: '거부',     variant: 'destructive' },
  CANCELLED:   { label: '취소됨',   variant: 'neutral' },
};

// ── 컴포넌트 ────────────────────────────────────────────────────

export function TemplateTransferPanel() {
  const { data: transfers, isLoading } = useMyTransfers();
  const requestMutation = useRequestTransfer();
  const cancelMutation = useCancelTransfer();

  const [showForm, setShowForm] = useState(false);
  const [sourceCode, setSourceCode] = useState('');
  const [reason, setReason] = useState('');

  if (isLoading) return <Spinner />;

  async function handleRequest() {
    if (!sourceCode.trim()) {
      alert('이관할 템플릿 코드를 입력하세요.');
      return;
    }
    await requestMutation.mutateAsync({ sourceTemplateCode: sourceCode.trim(), reason: reason || undefined });
    setShowForm(false);
    setSourceCode('');
    setReason('');
  }

  async function handleCancel(transferId: number) {
    if (!confirm('이관 신청을 취소하시겠습니까?')) return;
    await cancelMutation.mutateAsync(transferId);
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-lg font-semibold">SMS17 이관 신청</h2>
        <Button size="sm" onClick={() => setShowForm((v) => !v)}>
          {showForm ? '취소' : '+ 이관 신청'}
        </Button>
      </div>

      {/* 이관 신청 폼 */}
      {showForm && (
        <div className="border rounded-lg p-4 space-y-3 bg-gray-50">
          <div>
            <label className="block text-sm font-medium mb-1">소스 템플릿 코드 *</label>
            <input
              type="text"
              value={sourceCode}
              onChange={(e) => setSourceCode(e.target.value)}
              placeholder="SMS17의 템플릿 코드를 입력하세요"
              className="w-full border rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">이관 사유</label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="이관 사유를 입력하세요 (선택)"
              rows={2}
              className="w-full border rounded px-3 py-2 text-sm resize-none"
            />
          </div>
          <Button
            size="sm"
            onClick={handleRequest}
            disabled={requestMutation.isPending}
          >
            {requestMutation.isPending ? '신청 중...' : '이관 신청'}
          </Button>
        </div>
      )}

      {/* 이관 신청 목록 */}
      {!transfers || transfers.length === 0 ? (
        <EmptyState
          title="이관 신청 내역이 없습니다"
          description="SMS17에서 WiseCan으로 템플릿을 이관하려면 이관 신청을 해주세요."
        />
      ) : (
        <DataTable>
          <DataTableHeader>
            <DataTableHeaderCell>신청 ID</DataTableHeaderCell>
            <DataTableHeaderCell>소스 템플릿 코드</DataTableHeaderCell>
            <DataTableHeaderCell>상태</DataTableHeaderCell>
            <DataTableHeaderCell>신청일시</DataTableHeaderCell>
            <DataTableHeaderCell>작업</DataTableHeaderCell>
          </DataTableHeader>
          <DataTableBody>
            {transfers.map((t) => {
              const statusInfo = STATUS_MAP[t.status];
              return (
                <DataTableRow key={t.id}>
                  <DataTableCell className="font-mono text-xs">{t.id}</DataTableCell>
                  <DataTableCell className="font-mono text-xs">{t.sourceTemplateCode}</DataTableCell>
                  <DataTableCell>
                    <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                  </DataTableCell>
                  <DataTableCell className="text-xs text-gray-500">
                    {format(new Date(t.requestedAt), 'yyyy.MM.dd HH:mm', { locale: ko })}
                  </DataTableCell>
                  <DataTableCell>
                    {t.status === 'PENDING' && (
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={cancelMutation.isPending}
                        onClick={() => handleCancel(t.id)}
                      >
                        취소
                      </Button>
                    )}
                  </DataTableCell>
                </DataTableRow>
              );
            })}
          </DataTableBody>
        </DataTable>
      )}
    </div>
  );
}

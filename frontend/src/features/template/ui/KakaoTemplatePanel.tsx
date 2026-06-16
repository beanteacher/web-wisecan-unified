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
import { useKakaoTemplates, useDeleteKakaoTemplate } from '../model/useKakaoTemplates';
import type { KakaoTemplate, KakaoInspectionStatus } from '@/entities/template';

// ── 상수 ────────────────────────────────────────────────────────

const INSPECTION_MAP: Record<
  KakaoInspectionStatus,
  { label: string; variant: 'success' | 'destructive' | 'warning' | 'neutral' }
> = {
  REG: { label: '등록',     variant: 'neutral' },
  REQ: { label: '심사중',   variant: 'warning' },
  APR: { label: '승인',     variant: 'success' },
  REJ: { label: '반려',     variant: 'destructive' },
};

// ── 컴포넌트 ────────────────────────────────────────────────────

interface Props {
  onRegisterClick: () => void;
}

export function KakaoTemplatePanel({ onRegisterClick }: Props) {
  const { data: templates, isLoading, isError } = useKakaoTemplates();
  const deleteMutation = useDeleteKakaoTemplate();
  const [deletingCode, setDeletingCode] = useState<string | null>(null);

  if (isLoading) return <Spinner />;
  if (isError) return <p className="text-red-500 text-sm">템플릿 목록을 불러오지 못했습니다.</p>;
  if (!templates || templates.length === 0) {
    return (
      <EmptyState
        title="등록된 카카오 템플릿이 없습니다"
        description="알림톡/친구톡 템플릿을 등록하면 카카오 채널로 발송할 수 있습니다."
        action={<Button onClick={onRegisterClick}>템플릿 등록</Button>}
      />
    );
  }

  async function handleDelete(template: KakaoTemplate) {
    if (!confirm(`"${template.templateName}" 템플릿을 삭제하시겠습니까?`)) return;
    setDeletingCode(template.templateCode);
    try {
      await deleteMutation.mutateAsync(template.templateCode);
    } finally {
      setDeletingCode(null);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-lg font-semibold">카카오 알림톡 템플릿</h2>
        <Button size="sm" onClick={onRegisterClick}>+ 템플릿 등록</Button>
      </div>

      <DataTable>
        <DataTableHeader>
          <DataTableHeaderCell>템플릿 코드</DataTableHeaderCell>
          <DataTableHeaderCell>템플릿명</DataTableHeaderCell>
          <DataTableHeaderCell>유형</DataTableHeaderCell>
          <DataTableHeaderCell>심사 상태</DataTableHeaderCell>
          <DataTableHeaderCell>발송 가능</DataTableHeaderCell>
          <DataTableHeaderCell>작업</DataTableHeaderCell>
        </DataTableHeader>
        <DataTableBody>
          {templates.map((t) => {
            const inspection = t.inspectionStatus
              ? INSPECTION_MAP[t.inspectionStatus]
              : null;
            return (
              <DataTableRow key={t.templateCode}>
                <DataTableCell className="font-mono text-xs">{t.templateCode}</DataTableCell>
                <DataTableCell>{t.templateName}</DataTableCell>
                <DataTableCell>{t.messageType}</DataTableCell>
                <DataTableCell>
                  {inspection ? (
                    <Badge variant={inspection.variant}>{inspection.label}</Badge>
                  ) : '-'}
                </DataTableCell>
                <DataTableCell>
                  <Badge variant={t.sendable ? 'success' : 'neutral'}>
                    {t.sendable ? '가능' : '불가'}
                  </Badge>
                </DataTableCell>
                <DataTableCell>
                  <Button
                    size="sm"
                    variant="destructive"
                    disabled={deletingCode === t.templateCode}
                    onClick={() => handleDelete(t)}
                  >
                    {deletingCode === t.templateCode ? '삭제 중...' : '삭제'}
                  </Button>
                </DataTableCell>
              </DataTableRow>
            );
          })}
        </DataTableBody>
      </DataTable>
    </div>
  );
}

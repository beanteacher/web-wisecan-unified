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
import { useRcsBrands, useRcsTemplates } from '../model/useRcsTemplates';

export function RcsTemplatePanel() {
  const { data: brands, isLoading: brandsLoading } = useRcsBrands();
  const [selectedBrand, setSelectedBrand] = useState<string | null>(null);
  const { data: templates, isLoading: templatesLoading } = useRcsTemplates(selectedBrand);

  if (brandsLoading) return <Spinner />;

  const activeBrand = selectedBrand ?? brands?.[0] ?? null;

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-lg font-semibold">RCS 템플릿</h2>
      </div>

      {/* 브랜드 탭 */}
      {brands && brands.length > 0 ? (
        <div className="flex gap-2 flex-wrap">
          {brands.map((brandId) => (
            <button
              key={brandId}
              onClick={() => setSelectedBrand(brandId)}
              className={`px-3 py-1.5 rounded-full text-sm border transition-colors ${
                activeBrand === brandId
                  ? 'bg-black text-white border-black'
                  : 'bg-white text-gray-700 border-gray-300 hover:border-gray-500'
              }`}
            >
              {brandId}
            </button>
          ))}
        </div>
      ) : (
        <EmptyState
          title="등록된 RCS 브랜드가 없습니다"
          description="RCS 발송을 위해 브랜드(발신 프로필)를 먼저 등록해야 합니다."
        />
      )}

      {/* 선택 브랜드의 템플릿 목록 */}
      {activeBrand && (
        <div>
          {templatesLoading ? (
            <Spinner />
          ) : !templates || templates.length === 0 ? (
            <EmptyState
              title="이 브랜드에 등록된 RCS 템플릿이 없습니다"
              description="RCS 발송 시스템에서 템플릿을 등록한 뒤 확인하세요."
            />
          ) : (
            <DataTable>
              <DataTableHeader>
                <DataTableHeaderCell>템플릿 ID</DataTableHeaderCell>
                <DataTableHeaderCell>템플릿명</DataTableHeaderCell>
                <DataTableHeaderCell>승인 상태</DataTableHeaderCell>
                <DataTableHeaderCell>사용 상태</DataTableHeaderCell>
                <DataTableHeaderCell>발송 가능</DataTableHeaderCell>
              </DataTableHeader>
              <DataTableBody>
                {templates.map((t) => (
                  <DataTableRow key={t.messagebaseId}>
                    <DataTableCell className="font-mono text-xs">{t.messagebaseId}</DataTableCell>
                    <DataTableCell>{t.templateName}</DataTableCell>
                    <DataTableCell>
                      <Badge
                        variant={t.approvalResult === '승인' ? 'success' : 'warning'}
                      >
                        {t.approvalResult ?? '-'}
                      </Badge>
                    </DataTableCell>
                    <DataTableCell>
                      <Badge
                        variant={t.usageStatus === 'READY' ? 'success' : 'neutral'}
                      >
                        {t.usageStatus ?? '-'}
                      </Badge>
                    </DataTableCell>
                    <DataTableCell>
                      <Badge variant={t.sendable ? 'success' : 'neutral'}>
                        {t.sendable ? '가능' : '불가'}
                      </Badge>
                    </DataTableCell>
                  </DataTableRow>
                ))}
              </DataTableBody>
            </DataTable>
          )}
        </div>
      )}
    </div>
  );
}

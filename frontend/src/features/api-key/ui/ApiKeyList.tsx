'use client';

import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import {
  DataTable,
  DataTableHeader,
  DataTableHeaderCell,
  DataTableBody,
  DataTableRow,
  DataTableCell,
} from '@/components/ui/data-table';
import { Button } from '@/components/ui/button';
import { useApiKeys } from '../model/useApiKeys';
import { useRevokeApiKey } from '../model/useRevokeApiKey';
import type { ApiKey } from '@/entities/api-key';

function StatusBadge({ status }: { status: ApiKey['status'] }) {
  return (
    <span
      className={[
        'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
        status === 'ACTIVE'
          ? 'bg-green-50 text-green-700'
          : 'bg-slate-100 text-slate-500',
      ].join(' ')}
    >
      {status === 'ACTIVE' ? '활성' : '비활성'}
    </span>
  );
}

export function ApiKeyList() {
  const { data: keys, isLoading } = useApiKeys();
  const { mutate: revoke, isPending: isRevoking } = useRevokeApiKey();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12 text-sm text-slate-500">
        불러오는 중...
      </div>
    );
  }

  if (!keys || keys.length === 0) {
    return (
      <div className="flex items-center justify-center py-12 text-sm text-slate-500">
        발급된 API 키가 없습니다.
      </div>
    );
  }

  return (
    <DataTable>
      <DataTableHeader>
        <tr>
          <DataTableHeaderCell>이름</DataTableHeaderCell>
          <DataTableHeaderCell>키 접두사</DataTableHeaderCell>
          <DataTableHeaderCell>상태</DataTableHeaderCell>
          <DataTableHeaderCell>마지막 사용</DataTableHeaderCell>
          <DataTableHeaderCell>생성일</DataTableHeaderCell>
          <DataTableHeaderCell>액션</DataTableHeaderCell>
        </tr>
      </DataTableHeader>
      <DataTableBody>
        {keys.map((key) => (
          <DataTableRow key={key.id}>
            <DataTableCell className="text-left font-medium">{key.keyName}</DataTableCell>
            <DataTableCell className="font-mono text-xs">{key.keyPrefix}...</DataTableCell>
            <DataTableCell><StatusBadge status={key.status} /></DataTableCell>
            <DataTableCell>
              {key.lastUsedAt
                ? format(new Date(key.lastUsedAt), 'yyyy.MM.dd', { locale: ko })
                : '-'}
            </DataTableCell>
            <DataTableCell>
              {format(new Date(key.createdAt), 'yyyy.MM.dd', { locale: ko })}
            </DataTableCell>
            <DataTableCell>
              {key.status === 'ACTIVE' && (
                <Button
                  variant="outline-destructive"
                  size="sm"
                  loading={isRevoking}
                  onClick={() => revoke(key.id)}
                >
                  비활성화
                </Button>
              )}
            </DataTableCell>
          </DataTableRow>
        ))}
      </DataTableBody>
    </DataTable>
  );
}

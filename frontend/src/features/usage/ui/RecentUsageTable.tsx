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
import { useUsageHistory } from '../model/useUsageHistory';
import type { HistoryItem } from '@/entities/usage';

function StatusBadge({ status }: { status: HistoryItem['status'] }) {
  return (
    <span
      className={[
        'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
        status === 'SUCCESS'
          ? 'bg-green-50 text-green-700'
          : 'bg-red-50 text-red-600',
      ].join(' ')}
    >
      {status === 'SUCCESS' ? '성공' : '실패'}
    </span>
  );
}

export function RecentUsageTable() {
  const { data: history, isLoading } = useUsageHistory();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12 text-sm text-slate-500">
        불러오는 중...
      </div>
    );
  }

  if (!history || history.length === 0) {
    return (
      <div className="flex items-center justify-center py-12 text-sm text-slate-500">
        이력 없음
      </div>
    );
  }

  return (
    <DataTable>
      <DataTableHeader>
        <tr>
          <DataTableHeaderCell>엔드포인트</DataTableHeaderCell>
          <DataTableHeaderCell>상태</DataTableHeaderCell>
          <DataTableHeaderCell>호출 시각</DataTableHeaderCell>
        </tr>
      </DataTableHeader>
      <DataTableBody>
        {history.slice(0, 10).map((item) => (
          <DataTableRow key={item.id}>
            <DataTableCell className="text-left font-mono text-xs">{item.endpoint}</DataTableCell>
            <DataTableCell><StatusBadge status={item.status} /></DataTableCell>
            <DataTableCell>
              {format(new Date(item.calledAt), 'yyyy.MM.dd HH:mm', { locale: ko })}
            </DataTableCell>
          </DataTableRow>
        ))}
      </DataTableBody>
    </DataTable>
  );
}

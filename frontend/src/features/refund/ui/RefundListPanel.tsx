'use client';

import { useState } from 'react';
import { useRefunds } from '../model/useRefunds';
import { useCancelRefund } from '../model/useCancelRefund';
import {
  PAYMENT_METHOD_LABEL,
  REFUND_STATUS_LABEL,
  REFUND_STATUS_COLOR,
  type RefundResponse,
} from '@/entities/refund';

function formatAmount(amount: number) {
  return amount.toLocaleString('ko-KR') + '원';
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

const STATUS_BADGE_CLASS: Record<string, string> = {
  orange: 'bg-orange-100 text-orange-700',
  blue: 'bg-blue-100 text-blue-700',
  red: 'bg-red-100 text-red-700',
  gray: 'bg-gray-100 text-gray-500',
  green: 'bg-green-100 text-green-700',
};

function StatusBadge({ status }: { status: RefundResponse['status'] }) {
  const color = REFUND_STATUS_COLOR[status];
  const label = REFUND_STATUS_LABEL[status];
  return (
    <span className={`inline-flex items-center rounded-md px-2.5 py-0.5 text-xs font-semibold ${STATUS_BADGE_CLASS[color]}`}>
      {label}
    </span>
  );
}

export function RefundListPanel() {
  const { data: refunds, isLoading, isError } = useRefunds();
  const { mutate: cancelRefund, isPending: isCancelling } = useCancelRefund();
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  if (isLoading) {
    return <div className="py-12 text-center text-sm text-gray-400">불러오는 중...</div>;
  }

  if (isError) {
    return <div className="py-12 text-center text-sm text-red-500">환불 목록을 불러오지 못했습니다.</div>;
  }

  if (!refunds || refunds.length === 0) {
    return <div className="py-12 text-center text-sm text-gray-400">환불 신청 내역이 없습니다.</div>;
  }

  function handleCancel(refundId: number) {
    if (!confirm('환불 신청을 취소하시겠습니까?')) return;
    setCancellingId(refundId);
    cancelRefund(refundId, { onSettled: () => setCancellingId(null) });
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr className="bg-gray-50 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">
            <th className="px-4 py-3 border-b border-gray-200">신청일</th>
            <th className="px-4 py-3 border-b border-gray-200">결제수단</th>
            <th className="px-4 py-3 border-b border-gray-200">환불 금액</th>
            <th className="px-4 py-3 border-b border-gray-200">현금영수증</th>
            <th className="px-4 py-3 border-b border-gray-200">상태</th>
            <th className="px-4 py-3 border-b border-gray-200">운영자 메모</th>
            <th className="px-4 py-3 border-b border-gray-200"></th>
          </tr>
        </thead>
        <tbody>
          {refunds.map((r) => (
            <tr key={r.refundId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
              <td className="px-4 py-3 text-sm text-gray-700">{formatDate(r.requestedAt)}</td>
              <td className="px-4 py-3 text-sm text-gray-700">{PAYMENT_METHOD_LABEL[r.methodType]}</td>
              <td className="px-4 py-3 text-sm font-semibold text-gray-900">{formatAmount(r.amount)}</td>
              <td className="px-4 py-3 text-sm text-gray-600">
                {r.hasCashReceipt
                  ? r.cashReceiptCancelled
                    ? '마이너스 정정 완료'
                    : '발급됨'
                  : '-'}
              </td>
              <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
              <td className="px-4 py-3 text-sm text-gray-500">{r.operatorMemo ?? '-'}</td>
              <td className="px-4 py-3">
                {r.status === 'PENDING' && (
                  <button
                    onClick={() => handleCancel(r.refundId)}
                    disabled={isCancelling && cancellingId === r.refundId}
                    className="text-xs text-red-500 hover:text-red-700 disabled:opacity-40"
                  >
                    취소
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

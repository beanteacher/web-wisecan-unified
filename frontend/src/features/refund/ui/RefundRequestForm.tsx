'use client';

import { useState } from 'react';
import { useRequestRefund } from '../model/useRequestRefund';

interface Props {
  chargeBalanceId: number;
  maxAmount: number;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export function RefundRequestForm({ chargeBalanceId, maxAmount, onSuccess, onCancel }: Props) {
  const { mutate: requestRefund, isPending, isError, error } = useRequestRefund();

  const [amount, setAmount] = useState('');
  const [hasCashReceipt, setHasCashReceipt] = useState(false);
  const [cashReceiptIssueNo, setCashReceiptIssueNo] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setValidationError(null);

    const amountNum = Number(amount);
    if (!amount || isNaN(amountNum) || amountNum < 1) {
      setValidationError('환불 금액을 입력해 주세요.');
      return;
    }
    if (amountNum > maxAmount) {
      setValidationError(`환불 가능 금액(${maxAmount.toLocaleString()}원)을 초과했습니다.`);
      return;
    }
    if (hasCashReceipt && !cashReceiptIssueNo.trim()) {
      setValidationError('현금영수증 발행 번호를 입력해 주세요.');
      return;
    }

    requestRefund(
      {
        chargeBalanceId,
        amount: amountNum,
        hasCashReceipt,
        cashReceiptIssueNo: hasCashReceipt ? cashReceiptIssueNo.trim() : null,
      },
      { onSuccess },
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          환불 금액 <span className="text-red-500">*</span>
        </label>
        <input
          type="number"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          min={1}
          max={maxAmount}
          placeholder={`최대 ${maxAmount.toLocaleString()}원`}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <p className="mt-1 text-xs text-gray-400">환불 가능 금액: {maxAmount.toLocaleString()}원</p>
      </div>

      <div>
        <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
          <input
            type="checkbox"
            checked={hasCashReceipt}
            onChange={(e) => setHasCashReceipt(e.target.checked)}
            className="rounded border-gray-300"
          />
          현금영수증 발급된 건입니다
        </label>
      </div>

      {hasCashReceipt && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            현금영수증 국세청 발행 번호 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={cashReceiptIssueNo}
            onChange={(e) => setCashReceiptIssueNo(e.target.value)}
            placeholder="예: CR-2024-001"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <p className="mt-1 text-xs text-gray-400">승인 시 마이너스 정정이 자동 발행됩니다.</p>
        </div>
      )}

      {(validationError || isError) && (
        <div className="rounded-lg border-l-4 border-red-400 bg-red-50 px-4 py-3 text-sm text-red-700">
          {validationError ?? '환불 신청 중 오류가 발생했습니다. 다시 시도해 주세요.'}
        </div>
      )}

      <div className="flex gap-3 justify-end pt-2">
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            취소
          </button>
        )}
        <button
          type="submit"
          disabled={isPending}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isPending ? '신청 중...' : '환불 신청'}
        </button>
      </div>
    </form>
  );
}

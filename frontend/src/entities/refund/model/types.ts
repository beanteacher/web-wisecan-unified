export type PaymentMethodType =
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'BANK_TRANSFER'
  | 'VIRTUAL_ACCOUNT'
  | 'MOBILE'
  | 'GIFT_CARD'
  | 'POINT';

export type RefundStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'COMPLETED';

export interface RefundResponse {
  refundId: number;
  memberId: number;
  chargeBalanceId: number;
  amount: number;
  methodType: PaymentMethodType;
  status: RefundStatus;
  hasCashReceipt: boolean;
  cashReceiptCancelled: boolean;
  operatorMemo: string | null;
  requestedAt: string;
  processedAt: string | null;
}

export interface CreateRefundRequest {
  chargeBalanceId: number;
  amount: number;
  hasCashReceipt: boolean;
  cashReceiptIssueNo: string | null;
}

export const PAYMENT_METHOD_LABEL: Record<PaymentMethodType, string> = {
  CREDIT_CARD: '신용카드',
  DEBIT_CARD: '체크카드',
  BANK_TRANSFER: '계좌이체',
  VIRTUAL_ACCOUNT: '가상계좌',
  MOBILE: '휴대폰 소액결제',
  GIFT_CARD: '상품권',
  POINT: '포인트',
};

export const REFUND_STATUS_LABEL: Record<RefundStatus, string> = {
  PENDING: '검토 중',
  APPROVED: '승인',
  REJECTED: '반려',
  CANCELLED: '취소됨',
  COMPLETED: '완료',
};

export const REFUND_STATUS_COLOR: Record<RefundStatus, 'gray' | 'blue' | 'red' | 'green' | 'orange'> = {
  PENDING: 'orange',
  APPROVED: 'blue',
  REJECTED: 'red',
  CANCELLED: 'gray',
  COMPLETED: 'green',
};

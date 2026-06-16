import { api } from '@/shared/api/client';
import type { RefundResponse, CreateRefundRequest } from '../model/types';

export async function getRefunds(): Promise<RefundResponse[]> {
  return api.get('billing/refund').json<RefundResponse[]>();
}

export async function createRefund(request: CreateRefundRequest): Promise<RefundResponse> {
  return api.post('billing/refund', { json: request }).json<RefundResponse>();
}

export async function cancelRefund(refundId: number): Promise<void> {
  await api.delete(`billing/refund/${refundId}`);
}

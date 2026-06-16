import { api } from '@/shared/api/client';
import type {
  SendHistoryDetail,
  SendHistoryListParams,
  SendHistoryPage,
} from '../model/types';

/**
 * 발송 이력 목록 조회 (W-304).
 *
 * GET /histories
 * 키별 조회 범위 정책: scopeMember=false(기본, scope:key) / true(scope:member)
 */
export async function fetchSendHistoryList(
  params: SendHistoryListParams = {}
): Promise<SendHistoryPage> {
  const searchParams = new URLSearchParams();

  if (params.fromDate) searchParams.set('fromDate', params.fromDate);
  if (params.toDate) searchParams.set('toDate', params.toDate);
  if (params.channel) searchParams.set('channel', params.channel);
  if (params.callbackNumber) searchParams.set('callbackNumber', params.callbackNumber);
  if (params.recipientNumber) searchParams.set('recipientNumber', params.recipientNumber);
  if (params.status) searchParams.set('status', params.status);
  if (params.scopeMember) searchParams.set('scopeMember', 'true');
  if (params.page !== undefined) searchParams.set('page', String(params.page));
  if (params.size !== undefined) searchParams.set('size', String(params.size));

  return api.get('histories', { searchParams }).json<SendHistoryPage>();
}

/**
 * 발송 이력 단건 상세 조회 (W-304).
 *
 * GET /histories/{sendId}
 * 접근 불가 이력은 404 응답 (존재 자체 비노출).
 */
export async function fetchSendHistoryDetail(
  sendId: string,
  scopeMember = false
): Promise<SendHistoryDetail> {
  const searchParams = new URLSearchParams();
  if (scopeMember) searchParams.set('scopeMember', 'true');

  return api
    .get(`histories/${sendId}`, { searchParams })
    .json<SendHistoryDetail>();
}

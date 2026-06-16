import { api } from '@/shared/api/client';
import type {
  KakaoTemplate,
  KakaoRegisterRequest,
  RcsTemplate,
  TransferItem,
  TransferDetail,
  TransferRequest,
} from '../model/types';

// ── 카카오 템플릿 API ────────────────────────────────────────────

export async function fetchKakaoTemplates(): Promise<KakaoTemplate[]> {
  return api.get('api/v1/templates/kakao').json<KakaoTemplate[]>();
}

export async function fetchKakaoTemplateDetail(templateCode: string): Promise<KakaoTemplate> {
  return api.get(`api/v1/templates/kakao/${templateCode}`).json<KakaoTemplate>();
}

export async function registerKakaoTemplate(
  request: KakaoRegisterRequest
): Promise<string> {
  return api.post('api/v1/templates/kakao', { json: request }).json<string>();
}

export async function updateKakaoTemplate(
  templateCode: string,
  request: KakaoRegisterRequest
): Promise<boolean> {
  return api.put(`api/v1/templates/kakao/${templateCode}`, { json: request }).json<boolean>();
}

export async function deleteKakaoTemplate(templateCode: string): Promise<boolean> {
  return api.delete(`api/v1/templates/kakao/${templateCode}`).json<boolean>();
}

// ── RCS 템플릿 API ───────────────────────────────────────────────

export async function fetchRcsBrands(): Promise<string[]> {
  return api.get('api/v1/templates/rcs/brands').json<string[]>();
}

export async function fetchRcsTemplatesByBrand(brandId: string): Promise<RcsTemplate[]> {
  return api.get(`api/v1/templates/rcs/brands/${encodeURIComponent(brandId)}`).json<RcsTemplate[]>();
}

export async function fetchRcsTemplateDetail(messagebaseId: string): Promise<RcsTemplate> {
  return api.get(`api/v1/templates/rcs/${messagebaseId}`).json<RcsTemplate>();
}

// ── 이관 처리 큐 API ─────────────────────────────────────────────

export async function fetchMyTransfers(): Promise<TransferItem[]> {
  return api.get('api/v1/templates/transfer').json<TransferItem[]>();
}

export async function fetchTransferDetail(transferId: number): Promise<TransferDetail> {
  return api.get(`api/v1/templates/transfer/${transferId}`).json<TransferDetail>();
}

export async function requestTransfer(request: TransferRequest): Promise<TransferItem> {
  return api.post('api/v1/templates/transfer', { json: request }).json<TransferItem>();
}

export async function cancelTransfer(transferId: number): Promise<boolean> {
  return api.delete(`api/v1/templates/transfer/${transferId}`).json<boolean>();
}

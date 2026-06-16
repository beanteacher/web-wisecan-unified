import { api } from '@/shared/api/client';
import type { ApiKey, IssueApiKeyRequest, ScopeCatalog, UpdateScopesRequest } from '../model/types';

export async function listApiKeys(): Promise<ApiKey[]> {
  return api.get('api-keys').json<ApiKey[]>();
}

export async function issueApiKey(request: IssueApiKeyRequest): Promise<ApiKey> {
  return api.post('api-keys', { json: request }).json<ApiKey>();
}

/** 재발급: 기존 키 즉시 폐기 + 동일 설정으로 새 키 발급. rawKey 1회 반환. */
export async function rotateApiKey(id: number): Promise<ApiKey> {
  return api.post(`api-keys/${id}/rotate`).json<ApiKey>();
}

/** 폐기 */
export async function revokeApiKey(id: number): Promise<void> {
  await api.patch(`api-keys/${id}/revoke`);
}

/** 스코프·한도 수정 */
export async function updateApiKeyScopes(id: number, request: UpdateScopesRequest): Promise<ApiKey> {
  return api.patch(`api-keys/${id}/scopes`, { json: request }).json<ApiKey>();
}

/** 스코프 카탈로그 조회 (인증 불필요) */
export async function getApiKeyScopeCatalog(): Promise<ScopeCatalog> {
  return api.get('api-keys/scopes/catalog').json<ScopeCatalog>();
}

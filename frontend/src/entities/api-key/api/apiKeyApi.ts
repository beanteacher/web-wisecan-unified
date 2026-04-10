import { api } from '@/shared/api/client';
import type { ApiKey } from '../model/types';

export async function listApiKeys(): Promise<ApiKey[]> {
  return api.get('api-keys').json<ApiKey[]>();
}

export async function issueApiKey(keyName: string): Promise<ApiKey> {
  return api.post('api-keys', { json: { keyName } }).json<ApiKey>();
}

export async function revokeApiKey(id: number): Promise<void> {
  await api.delete(`api-keys/${id}`);
}

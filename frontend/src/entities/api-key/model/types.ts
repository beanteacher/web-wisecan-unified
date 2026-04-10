export interface ApiKey {
  id: number;
  keyName: string;
  keyPrefix: string;
  status: 'ACTIVE' | 'REVOKED';
  lastUsedAt: string | null;
  createdAt: string;
  rawKey?: string;
}

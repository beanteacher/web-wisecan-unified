// API Key 스코프 카탈로그 12종 (02_FEATURE_SPEC.md §5.3)
export type ApiKeyScopeName =
  | 'SEND'
  | 'SEND_SMS'
  | 'SEND_KAKAO'
  | 'SEND_RCS'
  | 'HISTORY_READ'
  | 'CALLBACK_READ'
  | 'CALLBACK_MANAGE'
  | 'KEY_READ'
  | 'BALANCE_READ'
  | 'TEMPLATE_READ'
  | 'BRAND_READ'
  | 'WEBHOOK_MANAGE';

export type ApiKeyType = 'TEST' | 'PRODUCTION';

export interface ScopeInfo {
  name: ApiKeyScopeName;
  value: string;
  description: string;
}

export interface PresetInfo {
  test: string[];
  sendOnly: string[];
  readOnly: string[];
  full: string[];
}

export interface ScopeCatalog {
  scopes: ScopeInfo[];
  presets: PresetInfo;
}

export interface ApiKey {
  id: number;
  keyName: string;
  keyPrefix: string;
  status: 'ACTIVE' | 'REVOKED';
  keyType: ApiKeyType;
  scopes: ScopeInfo[];
  dailyLimit: number | null;
  allowedCallbacks: string[];
  lastUsedAt: string | null;
  createdAt: string;
  /** 발급·재발급 직후 1회만 포함됨 */
  rawKey?: string;
}

export interface IssueApiKeyRequest {
  keyName: string;
  keyType?: ApiKeyType;
  scopes?: ApiKeyScopeName[];
  dailyLimit?: number | null;
  allowedCallbacks?: string[];
}

export interface UpdateScopesRequest {
  scopes: ApiKeyScopeName[];
  dailyLimit?: number | null;
  allowedCallbacks?: string[];
}

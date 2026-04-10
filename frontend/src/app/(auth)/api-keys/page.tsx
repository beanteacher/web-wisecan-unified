import { IssueApiKeyDialog } from '@/features/api-key';
import { ApiKeyList } from '@/features/api-key';

export default function ApiKeysPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">API 키 관리</h1>
          <p className="mt-1 text-sm text-slate-500">API 키를 발급하고 관리합니다.</p>
        </div>
        <IssueApiKeyDialog />
      </div>
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <ApiKeyList />
      </div>
    </div>
  );
}

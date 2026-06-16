import { AdminSystemSettings } from '@/features/admin-stats';

export default function AdminSettingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">시스템 설정</h1>
        <p className="mt-1 text-sm text-slate-500">
          운영 파라미터를 조회하고 수정합니다.
        </p>
      </div>

      <AdminSystemSettings />
    </div>
  );
}

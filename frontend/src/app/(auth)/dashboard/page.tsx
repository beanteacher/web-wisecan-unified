import { UsageSummaryCards } from '@/features/usage';
import { RecentUsageTable } from '@/features/usage';

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">대시보드</h1>
        <p className="mt-1 text-sm text-slate-500">API 사용 현황을 확인합니다.</p>
      </div>
      <UsageSummaryCards />
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100">
          <h2 className="text-base font-semibold text-slate-900">최근 호출 이력</h2>
        </div>
        <RecentUsageTable />
      </div>
    </div>
  );
}

import { AdminDashboardCards } from '@/features/admin-stats';
import { AdminSendStatsTable } from '@/features/admin-stats';

export default function AdminDashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">운영자 대시보드</h1>
        <p className="mt-1 text-sm text-slate-500">
          발송량·매출·회원 현황을 실시간으로 확인합니다. (갱신 주기 5분)
        </p>
      </div>

      <AdminDashboardCards />

      <AdminSendStatsTable />
    </div>
  );
}

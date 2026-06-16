'use client';

import { useDashboardSummary } from '../model/useDashboardSummary';

function fmt(n: number) {
  return n.toLocaleString('ko-KR');
}

function wonFmt(n: number) {
  return n.toLocaleString('ko-KR') + '원';
}

interface StatCardProps {
  label: string;
  value: string;
  sub?: string;
}

function StatCard({ label, value, sub }: StatCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-6 py-5">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-400">{sub}</p>}
    </div>
  );
}

export function AdminDashboardCards() {
  const { data, isLoading, isError } = useDashboardSummary();

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="h-24 rounded-xl border border-slate-200 bg-slate-100 animate-pulse" />
        ))}
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 px-6 py-4 text-sm text-red-600">
        대시보드 데이터를 불러오지 못했습니다.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      <StatCard
        label="전체 회원"
        value={fmt(data.totalMembers)}
        sub={`오늘 신규 +${fmt(data.newMembersToday)}`}
      />
      <StatCard
        label="오늘 발송"
        value={fmt(data.totalSentToday)}
        sub={`이번 주 ${fmt(data.totalSentThisWeek)}`}
      />
      <StatCard
        label="이번 달 발송"
        value={fmt(data.totalSentThisMonth)}
        sub={`오늘 매출 ${wonFmt(data.revenueToday)}`}
      />
      <StatCard
        label="이번 달 매출"
        value={wonFmt(data.revenueThisMonth)}
        sub={`이번 주 ${wonFmt(data.revenueThisWeek)}`}
      />
      <StatCard
        label="대기 발송 요청"
        value={fmt(data.pendingSendRequests)}
      />
      <StatCard
        label="실패 발송 요청"
        value={fmt(data.failedSendRequests)}
      />
    </div>
  );
}

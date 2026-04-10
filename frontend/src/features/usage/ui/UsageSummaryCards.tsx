'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useUsageSummary } from '../model/useUsageSummary';

function StatCard({ title, value }: { title: string; value: number | string }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-slate-500">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-bold text-slate-900">{value}</p>
      </CardContent>
    </Card>
  );
}

export function UsageSummaryCards() {
  const { data, isLoading } = useUsageSummary();

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardHeader>
              <div className="h-4 w-24 animate-pulse rounded bg-slate-200" />
            </CardHeader>
            <CardContent>
              <div className="h-8 w-16 animate-pulse rounded bg-slate-200" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <StatCard title="총 호출" value={data?.totalCalls ?? 0} />
      <StatCard title="성공" value={data?.successCalls ?? 0} />
      <StatCard title="실패" value={data?.failCalls ?? 0} />
    </div>
  );
}

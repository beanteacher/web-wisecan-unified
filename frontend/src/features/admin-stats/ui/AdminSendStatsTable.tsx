'use client';

import { useState } from 'react';
import { useSendStats, useChannelBreakdown } from '../model/useSendStats';
import type { StatsPeriod } from '@/entities/admin-stats';

const PERIODS: { label: string; value: StatsPeriod }[] = [
  { label: '일별 (30일)', value: 'DAILY' },
  { label: '주별 (12주)', value: 'WEEKLY' },
  { label: '월별 (12개월)', value: 'MONTHLY' },
];

export function AdminSendStatsTable() {
  const [period, setPeriod] = useState<StatsPeriod>('DAILY');
  const { data: stats, isLoading } = useSendStats(period);
  const { data: channels } = useChannelBreakdown();

  return (
    <div className="space-y-6">
      {/* 발송량 통계 */}
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="text-base font-semibold text-slate-900">발송량 통계</h2>
          <div className="flex gap-1">
            {PERIODS.map(({ label, value }) => (
              <button
                key={value}
                onClick={() => setPeriod(value)}
                className={[
                  'rounded-md px-3 py-1.5 text-xs font-medium transition-colors',
                  period === value
                    ? 'bg-slate-900 text-white'
                    : 'text-slate-500 hover:bg-slate-100',
                ].join(' ')}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {isLoading ? (
          <div className="p-6 text-sm text-slate-400">불러오는 중...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-50 text-left text-xs font-semibold text-slate-500">
                  <th className="px-6 py-3">날짜</th>
                  <th className="px-6 py-3 text-right">발송 건수</th>
                  <th className="px-6 py-3 text-right">수신자 수</th>
                  <th className="px-6 py-3 text-right">총 비용</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {stats?.data.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-6 py-8 text-center text-slate-400">
                      데이터가 없습니다.
                    </td>
                  </tr>
                )}
                {stats?.data.map((row) => (
                  <tr key={row.date} className="hover:bg-slate-50">
                    <td className="px-6 py-3 font-mono text-slate-600">{row.date}</td>
                    <td className="px-6 py-3 text-right text-slate-900">{row.count.toLocaleString('ko-KR')}</td>
                    <td className="px-6 py-3 text-right text-slate-900">{row.recipientCount.toLocaleString('ko-KR')}</td>
                    <td className="px-6 py-3 text-right text-slate-900">{row.totalCost.toLocaleString('ko-KR')}원</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 채널별 분포 */}
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100">
          <h2 className="text-base font-semibold text-slate-900">채널별 발송 분포 (이번 달)</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 text-left text-xs font-semibold text-slate-500">
                <th className="px-6 py-3">채널</th>
                <th className="px-6 py-3 text-right">발송 건수</th>
                <th className="px-6 py-3 text-right">수신자 수</th>
                <th className="px-6 py-3 text-right">총 비용</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {!channels || channels.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-8 text-center text-slate-400">
                    데이터가 없습니다.
                  </td>
                </tr>
              ) : (
                channels.map((ch) => (
                  <tr key={ch.channel} className="hover:bg-slate-50">
                    <td className="px-6 py-3 font-medium text-slate-700">{ch.channel}</td>
                    <td className="px-6 py-3 text-right text-slate-900">{ch.count.toLocaleString('ko-KR')}</td>
                    <td className="px-6 py-3 text-right text-slate-900">{ch.recipientCount.toLocaleString('ko-KR')}</td>
                    <td className="px-6 py-3 text-right text-slate-900">{ch.totalCost.toLocaleString('ko-KR')}원</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

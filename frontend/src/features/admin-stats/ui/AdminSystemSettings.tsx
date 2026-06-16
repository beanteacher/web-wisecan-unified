'use client';

import { useState } from 'react';
import { useSystemSettings, useUpdateSystemSetting } from '../model/useSystemSettings';
import type { SystemSetting } from '@/entities/admin-stats';

function SettingRow({ setting }: { setting: SystemSetting }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(setting.value);
  const { mutate, isPending } = useUpdateSystemSetting();

  function save() {
    if (!draft.trim()) return;
    mutate(
      { key: setting.key, value: draft },
      { onSuccess: () => setEditing(false) },
    );
  }

  function cancel() {
    setDraft(setting.value);
    setEditing(false);
  }

  return (
    <tr className="hover:bg-slate-50">
      <td className="px-6 py-4 font-mono text-sm text-slate-700">{setting.key}</td>
      <td className="px-6 py-4 text-sm text-slate-500">{setting.description}</td>
      <td className="px-6 py-4">
        {editing ? (
          <input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            className="w-full rounded-md border border-slate-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          />
        ) : (
          <span className="text-sm font-medium text-slate-900">{setting.value}</span>
        )}
      </td>
      <td className="px-6 py-4 text-xs text-slate-400 whitespace-nowrap">
        {setting.updatedAt ? setting.updatedAt.replace('T', ' ').slice(0, 19) : '-'}
      </td>
      <td className="px-6 py-4">
        {editing ? (
          <div className="flex gap-2">
            <button
              onClick={save}
              disabled={isPending}
              className="rounded-md bg-slate-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700 disabled:opacity-50"
            >
              {isPending ? '저장 중...' : '저장'}
            </button>
            <button
              onClick={cancel}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-100"
            >
              취소
            </button>
          </div>
        ) : (
          <button
            onClick={() => setEditing(true)}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-100"
          >
            수정
          </button>
        )}
      </td>
    </tr>
  );
}

export function AdminSystemSettings() {
  const { data: settings, isLoading, isError } = useSystemSettings();

  if (isLoading) {
    return <div className="py-8 text-center text-sm text-slate-400">불러오는 중...</div>;
  }

  if (isError) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 px-6 py-4 text-sm text-red-600">
        설정을 불러오지 못했습니다.
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-100">
        <h2 className="text-base font-semibold text-slate-900">시스템 설정 관리</h2>
        <p className="mt-0.5 text-xs text-slate-400">
          운영 중 변경 가능한 시스템 파라미터를 관리합니다.
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-left text-xs font-semibold text-slate-500">
              <th className="px-6 py-3">키</th>
              <th className="px-6 py-3">설명</th>
              <th className="px-6 py-3">값</th>
              <th className="px-6 py-3">수정일시</th>
              <th className="px-6 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {!settings || settings.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-slate-400">
                  등록된 설정이 없습니다.
                </td>
              </tr>
            ) : (
              settings.map((s) => <SettingRow key={s.key} setting={s} />)
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

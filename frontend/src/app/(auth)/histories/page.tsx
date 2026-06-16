import { SendHistoryPanel } from '@/features/send-history';

export const metadata = {
  title: '발송 이력',
};

export default function HistoriesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">발송 이력</h1>
        <p className="mt-1 text-sm text-slate-500">
          API 키별 또는 회원 전체의 발송 이력을 조회합니다.
        </p>
      </div>
      <SendHistoryPanel />
    </div>
  );
}

import { RefundListPanel } from '@/features/refund';

export default function RefundPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">환불 내역</h1>
        <p className="mt-1 text-sm text-slate-500">
          미사용 선불 잔액의 환불 신청 및 처리 현황을 확인합니다.
          결제일로부터 5년이 경과한 잔액은 환불이 불가합니다.
        </p>
      </div>
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100">
          <h2 className="text-base font-semibold text-slate-900">환불 신청 목록</h2>
        </div>
        <RefundListPanel />
      </div>
    </div>
  );
}

'use client';

import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { MessageResultDetail } from '@/features/message-tool';

export default function MessageResultPage() {
  const params = useParams();
  const router = useRouter();
  const msgId = typeof params.msgId === 'string' ? params.msgId : null;

  return (
    <div className="space-y-6">
      {/* 뒤로가기 버튼 */}
      <div>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => router.push('/dashboard/message-tools?tab=history')}
        >
          ← 이력으로 돌아가기
        </Button>
      </div>

      {/* 페이지 헤더 */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900">발송 결과 상세</h1>
        <p className="mt-1 text-sm text-slate-500">
          메시지 ID 기반 상태 및 응답 상세 정보
        </p>
      </div>

      <MessageResultDetail msgId={msgId} />
    </div>
  );
}

'use client';

import { useParams, useRouter } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { MessageResultDetail } from '@/features/message-tool';

export default function MessageResultPage() {
  const params = useParams();
  const router = useRouter();
  const msgId = typeof params.msgId === 'string' ? params.msgId : null;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={() => router.push('/dashboard/message-tools?tab=history')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">발송 결과 상세</h1>
          <p className="mt-0.5 text-xs text-slate-400">
            메시지 도구 &rsaquo; 발송 이력 &rsaquo; 상세
          </p>
        </div>
      </div>

      <MessageResultDetail msgId={msgId} />
    </div>
  );
}

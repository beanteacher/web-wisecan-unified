'use client';

import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { MessageSquare } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { sendMessageSchema, type SendMessageFormValues } from '../model/sendMessageSchema';
import { useSendMessage } from '../model/useSendMessage';

const CHANNEL_OPTIONS = [
  { value: 'EMAIL', label: '이메일' },
  { value: 'SMS', label: 'SMS' },
  { value: 'PUSH', label: '푸시 알림' },
  { value: 'SLACK', label: 'Slack' },
] as const;

const CHANNEL_BADGE_LABEL: Record<string, string> = {
  EMAIL: '이메일',
  SMS: 'SMS',
  PUSH: '푸시',
  SLACK: 'Slack',
};

export function SendMessageForm() {
  const router = useRouter();
  const { mutateAsync, isPending } = useSendMessage();

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<SendMessageFormValues>({
    resolver: zodResolver(sendMessageSchema),
    defaultValues: { channel: 'EMAIL', recipient: '', content: '' },
  });

  const watchedValues = watch();

  const onSubmit = async (data: SendMessageFormValues) => {
    const result = await mutateAsync(data);
    router.push(`/dashboard/message-tools/result/${result.messageId}`);
  };

  const hasPreview = watchedValues.recipient || watchedValues.content;

  return (
    <div className="flex gap-6">
      {/* 좌측: 발송 폼 */}
      <div className="flex-1 rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-5 text-base font-semibold text-slate-900">메시지 발송</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* 수신자 */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-700">
              수신자 <span className="text-red-500">*</span>
            </label>
            <Input
              {...register('recipient')}
              placeholder="예) user@example.com 또는 @slack-user"
              aria-invalid={!!errors.recipient}
            />
            <p className="text-xs text-slate-400">
              이메일, Slack 핸들, 전화번호 등 채널별 수신자 식별자
            </p>
            {errors.recipient && (
              <p className="text-xs text-red-500">{errors.recipient.message}</p>
            )}
          </div>

          {/* 채널 */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-700">
              채널 <span className="text-red-500">*</span>
            </label>
            <select
              {...register('channel')}
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400 transition-colors"
              aria-invalid={!!errors.channel}
            >
              <option value="">발송 채널을 선택하세요</option>
              {CHANNEL_OPTIONS.map(({ value, label }) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
            {errors.channel && (
              <p className="text-xs text-red-500">{errors.channel.message}</p>
            )}
          </div>

          {/* 본문 */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-700">
              본문 <span className="text-red-500">*</span>
            </label>
            <Textarea
              {...register('content')}
              placeholder="발송할 메시지 내용을 입력하세요. 최대 2,000자까지 입력 가능합니다."
              maxLength={2000}
              showCount
              aria-invalid={!!errors.content}
            />
            {errors.content && (
              <p className="text-xs text-red-500">{errors.content.message}</p>
            )}
          </div>

          <div className="flex gap-2 justify-end pt-1">
            <Button type="button" variant="ghost" onClick={() => reset()}>
              초기화
            </Button>
            <Button type="submit" loading={isPending}>
              발송
            </Button>
          </div>
        </form>
      </div>

      {/* 우측: 미리보기 패널 */}
      <div className="w-[360px] shrink-0 rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-5 text-base font-semibold text-slate-900">미리보기</h2>
        {hasPreview ? (
          <div className="space-y-3">
            {watchedValues.channel && (
              <div className="flex items-center gap-2">
                <Badge variant="info">
                  {CHANNEL_BADGE_LABEL[watchedValues.channel] ?? watchedValues.channel}
                </Badge>
              </div>
            )}
            {watchedValues.recipient && (
              <div>
                <p className="text-xs font-medium text-slate-400">수신자</p>
                <p className="mt-0.5 text-sm text-slate-900">{watchedValues.recipient}</p>
              </div>
            )}
            {watchedValues.content && (
              <div>
                <p className="text-xs font-medium text-slate-400">내용</p>
                <p className="mt-0.5 whitespace-pre-wrap text-sm text-slate-900">{watchedValues.content}</p>
              </div>
            )}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center gap-2 rounded-lg bg-slate-50 px-6 py-8 text-center">
            <MessageSquare className="h-8 w-8 text-slate-300" />
            <p className="text-sm font-medium text-slate-500">
              내용을 입력하면 미리보기가 표시됩니다
            </p>
            <p className="text-xs text-slate-400">채널, 수신자, 본문을 입력해보세요</p>
          </div>
        )}
      </div>
    </div>
  );
}

'use client';

import { useRef, useState, useEffect, type ChangeEvent } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { FormField } from '@/components/ui/form-field';
import { MessagePreview, type MessagePreviewVariant } from '@/shared/ui/MessagePreview';
import {
  sendMessageSchema,
  type SendMessageFormValues,
  getSmsByteLength,
  getAutoChannel,
  SMS_BYTE_LIMIT,
  LMS_BYTE_LIMIT,
} from '../model/sendMessageSchema';
import { useSendMessage } from '../model/useSendMessage';

const CHANNEL_LABEL: Record<'SMS' | 'LMS' | 'MMS', string> = {
  SMS: 'SMS (단문)',
  LMS: 'LMS (장문)',
  MMS: 'MMS (이미지)',
};

const CHANNEL_HELP: Record<'SMS' | 'LMS' | 'MMS', string> = {
  SMS: '본문 90 byte 이하 — 기본 단문 메시지',
  LMS: '본문 90 byte 초과 — 장문으로 자동 전환',
  MMS: '이미지가 첨부되어 멀티미디어 메시지로 자동 전환',
};

const MAX_IMAGE_BYTES = 300 * 1024; // 300KB

export function SendMessageForm() {
  const router = useRouter();
  const { mutateAsync, isPending } = useSendMessage();
  const [image, setImage] = useState<File | null>(null);
  const [imageError, setImageError] = useState<string | null>(null);
  const [imageObjectUrl, setImageObjectUrl] = useState<string | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<SendMessageFormValues>({
    resolver: zodResolver(sendMessageSchema),
    defaultValues: { recipient: '', content: '' },
  });

  // imageObjectUrl 생성/해제 (메모리 릭 방지)
  useEffect(() => {
    if (!image) {
      setImageObjectUrl(undefined);
      return;
    }
    const url = URL.createObjectURL(image);
    setImageObjectUrl(url);
    return () => {
      URL.revokeObjectURL(url);
    };
  }, [image]);

  const watchedValues = watch();
  const contentBytes = getSmsByteLength(watchedValues.content ?? '');
  const autoChannel = getAutoChannel(watchedValues.content ?? '', !!image);

  const onSubmit = async (data: SendMessageFormValues) => {
    const channel = getAutoChannel(data.content, !!image);
    const result = await mutateAsync({
      channel,
      recipient: data.recipient,
      content: data.content,
      // 이미지 자체 업로드는 별도 엔드포인트가 없어 메타데이터만 전송한다.
      options: image ? { attachmentName: image.name, attachmentSize: image.size } : undefined,
    });
    router.push(`/dashboard/message-tools/result/${result.messageId}`);
  };

  const handleReset = () => {
    reset();
    setImage(null);
    setImageError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleImageChange = (e: ChangeEvent<HTMLInputElement>) => {
    setImageError(null);
    const file = e.target.files?.[0] ?? null;
    if (!file) {
      setImage(null);
      return;
    }
    if (!file.type.startsWith('image/')) {
      setImageError('이미지 파일만 첨부 가능합니다 (PNG, JPG)');
      setImage(null);
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setImageError('이미지 크기는 300KB 이하여야 합니다');
      setImage(null);
      return;
    }
    setImage(file);
  };

  const handleRemoveImage = () => {
    setImage(null);
    setImageError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const hasError = Object.keys(errors).length > 0;
  const hasPreview = watchedValues.recipient || watchedValues.content || image;
  const byteLimit = autoChannel === 'SMS' ? SMS_BYTE_LIMIT : LMS_BYTE_LIMIT;
  const byteColorClass =
    autoChannel === 'MMS'
      ? 'text-blue-600'
      : contentBytes > SMS_BYTE_LIMIT
        ? 'text-amber-600'
        : 'text-slate-500';

  return (
    <div className="flex flex-col gap-4 lg:flex-row lg:gap-6">
      {/* 좌측: 발송 폼 */}
      <div className="flex-1 rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-5 text-base font-semibold text-slate-900">문자메시지 발송</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* 수신자 */}
          <FormField
            label="수신자"
            required
            htmlFor="recipient"
            hint="휴대폰 번호 (하이픈 없이, 예: 01012345678)"
            error={errors.recipient?.message}
          >
            <Input
              id="recipient"
              {...register('recipient')}
              placeholder="예) 01012345678"
              inputMode="numeric"
              aria-invalid={!!errors.recipient}
            />
          </FormField>

          {/* 본문 */}
          <FormField
            label="본문"
            required
            htmlFor="content"
            error={errors.content?.message}
          >
            <Textarea
              id="content"
              {...register('content')}
              placeholder="발송할 문자 내용을 입력하세요."
              maxLength={LMS_BYTE_LIMIT}
              aria-invalid={!!errors.content}
              className="min-h-[120px]"
            />
            <div className="mt-2 flex items-center justify-between text-[12px]">
              <span className={byteColorClass}>
                {contentBytes} / {byteLimit} byte
              </span>
              <span className="flex items-center gap-1.5 text-slate-500">
                자동 발송 타입
                <Badge variant={autoChannel === 'MMS' ? 'info' : autoChannel === 'LMS' ? 'warning' : 'neutral'}>
                  {CHANNEL_LABEL[autoChannel]}
                </Badge>
              </span>
            </div>
            <p className="mt-1 text-[11px] text-slate-400">{CHANNEL_HELP[autoChannel]}</p>
          </FormField>

          {/* 첨부 이미지 (선택) */}
          <FormField
            label="첨부 이미지 (선택)"
            htmlFor="attachment"
            hint="이미지를 첨부하면 MMS 로 자동 전환됩니다"
          >
            <input
              ref={fileInputRef}
              id="attachment"
              type="file"
              accept="image/png,image/jpeg"
              onChange={handleImageChange}
              className="hidden"
            />
            {image ? (
              <div className="flex items-center justify-between rounded-lg border border-blue-200 bg-blue-50 px-4 py-3">
                <div className="flex items-center gap-3">
                  <Badge variant="info">MMS</Badge>
                  <div>
                    <p className="text-[13px] font-medium text-slate-900">{image.name}</p>
                    <p className="text-[11px] text-slate-500">{(image.size / 1024).toFixed(1)} KB</p>
                  </div>
                </div>
                <Button type="button" variant="ghost" size="sm" onClick={handleRemoveImage}>
                  제거
                </Button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="flex h-20 w-full items-center justify-center rounded-lg border border-dashed border-slate-300 bg-slate-50 text-xs text-slate-400 transition-colors hover:bg-slate-100"
              >
                이미지를 클릭하여 업로드 (PNG, JPG / 최대 300KB)
              </button>
            )}
            {imageError && <p className="mt-1 text-[12px] text-red-600">{imageError}</p>}
          </FormField>

          {/* 버튼 */}
          <div className="flex gap-2 pt-1">
            <Button type="submit" loading={isPending}>
              발송
            </Button>
            <Button type="button" variant="secondary" onClick={handleReset}>
              초기화
            </Button>
          </div>

          {/* 에러 배너 */}
          {hasError && (
            <div className="relative flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 py-3 pr-4 pl-5">
              <div className="absolute left-0 top-0 h-full w-1 rounded-l-lg bg-red-600" />
              <p className="text-[13px] text-red-600">
                ! 입력 내용을 확인해주세요.{' '}
                {errors.recipient?.message ?? errors.content?.message}
              </p>
            </div>
          )}
        </form>
      </div>

      {/* 우측: 미리보기 패널 */}
      <div className="w-full shrink-0 rounded-xl border border-slate-200 bg-white p-6 lg:w-[360px]">
        <h2 className="mb-4 text-base font-semibold text-slate-900">미리보기</h2>
        <MessagePreview
          variant={hasPreview ? (autoChannel.toLowerCase() as MessagePreviewVariant) : null}
          recipient={watchedValues.recipient}
          content={watchedValues.content}
          imageUrl={imageObjectUrl}
          imageName={image?.name}
        />
      </div>
    </div>
  );
}

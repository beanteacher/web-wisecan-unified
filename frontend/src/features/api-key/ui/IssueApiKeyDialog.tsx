'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useIssueApiKey } from '../model/useIssueApiKey';

const schema = z.object({
  keyName: z.string().min(1, '키 이름을 입력하세요').max(50, '50자 이내로 입력하세요'),
});

type FormData = z.infer<typeof schema>;

export function IssueApiKeyDialog() {
  const [open, setOpen] = useState(false);
  const [rawKey, setRawKey] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const { mutateAsync, isPending } = useIssueApiKey();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    const result = await mutateAsync(data.keyName);
    if (result.rawKey) {
      setRawKey(result.rawKey);
    }
    reset();
  };

  const handleCopy = async () => {
    if (!rawKey) return;
    await navigator.clipboard.writeText(rawKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleClose = () => {
    setOpen(false);
    setRawKey(null);
    setCopied(false);
    reset();
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) handleClose(); else setOpen(true); }}>
      <DialogTrigger render={<Button />}>API 키 발급</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{rawKey ? 'API 키가 발급되었습니다' : 'API 키 발급'}</DialogTitle>
        </DialogHeader>

        {rawKey ? (
          <div className="space-y-4">
            <p className="text-sm text-slate-600">
              이 키는 지금만 확인할 수 있습니다. 반드시 복사해두세요.
            </p>
            <div className="rounded-lg bg-slate-50 border border-slate-200 p-3 font-mono text-sm break-all">
              {rawKey}
            </div>
            <div className="flex gap-2">
              <Button onClick={handleCopy} variant="secondary" className="flex-1">
                {copied ? '복사됨!' : '복사하기'}
              </Button>
              <Button onClick={handleClose} className="flex-1">확인</Button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-700">키 이름</label>
              <Input
                {...register('keyName')}
                placeholder="예: 프로덕션 서버"
                aria-invalid={!!errors.keyName}
              />
              {errors.keyName && (
                <p className="text-xs text-red-500">{errors.keyName.message}</p>
              )}
            </div>
            <DialogFooter>
              <Button type="button" variant="secondary" onClick={handleClose}>취소</Button>
              <Button type="submit" loading={isPending}>발급하기</Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

import { cn } from '@/shared/lib/cn';
import { EmptyPreview } from './variants/EmptyPreview';
import { SmsPreview } from './variants/SmsPreview';

export type MessagePreviewVariant = 'sms' | 'lms' | 'mms';

export interface MessagePreviewProps {
  variant?: MessagePreviewVariant | null;
  recipient?: string;
  content?: string;
  imageUrl?: string;
  imageName?: string;
  className?: string;
}

export function MessagePreview({
  variant,
  recipient,
  content,
  imageUrl,
  imageName,
  className,
}: MessagePreviewProps) {
  const isEmpty = !variant || (!recipient && !content);

  return (
    <div className={cn('h-[500px]', className)}>
      {isEmpty ? (
        <EmptyPreview />
      ) : (
        <SmsPreview
          variant={variant}
          recipient={recipient}
          content={content}
          imageUrl={imageUrl}
          imageName={imageName}
        />
      )}
    </div>
  );
}

import { z } from 'zod';

export const sendMessageSchema = z.object({
  channel: z.enum(['EMAIL', 'SMS', 'PUSH', 'SLACK'], {
    message: '채널을 선택하세요',
  }),
  recipient: z
    .string()
    .min(1, '수신자를 입력하세요')
    .max(200, '200자 이내로 입력하세요'),
  content: z
    .string()
    .min(1, '본문을 입력하세요')
    .max(2000, '2000자 이내로 입력하세요'),
});

export type SendMessageFormValues = z.infer<typeof sendMessageSchema>;

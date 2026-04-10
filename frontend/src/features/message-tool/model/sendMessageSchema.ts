import { z } from 'zod';

/**
 * 사용자 입력 폼 스키마.
 * 채널(SMS/LMS/MMS) 은 사용자가 직접 선택하지 않고 본문 바이트 수 + 이미지 첨부 여부로 자동 결정되므로
 * 이 스키마에는 포함하지 않는다. 자동 판정 규칙은 `getAutoChannel()` 참고.
 */
export const sendMessageSchema = z.object({
  recipient: z
    .string()
    .min(1, '수신자를 입력하세요')
    .regex(/^0\d{9,10}$/, '하이픈 없는 휴대폰 번호 형식으로 입력하세요 (예: 01012345678)'),
  content: z
    .string()
    .min(1, '본문을 입력하세요')
    .max(2000, '2000자 이내로 입력하세요'),
});

export type SendMessageFormValues = z.infer<typeof sendMessageSchema>;

/**
 * 한글 1자 = 2byte, ASCII 1자 = 1byte 로 계산 (국내 이통사 SMS 기준, EUC-KR 관행).
 * UTF-8 바이트 수와는 다르니 주의.
 */
export function getSmsByteLength(text: string): number {
  let bytes = 0;
  for (let i = 0; i < text.length; i++) {
    bytes += text.charCodeAt(i) > 127 ? 2 : 1;
  }
  return bytes;
}

/** SMS 단문 최대 바이트 (초과 시 LMS 로 자동 전환) */
export const SMS_BYTE_LIMIT = 90;
/** LMS 최대 바이트 */
export const LMS_BYTE_LIMIT = 2000;

/**
 * 입력 본문 + 이미지 첨부 여부로 실제 발송 채널을 자동 판정한다.
 * - 이미지 있으면 MMS
 * - 아니고 본문이 90 byte 초과면 LMS
 * - 그 외 SMS
 */
export function getAutoChannel(content: string, hasImage: boolean): 'SMS' | 'LMS' | 'MMS' {
  if (hasImage) return 'MMS';
  if (getSmsByteLength(content) > SMS_BYTE_LIMIT) return 'LMS';
  return 'SMS';
}

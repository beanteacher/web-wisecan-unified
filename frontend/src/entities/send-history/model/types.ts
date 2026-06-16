/**
 * 발송 이력 도메인 타입 (W-304).
 *
 * 백엔드 SendHistoryDto 와 1:1 대응.
 * 키별 조회 범위 정책(scope:key / scope:member)은 API 호출 파라미터로 전달한다.
 */

export type SendChannel = 'SMS' | 'LMS' | 'MMS' | 'KAKAO' | 'RCS';

export type SendRequestStatus = 'PENDING' | 'QUEUED' | 'FAILED' | 'CANCELLED';

// GET /histories 목록 항목
export interface SendHistoryListItem {
  sendId: string;
  channel: SendChannel;
  callbackNumber: string;
  recipientCount: number;
  status: SendRequestStatus;
  totalCost: number;
  requestedAt: string;
  createdAt: string;
}

// GET /histories/{sendId} 상세 항목
export interface SendHistoryDetail {
  sendId: string;
  channel: SendChannel;
  callbackNumber: string;
  recipientNumbers: string;
  recipientCount: number;
  subject: string | null;
  messageBody: string;
  status: SendRequestStatus;
  failReason: string | null;
  unitCost: number;
  totalCost: number;
  externalMsgId: number | null;
  requestedAt: string;
  createdAt: string;
  updatedAt: string;
}

// GET /histories 페이지 응답
export interface SendHistoryPage {
  content: SendHistoryListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// 목록 조회 파라미터
export interface SendHistoryListParams {
  fromDate?: string;
  toDate?: string;
  channel?: SendChannel;
  callbackNumber?: string;
  recipientNumber?: string;
  status?: SendRequestStatus;
  scopeMember?: boolean;
  page?: number;
  size?: number;
}

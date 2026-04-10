export type MessageStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'PARTIAL';
export type MessageChannel = 'EMAIL' | 'SMS' | 'PUSH' | 'SLACK';

// GET /api/v1/tools/message/{msgId} 응답
export interface MessageResult {
  messageId: string;
  channel: string;
  recipient: string;
  content: string;
  status: MessageStatus;
  sentAt: string;
  deliveredAt?: string | null;
  metadata?: Record<string, unknown> | null;
}

// GET /api/v1/tools/message/search 응답 항목
export interface MessageSearchItem {
  messageId: string;
  channel: string;
  recipient: string;
  status: MessageStatus;
  sentAt: string;
  responseTimeMs: number;
}

export interface MessageSearchParams {
  channel?: MessageChannel;
  status?: MessageStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

// POST /api/v1/tools/message/send 요청
export interface SendMessageRequest {
  channel: MessageChannel;
  recipient: string;
  content: string;
  options?: Record<string, unknown>;
}

// POST /api/v1/tools/message/send 응답
export interface SendMessageResponse {
  messageId: string;
  status: string;
  channel: string;
  recipient: string;
  sentAt: string;
}

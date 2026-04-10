import { api } from '@/shared/api/client';
import type {
  MessageResult,
  MessageSearchItem,
  MessageSearchParams,
  SendMessageRequest,
  SendMessageResponse,
} from '../model/types';

export async function sendMessage(req: SendMessageRequest): Promise<SendMessageResponse> {
  return api.post('tools/message/send', { json: req }).json<SendMessageResponse>();
}

export async function getMessageResult(messageId: string): Promise<MessageResult> {
  return api.get(`tools/message/${messageId}`).json<MessageResult>();
}

export async function searchMessages(params: MessageSearchParams): Promise<MessageSearchItem[]> {
  const searchParams = new URLSearchParams();
  if (params.channel) searchParams.set('channel', params.channel);
  if (params.status) searchParams.set('status', params.status);
  if (params.from) searchParams.set('from', params.from);
  if (params.to) searchParams.set('to', params.to);
  if (params.page !== undefined) searchParams.set('page', String(params.page));
  if (params.size !== undefined) searchParams.set('size', String(params.size));

  return api.get('tools/message/search', { searchParams }).json<MessageSearchItem[]>();
}

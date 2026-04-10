'use client';

import { useMutation } from '@tanstack/react-query';
import { sendMessage } from '@/entities/message';
import type { SendMessageRequest } from '@/entities/message';

export function useSendMessage() {
  return useMutation({
    mutationFn: (req: SendMessageRequest) => sendMessage(req),
  });
}

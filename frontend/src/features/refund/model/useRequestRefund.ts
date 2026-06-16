'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createRefund } from '@/entities/refund';
import type { CreateRefundRequest } from '@/entities/refund';

export function useRequestRefund() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateRefundRequest) => createRefund(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['billing', 'refunds'] });
      queryClient.invalidateQueries({ queryKey: ['billing', 'balance'] });
    },
  });
}

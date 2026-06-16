'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { cancelRefund } from '@/entities/refund';

export function useCancelRefund() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refundId: number) => cancelRefund(refundId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['billing', 'refunds'] });
    },
  });
}

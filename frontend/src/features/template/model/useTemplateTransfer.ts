'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchMyTransfers,
  requestTransfer,
  cancelTransfer,
} from '@/entities/template';
import type { TransferRequest } from '@/entities/template';

export function useMyTransfers() {
  return useQuery({
    queryKey: ['my-transfers'],
    queryFn: fetchMyTransfers,
  });
}

export function useRequestTransfer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: TransferRequest) => requestTransfer(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-transfers'] });
    },
  });
}

export function useCancelTransfer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (transferId: number) => cancelTransfer(transferId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-transfers'] });
    },
  });
}

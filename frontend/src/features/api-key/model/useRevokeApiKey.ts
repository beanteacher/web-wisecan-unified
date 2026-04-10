'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { revokeApiKey } from '@/entities/api-key';

export function useRevokeApiKey() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => revokeApiKey(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
    },
  });
}

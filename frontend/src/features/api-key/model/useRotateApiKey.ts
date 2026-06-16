'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { rotateApiKey } from '@/entities/api-key';

export function useRotateApiKey() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => rotateApiKey(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
    },
  });
}

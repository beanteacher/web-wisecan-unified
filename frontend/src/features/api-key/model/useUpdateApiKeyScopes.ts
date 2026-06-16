'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateApiKeyScopes } from '@/entities/api-key';
import type { UpdateScopesRequest } from '@/entities/api-key';

export function useUpdateApiKeyScopes() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateScopesRequest }) =>
      updateApiKeyScopes(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
    },
  });
}

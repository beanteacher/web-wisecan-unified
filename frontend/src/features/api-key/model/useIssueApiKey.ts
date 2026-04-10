'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { issueApiKey } from '@/entities/api-key';

export function useIssueApiKey() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (keyName: string) => issueApiKey(keyName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
    },
  });
}

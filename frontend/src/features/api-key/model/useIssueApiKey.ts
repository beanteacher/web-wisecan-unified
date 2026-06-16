'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { issueApiKey } from '@/entities/api-key';
import type { IssueApiKeyRequest } from '@/entities/api-key';

export function useIssueApiKey() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: IssueApiKeyRequest) => issueApiKey(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
    },
  });
}

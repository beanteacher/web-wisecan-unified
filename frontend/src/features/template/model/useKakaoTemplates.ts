'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchKakaoTemplates,
  fetchKakaoTemplateDetail,
  registerKakaoTemplate,
  updateKakaoTemplate,
  deleteKakaoTemplate,
} from '@/entities/template';
import type { KakaoRegisterRequest } from '@/entities/template';

export function useKakaoTemplates() {
  return useQuery({
    queryKey: ['kakao-templates'],
    queryFn: fetchKakaoTemplates,
  });
}

export function useKakaoTemplateDetail(templateCode: string | null) {
  return useQuery({
    queryKey: ['kakao-template', templateCode],
    queryFn: () => fetchKakaoTemplateDetail(templateCode!),
    enabled: !!templateCode,
  });
}

export function useRegisterKakaoTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: KakaoRegisterRequest) => registerKakaoTemplate(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kakao-templates'] });
    },
  });
}

export function useUpdateKakaoTemplate(templateCode: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: KakaoRegisterRequest) =>
      updateKakaoTemplate(templateCode, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kakao-templates'] });
      queryClient.invalidateQueries({ queryKey: ['kakao-template', templateCode] });
    },
  });
}

export function useDeleteKakaoTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (templateCode: string) => deleteKakaoTemplate(templateCode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kakao-templates'] });
    },
  });
}

'use client';

import { useQuery } from '@tanstack/react-query';
import {
  fetchRcsBrands,
  fetchRcsTemplatesByBrand,
  fetchRcsTemplateDetail,
} from '@/entities/template';

export function useRcsBrands() {
  return useQuery({
    queryKey: ['rcs-brands'],
    queryFn: fetchRcsBrands,
  });
}

export function useRcsTemplates(brandId: string | null) {
  return useQuery({
    queryKey: ['rcs-templates', brandId],
    queryFn: () => fetchRcsTemplatesByBrand(brandId!),
    enabled: !!brandId,
  });
}

export function useRcsTemplateDetail(messagebaseId: string | null) {
  return useQuery({
    queryKey: ['rcs-template', messagebaseId],
    queryFn: () => fetchRcsTemplateDetail(messagebaseId!),
    enabled: !!messagebaseId,
  });
}

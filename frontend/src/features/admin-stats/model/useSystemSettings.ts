import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchSystemSettings, updateSystemSetting } from '@/entities/admin-stats';

export function useSystemSettings() {
  return useQuery({
    queryKey: ['admin', 'settings'],
    queryFn: fetchSystemSettings,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUpdateSystemSetting() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      updateSystemSetting(key, value),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'settings'] });
    },
  });
}

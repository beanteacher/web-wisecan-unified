'use client';

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { authApi } from '../api/authApi';
import { useAuthStore } from './authStore';

export function useLogout() {
  const router = useRouter();
  const clearAuth = useAuthStore((s) => s.clearAuth);

  return useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // 서버 호출 실패해도 클라이언트 세션은 정리한다.
    }
    clearAuth();
    router.replace('/login');
  }, [clearAuth, router]);
}

'use client';

import { LogOut } from 'lucide-react';
import { useLogout } from '../model/useLogout';

interface LogoutButtonProps {
  className?: string;
}

export function LogoutButton({ className }: LogoutButtonProps) {
  const logout = useLogout();

  return (
    <button
      type="button"
      onClick={logout}
      aria-label="로그아웃"
      className={
        className ??
        'flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 transition-colors dark:text-zinc-400 dark:hover:bg-zinc-800/50 dark:hover:text-zinc-50'
      }
    >
      <LogOut className="h-4 w-4 shrink-0" />
      로그아웃
    </button>
  );
}

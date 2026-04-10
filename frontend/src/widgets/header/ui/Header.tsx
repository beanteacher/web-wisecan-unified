import Link from 'next/link';

export function Header() {
  return (
    <header className="sticky top-0 z-50 w-full border-b border-zinc-200 bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/60 dark:border-zinc-800 dark:bg-zinc-950/95">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link href="/login" className="flex items-center gap-2 font-semibold text-zinc-900 dark:text-zinc-50">
          <span className="text-lg font-bold">Wisecan</span>
          <span className="text-sm text-zinc-500 font-normal">MCP Connector</span>
        </Link>
        <nav className="hidden md:flex items-center gap-6 text-sm text-zinc-600 dark:text-zinc-400">
          {/* 유저 메뉴 placeholder — 인증 후 채워짐 */}
          <Link href="/login" className="hover:text-zinc-900 dark:hover:text-zinc-50 transition-colors">
            로그인
          </Link>
          <Link
            href="/register"
            className="rounded-md bg-zinc-900 px-3 py-1.5 text-white hover:bg-zinc-700 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200 transition-colors"
          >
            회원가입
          </Link>
        </nav>
      </div>
    </header>
  );
}

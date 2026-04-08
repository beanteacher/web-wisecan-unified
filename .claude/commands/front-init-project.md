# /init-project 커맨드

새로운 Next.js + FSD 프로젝트를 초기화합니다.

## 참조
- 아키텍처: `.claude/rules/frontend-architecture.md`
- 라이브러리: `.claude/rules/frontend-library-stack.md`

## 입력
$ARGUMENTS: 프로젝트명 (예: "my-app", "admin-dashboard")

---

## 실행 단계

### 1단계: 프로젝트 생성

```bash
pnpm create next-app@latest $ARGUMENTS --typescript --tailwind --eslint --app --src-dir --use-pnpm
cd $ARGUMENTS
```

### 2단계: 핵심 라이브러리 설치

```bash
# 런타임 의존성
pnpm add zustand @tanstack/react-query ky zod react-hook-form @hookform/resolvers date-fns framer-motion lucide-react

# 개발 의존성
pnpm add -D vitest @testing-library/react @testing-library/jest-dom @vitejs/plugin-react jsdom prettier eslint-config-prettier eslint-plugin-import
```

### 3단계: shadcn/ui 초기화

```bash
pnpm dlx shadcn@latest init -d
pnpm dlx shadcn@latest add button input dialog
```

### 4단계: FSD 디렉토리 구조 생성

```bash
mkdir -p src/{widgets,features,entities,shared/{api,ui,lib,hooks,constants,types}}
```

각 레이어에 빈 .gitkeep 생성:

```bash
for dir in src/widgets src/features src/entities; do
  touch "$dir/.gitkeep"
done
```

### 5단계: 공통 유틸리티 파일 생성

#### `src/shared/lib/cn.ts`
```tsx
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

#### `src/shared/api/client.ts`
```tsx
import ky from 'ky';

export const api = ky.create({
  prefixUrl: process.env.NEXT_PUBLIC_API_URL || '/api',
  timeout: 10000,
  hooks: {
    beforeRequest: [
      (request) => {
        const token = typeof window !== 'undefined'
          ? localStorage.getItem('token')
          : null;
        if (token) {
          request.headers.set('Authorization', `Bearer ${token}`);
        }
      },
    ],
  },
});
```

#### `src/app/providers.tsx`
```tsx
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            gcTime: 5 * 60 * 1000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}
```

### 6단계: tsconfig.json 경로 별칭 설정

```json
{
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/*"],
      "@/widgets/*": ["./src/widgets/*"],
      "@/features/*": ["./src/features/*"],
      "@/entities/*": ["./src/entities/*"],
      "@/shared/*": ["./src/shared/*"]
    }
  }
}
```

### 7단계: ESLint FSD 규칙 추가

`.eslintrc.js`에 FSD 레이어 의존성 규칙을 추가합니다.
상세 규칙은 `.claude/rules/frontend-library-stack.md`의 ESLint 섹션을 참고합니다.

### 8단계: dev-assistant 연결

```bash
bash ~/dev-assistant/scripts/setup.sh .
```

### 9단계: 초기 커밋

```bash
git add .
git commit -m "chore: 프로젝트 초기 셋업

- Next.js + TypeScript + Tailwind CSS
- FSD 아키텍처 디렉토리 구조
- 핵심 라이브러리 설치 (zustand, react-query, ky, zod)
- shadcn/ui 초기화
- dev-assistant 연결"
```

## 생성되는 구조

```
$ARGUMENTS/
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   └── providers.tsx
│   ├── widgets/
│   ├── features/
│   ├── entities/
│   └── shared/
│       ├── api/client.ts
│       ├── ui/
│       ├── lib/cn.ts
│       ├── hooks/
│       ├── constants/
│       └── types/
├── .claude -> ~/dev-assistant/.claude
├── CLAUDE.md
├── .mcp.json
└── .gitleaks.toml
```

## 체크리스트

- [ ] Next.js 프로젝트 생성
- [ ] 핵심 라이브러리 설치
- [ ] shadcn/ui 초기화
- [ ] FSD 디렉토리 구조 생성
- [ ] 공통 유틸리티 파일 생성
- [ ] tsconfig 경로 별칭 설정
- [ ] ESLint FSD 규칙 추가
- [ ] dev-assistant 연결
- [ ] 빌드 확인 (`pnpm build`)
- [ ] 초기 커밋
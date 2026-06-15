# 프론트엔드 코드 컨벤션 — 상세

> 규칙의 "왜"·우선순위는 `principles.md`, 레이어 배치는 `architecture.md`.

---

## 1. 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 컴포넌트 파일 | PascalCase.tsx | `StatCard.tsx` |
| 훅 파일 | camelCase.ts (`use*`) | `useAuth.ts` |
| 유틸/타입/api 파일 | camelCase.ts | `apiKeyApi.ts`, `types.ts` |
| 디렉토리(slice/segment) | kebab-case | `api-key/`, `send-message/` |
| 컴포넌트/타입/인터페이스 | PascalCase | `ApiKey`, `UserResponse` |
| 함수/변수 | camelCase | `listApiKeys` |
| 상수 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |

> 단, shadcn 컴포넌트(`components/ui/button.tsx`)는 라이브러리 관례상 kebab-case 파일명을 유지한다.

---

## 2. 컴포넌트 작성

```tsx
interface Props {
  title: string;
  onSubmit: (data: FormData) => void;
}

export default function StatCard({ title, onSubmit }: Props) {
  // 1. hooks  2. 상태/파생값  3. 핸들러  4. effect  5. render
  return <div>{title}</div>;
}
```
- 함수 컴포넌트 + `export default function`. **`React.FC` 미사용.**
- Props interface 는 컴포넌트 바로 위. 구조분해로 받기.
- 조건부 렌더는 early return.
- Server Component 기본. 상호작용 필요 시에만 최상단에 `'use client'`(트리 하단 배치).

---

## 3. 상태 관리 (경계)

```
URL 에 남을 상태(필터·페이지·탭)   → useSearchParams
서버 데이터                        → @tanstack/react-query
컴포넌트 로컬                      → useState / useReducer
여러 컴포넌트 공유 클라이언트 상태   → zustand
테마/인증 등 전역                  → zustand
```
- React Query 설정은 `app/providers.tsx`(staleTime 60s, gcTime 5m, retry 1, refetchOnWindowFocus false). 기존 값을 임의 변경하지 않는다.
- zustand 스토어는 `features/{x}/model/*Store.ts` 또는 전역이면 `shared`.

---

## 4. API 호출

```ts
// shared/api/client.ts — ky 인스턴스 (prefixUrl=/api/v1, 인증 헤더/401 처리 hook)
// entities/api-key/api/apiKeyApi.ts
import { api } from '@/shared/api/client';
import type { ApiKey } from '../model/types';

export async function listApiKeys(): Promise<ApiKey[]> {
  return api.get('api-keys').json<ApiKey[]>();
}
export async function issueApiKey(keyName: string): Promise<ApiKey> {
  return api.post('api-keys', { json: { keyName } }).json<ApiKey>();
}
```
- **호출은 entities/{x}/api 에 집중**, ky 클라이언트 경유. 컴포넌트에서 fetch/ky 직접 호출 금지.
- 백엔드 `ApiResponse<T>` envelope 의 `data` 추출·`success=false` 에러 처리는 **client hook 또는 api 세그먼트에서 일원화** — 컴포넌트는 도메인 타입만 받는다.
- React Query 와 결합: `useQuery({ queryKey: ['apiKey'], queryFn: listApiKeys })`. queryKey 는 계층적.

---

## 5. 폼 — react-hook-form + zod

```ts
// model/loginSchema.ts
export const loginSchema = z.object({
  email: z.string().email('올바른 이메일을 입력하세요'),
  password: z.string().min(8, '8자 이상 입력하세요'),
});
export type LoginFormData = z.infer<typeof loginSchema>;
```
```tsx
const { register, handleSubmit, formState: { errors, isSubmitting } } =
  useForm<LoginFormData>({ resolver: zodResolver(loginSchema) });
```
- 검증·타입의 단일 소스는 zod 스키마. 타입은 `z.infer` 로 파생.
- 제출 중 버튼은 `disabled={isSubmitting}` + 스피너(원문 텍스트 유지).

---

## 6. 스타일 — Tailwind v4 + shadcn

- 우선순위: Tailwind → (불가피 시) CSS Module. 클래스 순서: layout → sizing → spacing → typography → visual → interactive.
- 조건부 클래스는 `cn()`(clsx + tailwind-merge). `@apply` 보다 컴포넌트 추출 선호.
- 공용 UI 는 shadcn 컴포넌트 재사용(`components/ui/` · 신규는 `shared/ui/`). 색·라운드·상태 표현은 `../../.claude/rules/uiux-design-system.md` · `uiux-figma-plugin-rules.md` 준수.
- 아이콘은 `lucide-react`(SVG). 반응형 브레이크포인트 Mobile/Tablet/Desktop 3종 모두 고려.

---

## 7. Import 순서

```tsx
// 1. React / Next
import { useState } from 'react';
import Link from 'next/link';
// 2. 외부 라이브러리
import { useQuery } from '@tanstack/react-query';
// 3. 내부 (FSD 상위→하위, 절대경로 @/)
import { listApiKeys } from '@/entities/api-key';
import { Button } from '@/components/ui/button';
import { cn } from '@/shared/lib/cn';
import type { ApiKey } from '@/entities/api-key';
// 4. 상대경로
import { SubRow } from './SubRow';
// 5. 스타일
import styles from './Component.module.css';
```
- slice 내부 경로 직접 import 금지(배럴 `index.ts` 사용). 와일드카드 import 지양.

---

## 8. TypeScript

- `any` 금지(`unknown` 후 좁히기). API 응답 타입 필수 정의. `as` 최소화.
- `interface` 우선(유니온/인터섹션만 `type`). 제네릭 남용 금지.
- `strict` 모드 전제(`tsconfig`). `pnpm typecheck` 그린 유지.

---

## 9. 테스트 (Vitest + Testing Library)

- 테스트 파일은 대상 옆에 배치: `LoginForm.tsx` ↔ `LoginForm.test.tsx`, `useAuth.ts` ↔ `useAuth.test.ts`.
- 사용자 관점 쿼리(`getByRole`/`getByLabelText`) 우선. 구현 세부가 아닌 동작 검증.
- 실행: `pnpm test`(vitest run). 설정은 `vitest.config.ts` / `vitest.setup.ts`.

---

## 10. 커밋 (루트 `.claude/rules/git-workflow.md`)

- 타입 필수, scope 없음. 예: `feat: API Key 목록 페이지 추가`, `fix: 로그인 토큰 만료 처리 수정`, `style: 대시보드 반응형 레이아웃`.
- 한글 subject 허용. 본문은 "왜".

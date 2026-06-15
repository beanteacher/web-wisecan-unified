# 프론트엔드 라이브러리 스택 — WiseCan

> 실제 `frontend/package.json` 기준. 버전 핀은 package.json 이 정본.

---

## 1. 스택 요약

| 카테고리 | 라이브러리 | 비고 |
|----------|-----------|------|
| 프레임워크 | **Next.js 16.2** (App Router) | ⚠️ 브레이킹 체인지 — `node_modules/next/dist/docs/` 확인 |
| UI 런타임 | **React 19.2** | Server Component 기본 |
| 언어 | TypeScript 5 (strict) | `any` 금지 |
| 패키지 매니저 | **pnpm** (workspace) | `pnpm-lock.yaml` 정본 |
| 스타일 | **Tailwind CSS v4** (`@tailwindcss/postcss`) | v3 와 설정 방식 다름 |
| UI 킷 | **shadcn** + **@base-ui/react** | `components.json` 설정 |
| 서버 상태 | @tanstack/react-query 5 | `app/providers.tsx` |
| 클라이언트 상태 | zustand 5 | 경량 전역 상태 |
| 폼 | react-hook-form 7 + zod 4 + @hookform/resolvers | 스키마 기반 검증 |
| HTTP | **ky 2** | fetch 기반 경량(axios 미사용) |
| 날짜 | date-fns 4 | 트리쉐이킹 |
| 애니메이션 | framer-motion 12 | `'use client'` 필요 |
| 아이콘 | lucide-react | SVG |
| 클래스 유틸 | clsx + tailwind-merge (`cn`), class-variance-authority | shadcn variant |
| 테스트 | **Vitest 4** + @testing-library/react + jsdom | `vitest.config.ts` |
| 린트/포맷 | ESLint 9 (eslint-config-next) + Prettier | FSD import 규칙 강제 권장 |

---

## 2. 주요 스크립트

```bash
cd frontend
pnpm install            # 의존성 설치
pnpm dev                # 개발 서버 (next dev)
pnpm build              # 프로덕션 빌드 (next build)
pnpm start              # 프로덕션 실행 (next start)
pnpm lint               # eslint
pnpm typecheck          # tsc --noEmit
pnpm test               # vitest run
```

> 패키지 매니저는 **pnpm 고정**. `npm install`/`yarn` 사용 금지(lockfile 불일치).

---

## 3. 카테고리별 사용 규칙

### Next.js 16 / React 19
- `node_modules/next/dist/docs/` 우선 확인(`principles.md §1`). Server Component 기본, `'use client'` 최소화.
- 데이터 패칭은 Server Component 또는 React Query. Pages Router API 금지.

### Tailwind v4
- v4 는 설정 진입점이 `@tailwindcss/postcss`(postcss.config.mjs) + CSS `@import "tailwindcss"`. v3 의 `tailwind.config.js` content 배열 방식과 다르므로 기억으로 작성하지 말 것.
- 디자인 토큰(색·간격·타이포)은 `uiux-design-system.md` 기준. `globals.css` 의 CSS 변수와 정합.

### shadcn + @base-ui/react
- 컴포넌트 추가: `pnpm dlx shadcn@latest add <component>`. 출력 위치는 `components.json` alias(현재 `components/ui`).
- @base-ui/react 는 shadcn 의 headless 프리미티브. 직접 스타일은 Tailwind 로.

### React Query
- 프로바이더는 `app/providers.tsx`(`'use client'`). 기본 옵션(staleTime/gcTime/retry) 변경은 합의 후.
- API 함수는 `entities/{x}/api`, 훅은 `entities/{x}/model` 또는 `features`.

### zustand
- 스토어는 slice 의 `model/`. `create<State>()` + TypeScript 타입 명시.

### ky
- 인스턴스는 `shared/api/client.ts` 하나. `prefixUrl=/api/v1`, `beforeRequest` 로 JWT 헤더, `afterResponse` 로 401·`ApiResponse` envelope 처리.
- **axios 금지**(특히 `1.14.1`/`0.30.4` 공급망 공격 버전).

### Vitest
- 테스트는 대상 파일 옆. `vitest.setup.ts` 에 jest-dom 매처. `pnpm test` 그린 유지.

---

## 4. 새 라이브러리 도입 판단

```
1. 표준 스택(위 표)으로 해결되나? → 우선 사용.
2. shadcn/base-ui 로 되는 UI 인가? → 라이브러리 추가 대신 컴포넌트 추가.
3. 번들 크기·트리쉐이킹 영향은? → 무거운 의존성은 PR 에 근거.
4. 보안 — 공급망 이력 확인(axios 사례). lockfile(pnpm) 갱신 포함.
5. Next 16 / React 19 호환?  → peer deps 확인.
```

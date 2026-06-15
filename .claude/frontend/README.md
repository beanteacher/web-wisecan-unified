# 프론트엔드 개발자 참조 — WiseCan 통합 메시징 서비스

> Next.js 프론트엔드(`frontend/`) 개발자가 코드를 작성·리뷰하기 전에 읽는 진입점이다.
> 본 폴더(`.claude/frontend/`)는 **실제 코드베이스(`frontend/src`)의 관행**을 정본으로 한다.
> 기존 `.claude/rules/frontend-*.md` 와 충돌하면 **실제 코드 > 본 폴더 > rules 템플릿** 순으로 우선한다.

---

## 0. 가장 먼저 — Next.js 16 주의

`frontend/CLAUDE.md` 의 경고를 반드시 지킨다:

> **This is NOT the Next.js you know.** Next.js 16.2 는 학습 데이터와 API·관례·파일 구조가 다를 수 있다. 코드를 쓰기 전에 `node_modules/next/dist/docs/` 의 관련 가이드를 읽고, deprecation 경고를 따른다.

- 기억(훈련 데이터)으로 Next API 를 단정하지 말 것. `async` Server Component, `cookies()/headers()` 의 await, route handler 시그니처 등이 바뀌었을 수 있다.
- App Router 전제. Pages Router 패턴(`getServerSideProps` 등) 도입 금지.

---

## 1. 무엇부터 읽나 (우선순위)

1. `principles.md` — FSD 경계·타입 안전·보안·Next16 주의 등 Iron Rule.
2. `architecture.md` — FSD 레이어 + 실제 `src/` 구조(라우트 그룹, entities, shared).
3. `conventions.md` — 컴포넌트·네이밍·상태관리·스타일·import 규칙.
4. `stack.md` — 라이브러리 스택·스크립트.
5. `../../pm/03_IA.md` — 라우팅·내비·권한 매트릭스(화면 추가 시).
6. `../../pm/wireframes/` — 화면 와이어프레임.

---

## 2. 스택 한 줄 요약

Next.js 16.2(App Router) · React 19 · TypeScript 5(strict) · pnpm · Tailwind v4 · shadcn + @base-ui/react · @tanstack/react-query · zustand · react-hook-form + zod · ky · date-fns · framer-motion · lucide-react · Vitest.

상세는 `stack.md`.

---

## 3. 백엔드 계약 (반드시 맞춘다)

- 모든 API 응답은 **`ApiResponse<T>` envelope** = `{ success, data, message, timestamp }`. 프론트는 `data` 를 꺼내 쓰고 `success=false` 면 `message` 를 노출한다.
- API base URL prefix 는 **`/api/v1`**. ky 클라이언트(`shared/api/client`)의 `prefixUrl` 로 흡수.
- 인증: 웹 콘솔은 **JWT**(Authorization 헤더). 발송 자동화는 API Key(백엔드/SDK 영역).
- 백엔드 도메인·필드명은 `../../pm/05_DATA_MODEL.md` 정본을 따른다.

---

## 4. 작업 전 체크 (요약)

- [ ] Next 16 가이드(`node_modules/next/dist/docs/`)에서 사용할 API 를 확인했다.
- [ ] 새 코드의 FSD 레이어 위치가 `architecture.md` 와 정합한다(import 방향 준수).
- [ ] `'use client'` 는 상호작용이 필요한 트리 하단에만 붙였다.
- [ ] API 호출은 `entities/{x}/api` + ky 클라이언트를 경유한다(컴포넌트에서 fetch 직접 호출 금지).
- [ ] 타입은 `any` 없이 정의했다. API 응답 타입을 명시했다.
- [ ] `pnpm typecheck && pnpm lint && pnpm test` 그린.

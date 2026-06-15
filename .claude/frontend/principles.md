# 프론트엔드 핵심 원칙 (Iron Rule)

> 어기면 리뷰 반려. 상세 예시는 `conventions.md`, 레이어 배치는 `architecture.md`.

---

## 1. Next.js 16 — 기억으로 단정하지 않는다

- **코드를 쓰기 전에 `node_modules/next/dist/docs/` 를 확인**한다(`frontend/CLAUDE.md`). Next 16 은 API·구조가 학습 데이터와 다를 수 있다.
- Server Component 가 기본. `'use client'` 는 `useState`/`useEffect`/이벤트 핸들러/브라우저 API 가 필요한 컴포넌트에만, **트리 하단**에 붙인다.
- Pages Router 패턴(`getServerSideProps`, `_app`, `pages/`) 도입 금지. App Router 전용.
- deprecation 경고를 무시하지 않는다 — 경고대로 고친다.

> 왜: 틀린 기억으로 작성한 Next 코드는 빌드는 통과해도 런타임·캐싱·렌더링에서 조용히 깨진다.

---

## 2. FSD 레이어 경계를 지킨다

- 의존성 방향은 **한 방향**: `app → widgets → features → entities → shared`. 역방향·동일 레이어 간 import 금지.
  - `entities/message` 가 `entities/usage` 를 import 하지 않는다. 조합이 필요하면 상위(features/widgets)에서.
  - `shared` 는 다른 레이어를 import 하지 않는다(외부 라이브러리만).
- 각 slice 는 **`index.ts` (Public API)** 로만 외부에 노출한다. 내부 경로 직접 import 금지.
  ```ts
  import { listApiKeys } from '@/entities/api-key';        // OK
  import { listApiKeys } from '@/entities/api-key/api/apiKeyApi';  // 금지
  ```
- 코드 위치 판단: 도메인 무관 → `shared`, 데이터 모델 → `entities`, 사용자 액션 → `features`, 조합 UI 블록 → `widgets`, 라우팅/레이아웃 → `app`.

> 왜: 경계가 무너지면 순환 의존과 "어디를 고쳐야 깨지는지 모르는" 상태가 된다. FSD 는 변경 영향 범위를 레이어로 가둔다.

### 현재 코드의 과도기 상태 (인지하고 정렬)
- shadcn 컴포넌트가 `src/components/ui/` 에 있고, FSD 기준 위치는 `src/shared/ui/` 다. **신규 공용 UI 는 `shared/ui/` 에 두는 방향**으로 정렬하되, 기존 `components/ui/` 를 대량 이동하는 리팩터는 별도 PR 로 합의 후 진행한다(`architecture.md §과도기`).

---

## 3. 데이터 흐름 — API 는 entities 의 api 세그먼트로

- 네트워크 호출은 **`entities/{x}/api/*.ts`** 에 모으고 **`shared/api/client`(ky)** 를 경유한다. 컴포넌트에서 `fetch`/`ky` 를 직접 호출하지 않는다.
- 서버 상태는 **React Query**(`@tanstack/react-query`), 클라이언트 전역 상태는 **zustand**, URL 상태(필터·페이지·탭)는 `useSearchParams`. 로컬 상태는 `useState`. 이 경계를 섞지 않는다.
- 백엔드 응답은 `ApiResponse<T>` envelope 다. `data` 추출·에러(`success=false`) 처리는 **api 세그먼트 또는 ky hook 에서 일원화**하고, 컴포넌트는 정제된 도메인 타입만 받는다.
- queryKey 는 계층적으로: `['apiKey']`, `['apiKey', id]`, `['usage', { page }]`.

> 왜: 호출이 컴포넌트에 흩어지면 캐싱·인증 헤더·에러 처리가 제각각이 된다. MCP/CLI 와 같은 백엔드 계약을 프론트도 단일 지점에서 잡아야 한다.

---

## 4. 타입 안전 — any 금지

- **`any` 금지.** 모르면 `unknown` 후 좁힌다. API 응답 타입은 `entities/{x}/model/types.ts` 에 반드시 정의한다.
- `as` 타입 단언 최소화 — 타입 가드 사용. 불가피한 단언은 사유 주석.
- `interface` 우선(유니온/인터섹션만 `type`). 제네릭 남용 금지.
- 폼은 **zod 스키마 → `z.infer`** 로 타입을 파생한다. 검증과 타입의 단일 소스.

> 왜: 메시지 발송·결제 화면은 잘못된 타입 하나가 곧 오발송/오결제다. 타입을 신뢰할 수 있어야 한다.

---

## 5. 보안·UX 기본

- 토큰·비밀·API Key 원문을 **콘솔/로그에 출력하지 않는다.** localStorage 토큰 접근은 `shared/api/client` 의 인증 hook 한 곳으로 제한.
- 사용자 입력은 zod 로 검증하고, 외부 HTML 을 `dangerouslySetInnerHTML` 로 그대로 렌더하지 않는다.
- **금지 패키지** — axios `1.14.1` / `0.30.4`(공급망 공격 버전). 본 프로젝트는 ky 사용. axios 도입 자체를 지양.
- 로딩/에러/빈 상태를 항상 설계한다(`shared` 의 `Spinner`/`EmptyState`/`AlertBanner` 재사용). 버튼 로딩은 "로그인 중..." 텍스트 교체가 아니라 **원문 유지 + 스피너**(`../../.claude/rules/uiux-figma-plugin-rules.md`).
- 접근성: 시맨틱 태그·`aria-*`·키보드 포커스. 아이콘은 `lucide-react`(SVG) — 텍스트로 아이콘 흉내 금지.

> 왜: B2C 셀프서비스라 첫 화면의 신뢰·반응성이 전환율(보조 KPI)에 직접 작용한다.

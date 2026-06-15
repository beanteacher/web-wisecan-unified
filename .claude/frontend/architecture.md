# 프론트엔드 아키텍처 — FSD + Next.js App Router

> "이 코드 어디에 두지?" 의 단일 참조. 실제 `frontend/src` 기준.

---

## 1. FSD 레이어

```
app → widgets → features → entities → shared
 OK     OK         OK         OK     (외부 라이브러리만)
```

| 레이어 | import 가능 | 역할 |
|--------|------------|------|
| `app/` | widgets, features, entities, shared | Next 라우팅·레이아웃·프로바이더 (얇게) |
| `widgets/` | features, entities, shared | 여러 feature/entity 조합 UI 블록(Header, Sidebar) |
| `features/` | entities, shared | 사용자 액션 1건(로그인, 메시지 발송, 키 발급) |
| `entities/` | shared | 도메인 모델 + api + 표시용 컴포넌트 (행위 X) |
| `shared/` | 없음 | UI 킷·api 클라이언트·유틸·훅 (도메인 무관) |

- **동일 레이어 간 import 금지**, 역방향 금지. slice 는 `index.ts` 로만 노출.

---

## 2. 실제 `src/` 구조

```
src/
├── app/                              # Next.js App Router (라우팅 전용)
│   ├── layout.tsx · page.tsx · providers.tsx · globals.css
│   ├── (public)/                     #   비인증 라우트 그룹
│   │   ├── layout.tsx
│   │   ├── page.tsx                  #     랜딩
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   └── (auth)/                       #   인증 필요 라우트 그룹
│       ├── layout.tsx
│       ├── dashboard/page.tsx
│       ├── dashboard/message-tools/page.tsx
│       ├── dashboard/message-tools/result/[msgId]/page.tsx
│       ├── tools/message/page.tsx
│       └── api-keys/page.tsx
│
├── entities/                         # 도메인 모델 (현재 핵심 레이어)
│   ├── api-key/
│   │   ├── api/apiKeyApi.ts           #   listApiKeys / issueApiKey / revokeApiKey (ky 경유)
│   │   ├── model/types.ts            #   ApiKey 타입
│   │   └── index.ts                  #   Public API 배럴
│   ├── message/  (api · model · index)
│   └── usage/    (api · model · index)
│
├── shared/
│   └── api/client.ts                 # ky 인스턴스 (prefixUrl=/api/v1, 인증 헤더 hook)
│
└── components/ui/                    # shadcn 컴포넌트 (※ FSD 목표는 shared/ui — §6 과도기)
    ├── button · input · textarea · card · dialog · bottom-sheet
    ├── data-table · pagination · form-field
    ├── alert-banner · toast · spinner · empty-state · stat-card · status-dot · badge
```

> 아직 `features/`, `widgets/` 는 비어 있다(`entities/.gitkeep` 존재). 사용자 액션이 복잡해지면 `features/` 로 추출한다(§5).

---

## 3. 라우트 그룹 규칙

- `(public)` — 비로그인 접근(랜딩·로그인·회원가입). `(auth)` — 로그인 필수(대시보드·도구·키 관리).
- 인증 가드는 `(auth)/layout.tsx` 에서 처리(미인증 시 `/login` 리다이렉트). 페이지마다 가드를 흩뿌리지 않는다.
- 라우트 구조는 `../../pm/03_IA.md` 사이트맵과 정합. 새 화면은 **IA 먼저 갱신** 후 추가.

---

## 4. slice 내부 구조 (segment)

```
entities/api-key/
├── api/        # 네트워크 호출 (shared/api/client 경유)
├── model/      # 타입, 스토어, 훅, 스키마
├── ui/         # 표시용 컴포넌트 (선택)
└── index.ts    # Public API 배럴 — 외부는 이것만 import
```
features 도 동일 segment(`api`/`model`/`ui`/`index.ts`)를 따른다.

```ts
// entities/api-key/index.ts — 배럴 예시
export { listApiKeys, issueApiKey, revokeApiKey } from './api/apiKeyApi';
export type { ApiKey } from './model/types';
```

---

## 5. 새 기능 추가 판단

```
1. 도메인 무관 유틸/UI/훅?            → shared/
2. 백엔드 엔티티 표현 (Member, Send)?  → entities/{x}/ (api·model·ui)
3. 사용자 액션 (발송하기, 충전하기)?    → features/{action}/
4. 여러 feature/entity 조합 블록?       → widgets/
5. 라우팅/레이아웃?                    → app/
```

예: "메시지 발송" 화면 = `features/send-message/`(폼·제출 액션) + `entities/message/`(타입·api) + `shared/ui`(Button/FormField) 조합. 페이지(`app/(auth)/tools/message/page.tsx`)는 이들을 **조합만** 한다.

---

## 6. 과도기 메모 (components/ui → shared/ui)

- 현재 shadcn 공용 컴포넌트가 `src/components/ui/` 에 있다. FSD 정합 위치는 `src/shared/ui/`.
- **신규 공용 UI 는 `shared/ui/` 에 추가**하는 방향으로 정렬한다.
- 기존 `components/ui/` 대량 이동은 import 경로가 광범위하게 바뀌므로 **독립 리팩터 PR** 로 합의 후 진행한다. 그 전까지 두 경로의 역할(둘 다 "공용 UI")을 혼동하지 않는다.
- `components.json`(shadcn 설정)의 alias 도 이동 시 함께 갱신한다.

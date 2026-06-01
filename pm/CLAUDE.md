# pm/ — WiseCan 통합 메시징 서비스 설계 문서 디렉토리

> 본 디렉토리는 WiseCan 통합 메시징 서비스의 **제품·기획 설계 산출물**을 보관한다.
> PM 일반 운영 프로세스(스프린트·Jira·회고 등)는 `.claude/rules/pm-sprint-operations.md` 를 참조한다.

---

## 1. 디렉토리 구조

```
pm/
├── CLAUDE.md                  ← 본 파일 (디렉토리 안내)
├── 01_PRD.md                  ← 제품 요구사항 정의서
├── 02_FEATURE_SPEC.md         ← 기능 정의서 (액션 단위)
├── 03_IA.md                   ← 정보구조도 (도메인·라우팅·내비)
├── 04_PROJECT_PLAN.md         ← 프로젝트 계획서 (마일스톤·WBS·리스크)
├── wireframes/                ← HTML 와이어프레임 (33개 화면 + admin + _shared)
│   ├── 01.index.html ~ 33.components.html
│   ├── _shared/               ← 공통 헤더/사이드바/스타일
│   └── admin/                 ← 어드민 콘솔 와이어프레임
├── design/
│   └── build/                 ← markdown → PDF 빌드 스크립트
│       ├── build.cmd          ← Windows 더블클릭 진입점
│       ├── build.js           ← markdown-it + Edge headless 변환기
│       └── package.json       ← markdown-it 의존성
└── _reference/                ← 과거 산출물 아카이브 (PRD/FS/NFR/SRS 초안)
```

---

## 2. 설계 문서 4종 — 작성 순서·역할·교차 참조

| # | 문서 | 역할 | 주 독자 | 의존 |
|---|------|------|---------|------|
| **01** | `01_PRD.md` | 비전·페르소나·KPI·MVP 범위를 1문서에 압축 | 의사결정권자, 신규 합류자 | 없음 (최상위) |
| **02** | `02_FEATURE_SPEC.md` | 사용자 액션 단위로 트리거/사전조건/정상흐름/예외/권한/데이터영향/공수 7필드 카드 | 개발(BE/FE), QA | 01 |
| **03** | `03_IA.md` | 4개 도메인(공개/체험/회원/어드민) 사이트맵·라우팅·내비·권한 매트릭스 | FE, 디자이너 | 01, 02 |
| **04** | `04_PROJECT_PLAN.md` | 22주 / 11 스프린트 마스터 플랜, M0~M5 마일스톤, 리스크·자원·운영 게이트 | PM, 전 팀 | 01, 02, 03 |

### 문서 헤더 규칙

- 상단 메타는 **작성일만** 표기 — `> 작성일 YYYY-MM-DD`
- 버전·작성자·검토자 등 추가 메타 항목 나열 금지

### 본문 교차 참조 규칙

- 문서 내 절(節)은 `1.`, `1.2.`, `§A.`, `§14.C.` 등으로 번호를 매긴다.
- 본문에서 다른 절을 가리킬 때는 `§14.C` 형태로 쓴다 — PDF 빌드 시 자동으로 anchor 링크로 변환된다 (§ 기호는 출력에 표시되지 않음).
- 헤딩에 들어간 `§A.` 의 `§` 도 출력 단계에서 제거된다.

---

## 3. wireframes/ — HTML 와이어프레임

- 정적 HTML로 작성된 33개 화면(`01.index.html` ~ `33.components.html`) + `admin/` 어드민 콘솔 + `_shared/` 공통 자산.
- 디자인 시안 단계 산출물 — 실제 구현은 `frontend/` (Next.js + FSD) 에서 컴포넌트로 재작성한다.
- 파일명 번호 순서는 `03_IA.md` 의 사이트맵 노드 순서와 정합한다 — 새 화면 추가 시 IA 먼저 갱신한 뒤 wireframes 번호 부여.

### 화면 ↔ 문서 정합 체크리스트

- [ ] `03_IA.md` 사이트맵에 해당 라우트가 존재한다
- [ ] `02_FEATURE_SPEC.md` 에 해당 화면에서 일어나는 사용자 액션 카드가 있다
- [ ] `_shared/` 의 공통 헤더·사이드바를 import 하고 화면 고유 영역만 재작성했다

---

## 4. design/build/ — markdown → PDF 빌드

`01_PRD.md`, `02_FEATURE_SPEC.md`, `03_IA.md`, `04_PROJECT_PLAN.md` 4개 문서를 PDF로 변환한다.

### 실행 방법

```cmd
:: Windows — pm/design/build/build.cmd 더블클릭 또는
cd pm\design\build
build.cmd
```

```bash
# 또는 직접 node 실행 (markdown-it 사전 설치 필요)
cd pm/design/build
npm install
node build.js
```

### 산출물

- `pm/design/build/01_PRD.pdf`
- `pm/design/build/02_FEATURE_SPEC.pdf`
- `pm/design/build/03_IA.pdf`
- `pm/design/build/04_PROJECT_PLAN.pdf`

### 빌드 파이프라인

1. `markdown-it` 으로 렌더 + 헤딩 자동 ID 부여 (`1.` → `sec-1`, `14.C` → `sec-14-C`, `§A` → `sec-A`)
2. 본문의 `§N` / `§N.X` 참조를 anchor 링크로 자동 변환
3. 헤딩의 `§` 기호 제거
4. Pretendard CSS 적용 후 Microsoft Edge headless 로 HTML → PDF 출력

### 사전 요구사항

- Node.js 18+
- Microsoft Edge — 경로 고정: `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`
  (다른 경로면 `build.js` 의 `EDGE_PATH` 수정)
- `pm/design/build/node_modules/` (없으면 `build.cmd` 가 `npm install` 자동 실행)

### 빌드 대상 변경 시

`build.js` 의 `TARGETS` 배열을 수정한다. 각 항목은 `{ md, pdf, title }` 셋트 — `md` 는 `pm/` 기준 절대 경로, `pdf` 는 `pm/design/build/` 출력 파일명.

---

## 5. _reference/ — 아카이브

`_reference/PRD.md`, `_reference/design/FS.md`, `NFR.md`, `SRS.md` 는 현 4종 문서로 통합되기 전의 초안이다.
**신규 작성은 항상 `pm/01_PRD.md` ~ `pm/04_PROJECT_PLAN.md` 4종에서 한다.** `_reference/` 는 의사결정 이력 추적용으로만 보존한다.

---

## 6. 문서 변경 시 운영 규칙

1. **단일 진실 원천** — 동일 사실(KPI·MVP 범위·일정·도메인 구조)이 두 문서에 중복되면 그 중 한 곳을 정본으로 정하고 나머지는 `§참조` 만 둔다 (예: KPI 정본 = `01_PRD §성공 지표`, `04_PROJECT_PLAN` 은 `→ 01_PRD §성공 지표 K1·K2·K3` 로 위임).
2. **placeholder 금지** — `TBD`, `추후 작성`, `반영 예정` 등 미작성 표시 금지. 결정 전 항목은 문서에서 빼고 결정된 시점에 추가한다.
3. **변경 ↔ PDF 동기화** — `01`~`04` 중 어느 하나를 수정했으면 같은 커밋에서 `pm/design/build/*.pdf` 도 재생성한다.
4. **커밋 메시지** — `docs: 02_FEATURE_SPEC §3 발신번호 등록 시나리오 보강` 처럼 문서 번호와 절을 명시한다. scope 는 붙이지 않는다 (루트 `.claude/rules/git-workflow.md` 준수).
5. **본문 화살표·이모지** — 본문 링크에 임의의 "→" 를 붙이는 패턴 금지. 통일하거나 전부 제거한다.

---

## 7. 빠른 진입 가이드

> 처음 합류한 사람이 30분 안에 프로젝트 전체상을 파악하는 순서.

1. `01_PRD.md` — 비전·페르소나·KPI·MVP 범위 (필독)
2. `03_IA.md §0. 도메인 분리 원칙` — 4개 도메인 구분
3. `02_FEATURE_SPEC.md §1. 회원 가입·인증` — 액션 카드 7필드 포맷 익히기
4. `04_PROJECT_PLAN.md §2. 마일스톤` — 22주 일정 감 잡기
5. `wireframes/01.index.html` ~ `07.dashboard.html` — 회원 콘솔 골격 확인

---
globs: ["*sprint*", "*jira*", "MEMORY.md", "*plan*", "*report*", "AGENTS.md"]
description: "PM 스프린트 운영 프로세스 (Jira 티켓 발행, 스프린트 시작/종료, 팀 리더 관리, 완료 체크리스트)"
---

# PM 스프린트 운영 프로세스 (참고 자료 전용)

> **이 파일은 PM 스프린트 운영 프로세스 참고 자료만 포함합니다. PM 역할 정의·책임·금지사항은 `project_manager/AGENTS.md`를 참고하세요.**

---

## 스프린트 실행 플로우

### Phase 1: 스프린트 시작 (PM 역할)

```
1. Jira 스프린트 준비
   → 백로그에서 이번 스프린트 티켓 선별
   → 담당자 배정 + DoD 작성
   → jira_batch_create_issues 또는 jira_create_issue로 티켓 생성
   → jira_add_issues_to_sprint로 스프린트에 티켓 배치
   → jira_create_sprint + jira_update_sprint(state: active)로 스프린트 시작

2. 사용자 선보고 (필수)
   → 가장 중요한 작업 1개 + 실제 작업물 경로
   → placeholder 문구 절대 금지 ("TBD", "추후 작성", "반영 예정")
   → Sub Agent별 핵심 작업 1~3개 배정 내역

3. Slack 알림
   → #general에 스프린트 목표 + 주요 티켓 목록 공유

4. Sub Agent 작업 시작
   → PM 선보고 완료 후에만 Sub Agent 착수 허용
```

### Phase 2: 스프린트 진행 중

```
1. 진행 현황 추적
   → Jira 보드로 티켓 상태 모니터링
   → Blocked 티켓 즉시 대응 (사유 코멘트 + 해소 방안)
   → 스코프 변경 시 티켓 추가/제거 + 사유 코멘트

2. 티켓 상태 전환 관리
   → To Do → In Progress: 작업 착수 시 (담당 에이전트)
   → In Progress → In Review: 구현 완료, 리뷰 요청 시 (담당 에이전트)
   → In Review → Done: 리뷰 통과 + DoD 충족 (PM 승인)
   → → Blocked: 외부 의존성/블로커 발생 (사유 코멘트 필수)
```

### Phase 3: 스프린트 종료

```
1. 각 Worker Jira 완료 처리
   → 작업 완료된 티켓을 "완료" 상태로 전환 (jira_transition_issue)
   → 각 티켓에 산출물 경로, DoD 충족 여부 코멘트 (jira_add_comment)

2. 각 Worker 커밋 내역 정리
   → 역할별 git commit (커밋 컨벤션 준수)
   → 커밋 메시지에 Jira 티켓 번호 포함: feat: 기능 추가 (PROJ-123)

3. PM 커밋 내역 확인 후 보고
   → 산출물 경로, DoD 충족 여부, 변경 파일 수 사용자에게 보고
   → 미완료 티켓 → 다음 스프린트로 이월 (사유 기록)

4. PM PR 생성
   → develop 브랜치로 PR 생성 (main 직접 push 절대 금지)
   → PR 제목: 커밋 컨벤션 형식
   → 변경 파일 5개 초과 시 기능 단위로 PR 분리

5. 사용자 merge 대기
   → 사용자가 PR 승인/merge할 때까지 대기
   → merge 강제 실행 금지

6. merge 후 Jira 스프린트 종료
   → jira_update_sprint(state: closed)로 스프린트 완료 처리
   → Slack #general에 완료/미완료 요약 + 다음 스프린트 예고

7. MEMORY.md 업데이트 (필수)
   → 스프린트 진행 결과를 MEMORY.md에 기록
   → 위치: {프로젝트 루트}/MEMORY.md — 다른 경로 절대 금지
   → MEMORY.md 업데이트 없이 커밋/종료 금지
```

---

## Jira 티켓 운영 규칙

### PM의 Jira 책임

1. **에픽 생성** — 마스터 플랜의 마일스톤 단위로 에픽을 생성한다.
2. **스토리/태스크 생성** — WBS 기반으로 스토리와 태스크를 생성하고 담당 에이전트에 배정한다.
3. **스프린트 생성/관리** — 스프린트 생성, 티켓 배치, 시작/종료를 관리한다.
4. **우선순위 설정** — 모든 티켓의 우선순위(Highest/High/Medium/Low/Lowest)를 지정한다.
5. **DoD 명시** — 각 티켓의 완료기준(Definition of Done)을 Description에 명시한다.
6. **Done 전환 승인** — In Review 상태의 티켓을 검토 후 Done으로 전환한다.

### 티켓 생성 템플릿

```
제목: [에이전트] 작업 요약
설명:
  - 목적:
  - 완료기준(DoD):
  - 산출물:
  - 의존성: (선행 티켓 링크)
  - 참고: (Figma, API 명세, 설계 문서 등)
우선순위: High
담당자: FE / BE / QA / ...
에픽 링크: PROJ-XX
```

### 티켓 유형

| 유형 | 용도 | 생성 주체 |
|------|------|-----------|
| **Epic** | 대기능 단위 (스프린트 여러 개 걸침) | PM |
| **Story** | 사용자 가치 단위 기능 | PM |
| **Task** | 기술 작업 (인프라, 리팩터링 등) | PM 또는 담당 에이전트 |
| **Sub-task** | Story/Task의 세부 작업 | 담당 에이전트 |
| **Bug** | 결함 | QA 또는 발견한 에이전트 |

### 티켓 간 연결 (Link) 규칙

| 시나리오 | 링크 유형 | 예시 |
|----------|-----------|------|
| 디자인 완료 → FE 구현 | blocks / is blocked by | UI/UX 티켓 → FE 티켓 |
| API 구현 → FE 연동 | blocks / is blocked by | BE 티켓 → FE 티켓 |
| 버그 → 원인 티켓 | relates to | Bug → 관련 Story |
| 에셋 제작 → FE 뷰어 | blocks / is blocked by | 3D 티켓 → FE 티켓 |

---

## Design-First 워크플로우

> UI/UX 시안 완료 후에만 FE 구현을 허용한다. 시안 없이 FE 코드 작성 절대 금지.

### 의존성 설정

- 팀 구성 시 반드시 **UI/UX 시안 태스크 → FE 구현 태스크** 순서로 의존성(blocks) 설정
- UI/UX manifest 산출물 경로: `{프로젝트}/uiux_designer/figma-manifests/{산출물명}/`
- 필수 파일: `manifest.json` + `manifest.import-data.json` + `code.js` + `ui.html`

### manifest 작성 규칙

- manifest 형식은 `uiux_designer/AGENTS.md` 확인 후 작성 — 자의적 형식 금지
- `code.js`는 **ES5 호환 필수** (Figma 런타임 제약)
  - 금지: bare catch, arrow function, template literal, forEach
- FE는 핸드오프 통보를 받기 전까지 해당 섹션 구현 금지

---

## 팀 리더(Lead) 워커 관리 규칙

### Worker stuck 대응 (5분 이상 무응답/무한루프 시)

```
수동 대기 금지 — 리더가 능동적으로 원인을 조사하고 해결 가이드를 제공해야 한다.

조사 방법:
1. 해당 워커의 작업 디렉터리를 직접 Read/Glob으로 확인 (진행 상황 파악)
2. context7 MCP 등을 활용해 라이브러리/프레임워크 공식 문서 조회
   (설정 오류, 올바른 사용법 확인)
3. 조사 결과를 바탕으로 구체적인 해결 방안을 SendMessage로 워커에게 전달
4. 필요시 Explore agent를 스폰해서 병렬로 원인 분석

금지:
- 단순 "상태 확인" 메시지만 반복 전송 — 원인 분석 없는 상태 확인은 의미 없음
- 워커가 자력 해결할 때까지 방치
```

---

## 팀 실행 완료 후 체크리스트 (리더 필수)

> 팀 셧다운 전에 반드시 아래 항목을 **모두** 수행한다. 하나라도 누락 시 셧다운 금지.

```
□ Jira 티켓 상태 업데이트
  → 완료된 티켓은 "진행 중" → "완료" 상태로 전환 (jira_transition_issue)

□ Jira 티켓에 완료 코멘트 추가
  → 각 티켓에 산출물 경로, DoD 충족 여부 코멘트 (jira_add_comment)

□ Worker 산출물 검증
  → 셧다운 전 각 워커가 생성한 파일이 실제로 존재하는지 확인

□ PM에게 결과 보고
  → 각 워커의 산출물 경로·DoD 충족 여부를 PM에게 전달

□ 사용자 요청 사항 처리
  → 사용자가 대화 중 요청한 질의(PM 문의, 추가 작업 등)를 누락 없이 처리

□ 커밋 생성
  → 워커들이 생성한 파일을 역할별로 git commit (커밋 컨벤션 준수)

□ PR 생성
  → develop 브랜치로 PR 생성 (main 직접 push 금지, 사용자 승인 필수)

□ Slack 스프린트 완료 알림
  → 산출물 요약 포함하여 관련 채널에 메시지 전송

□ MEMORY.md 업데이트
  → 스프린트 진행 결과를 MEMORY.md에 기록
```

---

## Slack 운영 규칙 (PM)

| 이벤트 | 채널 | 내용 |
|--------|------|------|
| 스프린트 시작 | #general | 스프린트 목표 + 주요 티켓 목록 |
| 블로커 에스컬레이션 | #dev | 블로커 내용 + 관련 티켓 + 영향 범위 |
| 스프린트 종료 | #general | 완료/미완료 요약 + 다음 스프린트 예고 |
| 긴급 이슈 | #bugs / #alerts | 즉시 공유 + 대응 담당자 지정 |
| 핸드오프 완료 | #dev | 산출물 완료 + 티켓 번호 |

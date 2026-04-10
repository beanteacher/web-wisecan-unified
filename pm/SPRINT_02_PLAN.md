# SPRINT 2 계획서

## Sprint Goal
MCP 서버 연동 기반 위에 메시지 도구(발송/조회/검색) 프록시 API와 웹 UI, 그리고 외부 호출용 API Key 인증 미들웨어를 완성해, 사용자가 웹과 REST API 양쪽에서 메시지 도구를 실제로 사용할 수 있게 한다.

## 기간
Sprint 2 (2주)

## 포함 범위
- 도구 UI 와이어프레임 (메시지 도구 먼저 — 발송/조회/검색)
- MCP 서버 연동 모듈 (stdio 프로세스 lifecycle 관리 + 호출/응답 파싱)
- API Key 인증 미들웨어 (X-API-Key 헤더 기반, Rate Limiting 포함, 외부 REST 호출용)
- 메시지 도구 프록시 API (send / get_result / search)
- 메시지 발송/조회 프론트엔드 UI (`/tools/message`)

## 제외 범위 (Sprint 3 이월)
- 2.1.1 중 메시지 통계·파일 변환 화면 와이어프레임 (Sprint 3에서 통계/파일 이관 시점에 작성)
- 2.2.3 메시지 통계 프록시 API (stat_summary, daily_report)
- 2.2.4 파일 변환 프록시 API (md-to-docx, md-to-pdf)
- 2.3.2 메시지 통계 UI
- 2.3.3 파일 변환 UI
- 사용량 상세 차트·기간 필터 (Phase 3)
- 에이전트 진단 도구 (Phase 3)

**이월 사유**: MCP 서버 연동(2.2.1)이 Sprint 2의 최대 기술 리스크이며, 이를 기반으로 한 프록시 계열 작업을 한 스프린트에 모두 넣으면 일정 과부하가 발생한다. 메시지 도구 일부(send/get_result/search)만 먼저 안정화해 End-to-End 루트를 확보한 뒤, Sprint 3에서 동일 패턴으로 통계·파일 변환을 확장한다.

---

## 담당별 작업

### UI/UX 디자이너

| WBS ID | 작업 | DoD | 예상 공수 |
|--------|------|-----|-----------|
| 2.1.1-a | 메시지 도구 와이어프레임 (발송/조회/검색) | `uiux/figma-manifests/tools-message-wireframe/` 하위에 `manifest.json` + `manifest.import-data.json` + `code.js` + `ui.html` 생성. 메시지 발송 폼(수신자, 채널, 본문, 첨부), 발송 결과 상세, 다중 조건 검색(기간/상태/채널) PC(1440px) + Mobile(375px) 프레임. 에러/로딩/빈 상태 포함. `code.js`는 ES5 호환. FE 핸드오프 통보 완료. | 3일 |

**참고**: `2.1.1`은 Master Plan상 "메시지+파일" 통합 와이어프레임이지만, Sprint 2 범위에서는 메시지 섹션만 선행(`2.1.1-a`)으로 분리 진행한다. 파일·통계 섹션은 Sprint 3에서 `2.1.1-b`로 이어서 작성한다.

**Handoff 순서**: 메시지 발송 프레임 1차 완료 즉시 FE에 handoff 통보 → 조회/검색 프레임 추가 완료 시 2차 handoff.

### Backend 개발

| WBS ID | 작업 | DoD | 예상 공수 |
|--------|------|-----|-----------|
| 2.2.1 | MCP 서버 연동 모듈 (stdio 프로세스 관리) | `backend/src/main/java/.../mcp/` 패키지 생성. `McpProcessManager`(프로세스 spawn/health-check/종료), `McpClient`(JSON-RPC 요청/응답 파싱), `McpToolInvoker`(도구명+파라미터 → 결과 매핑). 단위 테스트: mock 프로세스로 호출/응답/에러/타임아웃 케이스 통과. `application.yml`에 MCP 실행 경로 설정. | 3일 |
| 2.2.5 | API Key 인증 미들웨어 (외부 호출용) | `ApiKeyAuthFilter` 구현: `X-API-Key` 헤더 → SHA-256 해시 → DB 조회 → 유효/만료/REVOKED 분기. `SecurityConfig`에서 `/api/v1/tools/**` 경로에 적용 (기존 JWT 필터와 공존). Redis 기반 Rate Limiting (분당 60회 기본값, 환경설정 키). 401/429 응답 포맷은 `ApiResponse` 래핑. `@WebMvcTest` + 통합 테스트로 유효/만료/한도초과 케이스 통과. | 2일 |
| 2.2.2 | 메시지 도구 프록시 API (send/get_result/search) | `tools/message` 패키지에 `MessageToolController` + `MessageToolService` + `MessageToolDto`. 엔드포인트 3종: `POST /api/v1/tools/message/send`, `GET /api/v1/tools/message/{msgId}`, `GET /api/v1/tools/message/search`. 각 호출은 (1) API Key 인증 통과 (2) `McpToolInvoker` 호출 (3) `ApiUsage` 기록(tool_name, status, response_time_ms). 단위(`@ExtendWith(MockitoExtension.class)`) + 슬라이스(`@WebMvcTest`) 테스트, MCP 모듈은 Mock 처리. `./gradlew build` 통과. | 2.5일 |

**실행 순서**: `2.2.1` → `2.2.5`와 `2.2.2` 병렬 (2.2.2는 2.2.1의 `McpToolInvoker` 인터페이스가 먼저 확정된 시점부터 착수 가능). Key 인증(2.2.5)과 프록시(2.2.2)는 별도 패키지라 병렬화 가능.

**의존성 주의**: 2.2.2는 API Key 인증 필터(2.2.5)가 완료되지 않아도 컨트롤러 구현은 가능하나, 통합 테스트 단계에서 2.2.5 완료가 필요하다.

### Frontend 개발

| WBS ID | 작업 | DoD | 의존성 |
|--------|------|-----|--------|
| 2.3.1 | 메시지 발송/조회 UI | `frontend/src/features/message-tool/` 아래 FSD 구조(`api/`, `model/`, `ui/`, `index.ts`)로 구현. `app/(auth)/tools/message/page.tsx`에 배치. 기능: (a) 발송 폼 — react-hook-form + zod 검증, 수신자/채널/본문/옵션 입력, 성공 시 토스트 + 결과 상세로 이동, (b) 발송 결과 상세 페이지 — `msgId`로 조회, 상태/응답시간 표시, (c) 검색 — 기간/상태/채널 필터, 페이지네이션, 빈 상태 처리. API는 BE `/api/v1/tools/message/*`에 JWT 기반으로 호출(웹 UI는 JWT, 외부 REST는 API Key). `pnpm build` 성공, Vitest로 폼 검증·API mock 테스트 최소 3건 통과. | 2.1.1-a handoff + 2.2.2 완료 |

**실행 순서**: `2.1.1-a` 1차 handoff 후 발송 폼 UI 착수 → `2.2.2` 완료 시점에 API 연동으로 전환. 조회/검색은 2차 handoff + BE 완료 후.

---

## 의존성 맵 (임계 경로)

```
[UI/UX]                          [Backend]                         [Frontend]

2.1.1-a 메시지 WF ────────────┐
   (3일)                       │
                               │   2.2.1 MCP 연동 모듈
                               │      (3일)
                               │          │
                               │          ├─→ 2.2.2 메시지 프록시 API
                               │          │      (2.5일)
                               │          │          │
                               │          └─→ 2.2.5 API Key 인증
                               │                 (2일)
                               │                 │
                               └──────────────────┴────→ 2.3.1 메시지 발송/조회 UI
                                                            (3.5일)
```

**임계 경로**: `2.2.1 (3일)` → `2.2.2 (2.5일)` → `2.3.1 API 연동 단계 (~1.5일)` = **총 7일**

- UI/UX(`2.1.1-a`)는 3일로 BE 임계 경로와 병렬 완료 가능.
- `2.2.5`는 임계 경로 외(2.2.1 → 2.2.2와 병렬).
- FE(`2.3.1`)는 handoff 후 UI 선구현(3일) → BE 완료 후 API 연동(1.5일) = 실소요 3.5일 (1.5일은 UI 선구현 중 BE 완료 대기와 오버랩).

---

## 완료기준 (Sprint DoD)

- [x] 메시지 도구 와이어프레임(`2.1.1-a`) manifest 4종 파일(`manifest.json`, `manifest.import-data.json`, `code.js`, `ui.html`) 생성 및 FE handoff 통보 완료
- [x] MCP 서버 연동 모듈(`2.2.1`) 구현 + 단위 테스트(호출/응답/에러/타임아웃) 통과
- [x] API Key 인증 미들웨어(`2.2.5`) 동작 — 유효/만료/REVOKED/Rate Limit 4케이스 테스트 통과
- [x] 메시지 프록시 API(`2.2.2`) 3종 엔드포인트 동작 + `ApiUsage` 기록 동작 + 단위/슬라이스 테스트 통과
- [x] 메시지 발송 폼(`2.3.1`) — 유효성 검사, 성공/실패 핸들링, API 연동 완료
- [x] 메시지 결과 조회 화면 — `msgId` 기반 조회, 상태 표시 완료
- [x] 메시지 검색 화면 — 필터/페이지네이션/빈 상태 처리 완료
- [x] Backend `./gradlew build` 성공, 기존 Sprint 1 테스트 + Sprint 2 신규 테스트 전부 통과
- [x] Frontend `pnpm build` 성공, Vitest 기존 5건 + Sprint 2 신규 테스트 통과 (16/16)
- [ ] Sprint 2 신규 API는 JWT(웹 UI) 경로와 X-API-Key(외부 REST) 경로 양쪽에서 호출 가능 — 각 경로 최소 1건 수동 동작 확인 기록 (수동 확인 필요)
- [ ] `MEMORY.md`에 Sprint 2 결과 기록 (후속 작업)

---

## 리스크

| 리스크 | 영향 | 완화책 |
|--------|------|--------|
| MCP 서버 stdio 프로세스 lifecycle 복잡도 (hang, zombie, 타임아웃) | BE 일정 대폭 지연 → FE 착수 지연 | 2.2.1 착수 전 hello-world 수준 MCP 호출을 1일차 스파이크로 검증. 타임아웃(기본 10s) + 강제 종료 로직을 최초 버전부터 포함. 테스트는 Mock 프로세스 기반으로 격리. |
| MCP 응답 스키마 변동 → 파싱 오류 | 런타임 에러, 프록시 API 500 | `McpToolInvoker`에서 응답을 DTO 직변환하지 않고 `JsonNode`로 한 번 받은 뒤 관대한 파싱. 스키마 변경 시 한 곳만 수정. |
| API Key 인증 필터와 기존 JWT 필터 충돌 | 기존 `/api/v1/auth/**`, `/api/v1/api-keys/**` 접근 불가 | `SecurityConfig`에서 경로별 Filter chain 분리 (`/api/v1/tools/**`에만 `ApiKeyAuthFilter` 적용). Sprint 1 회귀 테스트(46건) 전부 재실행. |
| Redis Rate Limiting 미구축 인프라 | 2.2.5 지연 | 로컬 `docker-compose.yml`에 Redis 추가(Sprint 1 설정 재사용). Rate Limiter는 in-memory fallback 스위치 제공(테스트 환경용). |
| UI/UX handoff 지연 → FE 착수 지연 | 2.3.1 일정 밀림 | `2.1.1-a` 1차 handoff(발송 폼)를 3일차 오전까지 완료 → FE는 그 시점부터 UI 선구현 착수. 조회/검색 화면은 2차 handoff 이후. |
| 메시지 도구의 실제 입력 스키마(MCP send 파라미터)가 미확정 | 폼 필드 재작업 | UI/UX 착수 전 BE와 함께 `message/send` 입력 스키마를 1일차에 확정하고 `manifest.import-data.json`에 기록. |
| 웹 UI는 JWT, 외부 REST는 API Key 이중 인증 경로 혼동 | 보안 구멍, 테스트 누락 | 2.2.2 컨트롤러 문서에 각 경로의 인증 방식을 명시. Sprint DoD 체크리스트에 "양쪽 경로 수동 동작 확인" 항목 포함. |

---

## Sprint 1 회고 반영 (Keep / Problem / Try)

- **Keep**: Sprint 1 Handoff 순서(디자인 시스템 선행 → 페이지별 병렬)가 잘 동작 → Sprint 2도 `2.1.1-a` 선행 handoff 구조 유지.
- **Problem**: 미정. (Sprint 1 종료 보고 기준 전 DoD 완료 상태, 회고 추가 항목은 Sprint 2 킥오프 시 업데이트 예정)
- **Try**: 이번 스프린트부터 MCP 연동처럼 기술 리스크가 높은 항목은 1일차 스파이크 결과를 `pm/SPIKE_NOTE.md`(신규)로 기록해 다음 스프린트 추정에 반영.

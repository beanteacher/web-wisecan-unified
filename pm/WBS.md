# WBS — Wisecan MCP Connector B2C

## Phase 1 — 기반 구축 (Sprint 1~2)

| WBS ID | 작업명 | 담당 | 산출물 | DoD | 의존성 |
|--------|--------|------|--------|-----|--------|
| 1.1.1 | 디자인 시스템 초기화 (색상, 타이포, 간격) | UI/UX | figma-manifests/design-system-init/ | manifest.import-data.json 작성 완료, 색상·타이포·간격 토큰 정의 | - |
| 1.1.2 | 인증 화면 와이어프레임 (로그인/회원가입) | UI/UX | figma-manifests/auth-wireframe/ | 로그인·회원가입 PC/Mobile 프레임, 에러 상태 포함 | 1.1.1 |
| 1.1.3 | 대시보드·API Key 화면 와이어프레임 | UI/UX | figma-manifests/dashboard-wireframe/ | 대시보드·API Key 관리 PC/Mobile 프레임 | 1.1.1 |
| 1.2.1 | Member 엔티티 + Repository | Backend | domain/Member.java, repository/ | 컴파일 성공, @DataJpaTest 통과 | - |
| 1.2.2 | 회원가입 API | Backend | auth/register 엔드포인트 | POST /api/v1/auth/register 201 반환, 비밀번호 BCrypt 해시, 단위+통합 테스트 | 1.2.1 |
| 1.2.3 | 로그인 API + JWT 발급 | Backend | auth/login 엔드포인트, JwtProvider | POST /api/v1/auth/login JWT 토큰 반환, 만료 시간 설정, 테스트 통과 | 1.2.2 |
| 1.2.4 | JWT 필터 + SecurityConfig 완성 | Backend | JwtAuthenticationFilter | 인증 필요 API에 401 반환, 유효 토큰 시 200, 테스트 통과 | 1.2.3 |
| 1.2.5 | ApiKey 엔티티 + CRUD API | Backend | api-key 패키지 전체 | 발급(POST)·목록(GET)·비활성화(PATCH) 동작, Key 해시 저장, 테스트 통과 | 1.2.4 |
| 1.2.6 | ApiUsage 엔티티 + 조회 API | Backend | usage 패키지 전체 | 사용 이력 기록·조회 동작, 테스트 통과 | 1.2.5 |
| 1.3.1 | 공통 레이아웃 (Header/Footer/Sidebar) | Frontend | widgets/header, footer, sidebar | 네비게이션 동작, 반응형, 빌드 성공 | 1.1.1 handoff |
| 1.3.2 | 랜딩 페이지 구현 | Frontend | app/(public)/page.tsx | 히어로·기능소개·요금·CTA 섹션, 반응형, 빌드 성공 | 1.1.2 handoff |
| 1.3.3 | 회원가입/로그인 페이지 | Frontend | features/auth/ | 폼 유효성 검사, API 연동, 에러 표시, JWT 저장 | 1.1.3 handoff, 1.2.3 |
| 1.3.4 | API Key 관리 페이지 | Frontend | features/api-key/ | 발급·목록·비활성화 UI, API 연동 | 1.1.4 handoff, 1.2.5 |
| 1.3.5 | 대시보드 페이지 | Frontend | app/(auth)/dashboard/ | 사용량 요약 카드, 최근 이력 테이블, API 연동 | 1.1.4 handoff, 1.2.6 |

## Phase 2 — 핵심 기능 (Sprint 3~4)

| WBS ID | 작업명 | 담당 | 산출물 | DoD | 의존성 |
|--------|--------|------|--------|-----|--------|
| 2.1.1 | 도구 UI 와이어프레임 (메시지/파일) | UI/UX | figma-manifests/tools-wireframe/ | 메시지 발송·조회·통계, 파일 변환 PC/Mobile 프레임 | 1.1.1 |
| 2.2.1 | MCP 서버 연동 모듈 (stdio 프로세스 관리) | Backend | mcp 패키지 | MCP 도구 호출·응답 파싱, 프로세스 lifecycle 관리, 테스트 통과 | 1.2.4 |
| 2.2.2 | 메시지 도구 프록시 API | Backend | tools/message 엔드포인트 | send/get_result/search REST API 동작, API Key 인증, 사용량 기록 | 2.2.1 |
| 2.2.3 | 메시지 통계 프록시 API | Backend | tools/message/stats 엔드포인트 | stat_summary/daily_report REST API 동작 | 2.2.1 |
| 2.2.4 | 파일 변환 프록시 API | Backend | tools/file 엔드포인트 | md-to-docx/md-to-pdf REST API + 파일 다운로드 동작 | 2.2.1 |
| 2.2.5 | API Key 인증 미들웨어 (외부 호출용) | Backend | ApiKeyAuthFilter | X-API-Key 헤더 인증, Rate Limiting (Redis), 테스트 통과 | 1.2.5 |
| 2.3.1 | 메시지 발송/조회 UI | Frontend | features/message-tool/ | 발송 폼, 결과 조회, 검색 필터, API 연동 | 2.1.1 handoff, 2.2.2 |
| 2.3.2 | 메시지 통계 UI | Frontend | features/message-stats/ | 통계 요약 카드, 일일 리포트 표, 차트 | 2.1.1 handoff, 2.2.3 |
| 2.3.3 | 파일 변환 UI | Frontend | features/file-tool/ | MD 입력, 변환 실행, 파일 다운로드 | 2.1.1 handoff, 2.2.4 |

## Phase 3 — 확장 (Sprint 5~6)

| WBS ID | 작업명 | 담당 | 산출물 | DoD | 의존성 |
|--------|--------|------|--------|-----|--------|
| 3.1.1 | 사용량 상세 통계 차트 | Frontend | features/usage/ 확장 | 일별/도구별 차트, 기간 필터 | 2.3.2 |
| 3.2.1 | 에이전트 진단 프록시 API | Backend | tools/agent 엔드포인트 | 5개 진단 도구 REST API 동작 | 2.2.1 |
| 3.2.2 | 에이전트 진단 UI | Frontend | features/agent-tool/ | 설정 분석, 로그 분석, 종합 진단 UI | 3.2.1 |
| 3.3.1 | API 문서 페이지 | Frontend | app/(public)/docs/ | API 사용법, 코드 예제 | 2.2.2 |
| 3.3.2 | 요금제 설계 + 결제 연동 | Backend+FE | 요금 패키지 | 요금제 모델, 결제 페이지 | Phase 2 완료 |

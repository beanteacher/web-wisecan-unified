# MASTER PLAN — Wisecan MCP Connector B2C

## 비전
MCP(Model Context Protocol) 기반 메시지 발송·에이전트 진단·파일 변환 도구를 웹 UI + REST API로 제공하는 B2C SaaS 서비스

## 목표
1. 사용자가 웹 화면에서 23개 MCP 도구를 직접 사용할 수 있다
2. API Key를 발급받아 외부 시스템에서 REST API로 도구를 호출할 수 있다
3. 외부 빌더(IMWEB 등)로 홍보 페이지를 별도 운영하고, 가입/로그인으로 연결한다

## 성공 지표 (KPI)
- 회원가입 전환율: 랜딩 → 가입 15% 이상
- API Key 발급률: 가입 → Key 발급 60% 이상
- 도구 사용률: Key 발급 후 7일 내 1회 이상 호출 80%

## MVP 범위

### 포함 (MVP)
| 영역 | 범위 |
|------|------|
| 홍보 | 외부 빌더(IMWEB 등)로 별도 제작 — 이 프로젝트 범위 밖 |
| 인증 | 회원가입, 로그인, JWT 인증 |
| API Key | 발급, 목록 조회, 비활성화, 사용량 조회 |
| 도구 UI | 메시지 발송(send, get_result, search) |
| 도구 UI | 메시지 통계(stat_summary, daily_report) |
| 도구 UI | 파일 변환(md_to_docx, md_to_pdf) |
| API Gateway | API Key 인증 기반 REST API 프록시 |
| 대시보드 | API 사용량 요약, 최근 호출 이력 |

### 제외 (비MVP → Phase 2+)
| 영역 | 사유 |
|------|------|
| 에이전트 진단 도구 UI | 서버 접근 권한 이슈, MVP 이후 |
| 요금제/결제 | 초기 무료 운영 후 도입 |
| 팀/조직 관리 | 개인 사용자 우선 |
| 이미지 생성 도구 | 우선순위 낮음 |
| OAuth 소셜 로그인 | Phase 2 |
| 알림(이메일/슬랙) | Phase 2 |

## 단계별 마일스톤

### Phase 1 — 기반 구축 (Sprint 1~2)
- 인증 시스템 (회원가입/로그인/JWT)
- API Key CRUD
- 기본 레이아웃/네비게이션
- 대시보드

### Phase 2 — 핵심 기능 (Sprint 3~4)
- 메시지 도구 UI (발송, 조회, 검색)
- 메시지 통계 UI (통계 요약, 일일 리포트)
- API Gateway (API Key 인증 + MCP 프록시)
- 대시보드

### Phase 3 — 파일·확장 (Sprint 5~6)
- 파일 변환 도구 UI
- 사용량 상세 통계/차트
- 에이전트 진단 도구 (비MVP, 여건 시)
- 요금제 설계

## 릴리즈 게이트
- 기능 게이트: MVP 기능 100% 구현 + DoD 충족
- 품질 게이트: 주요 API 테스트 통과, TypeScript 에러 0
- 보안 게이트: JWT 인증 정상, API Key 해시 저장, SQL Injection 방어
- 운영 게이트: Docker Compose 로컬 실행 가능

## UI/UX 레퍼런스 가이드

### 디자인 레퍼런스 소스
| 소스 | URL | 용도 | 비용 |
|------|-----|------|------|
| HyperUI | hyperui.dev/components/marketing | 히어로, 기능소개, 요금, CTA, FAQ, Footer 등 랜딩 섹션 | 무료 |
| shadcn/ui Blocks | ui.shadcn.com/blocks | 대시보드, 로그인, 사이드바 레이아웃 | 무료 |
| shadcn/ui Charts | ui.shadcn.com/charts | 통계/차트 컴포넌트 | 무료 |

### 페이지별 레퍼런스 매핑
| 페이지 | 레퍼런스 소스 | 참고 카테고리 |
|--------|-------------|-------------|
| 랜딩 - 히어로 | HyperUI | Marketing → CTAs (7개 변형) |
| 랜딩 - 기능소개 | HyperUI | Marketing → Feature Grids (8개 변형) |
| 랜딩 - 요금표 | HyperUI | Marketing → Pricing (2개 변형) |
| 랜딩 - FAQ | HyperUI | Marketing → FAQs (6개 변형) |
| 랜딩 - Header | HyperUI | Marketing → Header (8개 변형) |
| 랜딩 - Footer | HyperUI | Marketing → Footers (24개 변형) |
| 로그인/회원가입 | shadcn/ui Blocks | Login / Signup 탭 |
| 대시보드 | shadcn/ui Blocks | Featured → Dashboard 블록 |
| 통계/차트 | shadcn/ui Charts | Area, Bar, Line, Pie Charts |

### UI/UX 작업 규칙
1. 위 레퍼런스에서 마음에 드는 컴포넌트를 선택한다
2. 우리 서비스(MCP Connector Tools)에 맞게 **벤치마킹하여 재설계**한다
3. Figma manifest에 레퍼런스 출처와 변경 사항을 명시한다
4. 단순 복사가 아닌, 우리 브랜드 색상·카피·레이아웃으로 커스터마이징한다
5. FE는 레퍼런스 소스의 코드를 기반으로 구현하되, UI/UX 시안에 맞게 수정한다

## 핵심 리스크
| 리스크 | 영향 | 완화책 |
|--------|------|--------|
| MCP 서버 연동 복잡도 | 일정 지연 | Backend에서 MCP를 직접 호출하지 않고 REST 래핑 |
| ky 등 프론트 라이브러리 Breaking Change | 빌드 실패 | 버전 고정 + 빌드 검증 |
| DB 스키마 충돌 (기존 MCP 테이블) | 데이터 무결성 | 별도 스키마(wisecan_unified)로 분리 |

## 기술 아키텍처

```
[Browser]
   │
   ├── 홍보 페이지 (SSR/SSG)
   ├── 대시보드 (CSR + React Query)
   └── 도구 UI (CSR + React Query)
         │
    [Next.js Frontend]
         │ REST API
         ▼
    [Spring Boot Backend]
         │
         ├── /api/v1/auth/**     → 인증 (JWT)
         ├── /api/v1/api-keys/** → API Key 관리
         ├── /api/v1/tools/**    → 도구 프록시 (MCP 래핑)
         └── /api/v1/usage/**    → 사용량 조회
         │
         ├── MySQL (wisecan_unified) → 회원, API Key, 사용 이력
         ├── Redis              → 세션 캐시, Rate Limiting
         └── MCP Server (stdio) → 실제 도구 실행
```

## 데이터 모델 (핵심 엔티티)

### Member (회원)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 회원 ID |
| email | VARCHAR(255) UNIQUE | 이메일 (로그인) |
| password | VARCHAR(255) | BCrypt 해시 |
| name | VARCHAR(100) | 이름 |
| role | ENUM(USER, ADMIN) | 권한 |
| status | ENUM(ACTIVE, SUSPENDED) | 상태 |
| created_at | DATETIME | 가입일 |

### ApiKey (API 키)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | Key ID |
| member_id | BIGINT FK | 소유자 |
| key_name | VARCHAR(100) | 별칭 |
| key_prefix | VARCHAR(8) | 식별 접두사 (wc_xxxx) |
| key_hash | VARCHAR(255) | SHA-256 해시 |
| status | ENUM(ACTIVE, REVOKED) | 상태 |
| last_used_at | DATETIME | 마지막 사용 |
| created_at | DATETIME | 생성일 |

### ApiUsage (사용 이력)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 이력 ID |
| api_key_id | BIGINT FK | 사용 Key |
| tool_name | VARCHAR(100) | 호출 도구명 |
| status | ENUM(SUCCESS, FAIL) | 결과 |
| response_time_ms | INT | 응답 시간 |
| error_message | TEXT | 에러 메시지 |
| called_at | DATETIME | 호출 시각 |

## 페이지 구성

### 공개 페이지 (인증 불필요)
| 경로 | 설명 |
|------|------|
| `/` | 로그인 페이지로 리다이렉트 (랜딩은 IMWEB 등 외부 빌더로 별도 운영) |
| `/login` | 로그인 |
| `/register` | 회원가입 |
| `/docs` | API 문서 (Phase 2) |

### 인증 필요 페이지
| 경로 | 설명 |
|------|------|
| `/dashboard` | 대시보드 (사용량 요약, 최근 이력) |
| `/api-keys` | API Key 관리 (발급, 목록, 비활성화) |
| `/tools/message` | 메시지 도구 (발송, 조회, 검색) |
| `/tools/message/stats` | 메시지 통계 (요약, 일일 리포트) |
| `/tools/file` | 파일 변환 (MD→DOCX, MD→PDF) |
| `/settings` | 계정 설정 |

## 백엔드 API 설계

### 인증
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/register` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 → JWT 발급 |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |

### API Key 관리
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/api-keys` | Key 발급 |
| GET | `/api/v1/api-keys` | 내 Key 목록 |
| PATCH | `/api/v1/api-keys/{id}/revoke` | Key 비활성화 |
| GET | `/api/v1/api-keys/{id}/usage` | Key별 사용량 |

### 도구 프록시
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/tools/message/send` | 메시지 발송 |
| GET | `/api/v1/tools/message/{msgId}` | 발송 결과 조회 |
| GET | `/api/v1/tools/message/search` | 다중 조건 검색 |
| GET | `/api/v1/tools/message/stats` | 통계 요약 |
| GET | `/api/v1/tools/message/daily-report` | 일일 리포트 |
| POST | `/api/v1/tools/file/md-to-docx` | MD→DOCX 변환 |
| POST | `/api/v1/tools/file/md-to-pdf` | MD→PDF 변환 |

### 사용량
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/usage/summary` | 사용량 요약 |
| GET | `/api/v1/usage/history` | 호출 이력 |

## 프론트엔드 FSD 구조

```
src/
├── app/                              # 라우팅
│   ├── (public)/                     # 공개 페이지 그룹
│   │   ├── page.tsx                  # → /login 리다이렉트
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (auth)/                       # 인증 필요 페이지 그룹
│   │   ├── dashboard/page.tsx
│   │   ├── api-keys/page.tsx
│   │   ├── tools/
│   │   │   ├── message/page.tsx
│   │   │   ├── message/stats/page.tsx
│   │   │   └── file/page.tsx
│   │   └── settings/page.tsx
│   ├── layout.tsx
│   └── providers.tsx
│
├── widgets/
│   ├── header/                       # 공통 헤더 (로고, 네비, 유저메뉴)
│   ├── sidebar/                      # 대시보드 사이드바
│   └── footer/                       # 공통 푸터
│
├── features/
│   ├── auth/                         # 로그인, 회원가입, 로그아웃
│   ├── api-key/                      # API Key 발급, 관리
│   ├── message-tool/                 # 메시지 발송/조회/검색
│   ├── message-stats/                # 메시지 통계/리포트
│   ├── file-tool/                    # 파일 변환
│   └── usage/                        # 사용량 조회
│
├── entities/
│   ├── member/                       # 회원 타입, API, UI
│   ├── api-key/                      # API Key 타입, API, UI
│   └── tool/                         # 도구 메타 정보
│
└── shared/
    ├── api/client.ts
    ├── ui/
    ├── lib/
    ├── hooks/
    ├── constants/
    └── types/
```

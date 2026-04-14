# OVERALL DESIGN — Wisecan Unified Messaging Platform (B2C + MCP)

> 본 문서는 **전체 서비스 설계서**입니다. MCP 상세 설계는 `MCP_DESIGN.md`로 분리 보고합니다.
> 기존 `pm/MASTER_PLAN.md` 및 `backend/src/main/java/com/wisecan/unified/mcp/PingTool.java` 등 현재 구현물은 **참고용**이며, 본 설계서가 새 기준선입니다.
> 작성일: 2026-04-14

---

## 1. 비전 · 목표

### 1.1 비전
국내 솔라피(solapi.com)를 벤치마킹하여, **SMS / LMS / MMS / 알림톡(KKO) / 브랜드 메시지 / RCS** 를 단일 계정·단일 API로 발송할 수 있는 **B2C SaaS 메시징 플랫폼**을 제공한다.
추가로 **MCP(Model Context Protocol)** 인터페이스를 기본 탑재하여, 사용자의 AI 에이전트(Claude Desktop, Cursor, Gemini CLI 등)가 직접 메시지 발송·조회·통계를 호출할 수 있는 **AI-Native Messaging Platform** 으로 차별화한다.

### 1.2 서비스 포지셔닝
| 축 | 솔라피 (벤치마크) | Wisecan Unified (본 서비스) |
|----|------------------|-----------------------------|
| 채널 | SMS/LMS/MMS/KKO/RCS | 동일 + MCP Tool 기반 자동화 |
| API | REST + SDK | REST + SDK + **MCP Server (Streamable HTTP)** |
| 주 사용자 | 개발자, 마케터 | 개발자, 마케터, **AI 에이전트 사용자** |
| 차별점 | 안정성/저가 | AI 에이전트 자연어로 발송·리포트 |

### 1.3 KPI
| KPI | 목표 |
|-----|------|
| 가입 → 발신번호 등록 완료율 | 60% (7일 내) |
| 가입 → 첫 메시지 발송 성공률 | 40% (14일 내) |
| API Key 발급 사용자의 MCP 도구 1회 이상 사용률 | 30% (30일 내) |
| 월간 메시지 발송 성공률 | 99.5% 이상 |
| 결과 코드 미해석 비율 | 0.5% 미만 |

---

## 2. 솔라피 벤치마킹 분석 (핵심 기능 매핑)

| 기능 | 솔라피 | Wisecan Unified | MVP |
|------|-------|------------------|-----|
| 회원가입/로그인 | 이메일 + 소셜 | 이메일 + JWT (소셜은 Phase 2) | O |
| 발신번호 등록 | 서류·ARS | 서류·ARS (Phase 2 자동화) | O |
| 메시지 발송 | SMS/LMS/MMS/KKO/RCS | 동일 | O |
| 예약 발송 | O | O | O |
| 대체발송(Fallback) | O | O (KKO→SMS, RCS→SMS 등) | O |
| 그룹 발송 | O | O (`SEND_GROUP_TRAN`) | O |
| 주소록 | O | O | O |
| 메시지 템플릿 | O | O (KKO/RCS/문자) | O |
| 카카오 발신프로필·템플릿 검수 | O | O | O |
| RCS 브랜드·템플릿·이미지 | O | O (`RCS_IMAGE`) | O |
| 발송 결과 조회 | O | O (`SEND_*_LOG`) | O |
| 통계/리포트 | O | O (일/주/월, 채널별) | O |
| 잔여 포인트·충전·요금제 | O | Phase 2 (MVP 무료 할당) | X |
| 수신 거부 관리 | O | O (`SEND_REJECT`) | O |
| 개발자 콘솔/API 키 | O | O | O |
| Webhook | O | Phase 2 | X |
| **MCP Server** | **없음** | **기본 탑재 (차별점)** | O |

---

## 3. 페르소나 · 유스케이스

### 3.1 페르소나
- **P1. 개발자 (SI/스타트업)**: REST API로 트랜잭션 메시지 발송 (회원가입 인증, 주문 알림).
- **P2. 마케터**: 웹 콘솔에서 주소록 업로드 후 대량 발송, 결과 리포트 조회.
- **P3. AI 에이전트 사용자 (신규)**: Claude Desktop/Cursor 등에서 자연어로 "지난주 발송 실패 목록 뽑아줘" 수행.
- **P4. 운영자**: 발송 한도, 발신번호 승인, 수신거부 목록 관리.

### 3.2 핵심 유스케이스
1. **가입 → 발신번호 등록 → 테스트 발송 → 운영 발송** (개발자)
2. **주소록 업로드 → 카카오 알림톡 대량 발송 → 실패 건 SMS 대체 발송 → 결과 리포트** (마케터)
3. **"어제 실패한 알림톡 원인별로 집계해줘" → MCP Tool 자동 호출 → 테이블로 응답** (AI 에이전트 사용자)
4. **RCS 이미지 업로드 → RCS 브랜드 메시지 발송 → 결과 추적** (마케터)

---

## 4. 서비스 기능 목록 (F-ID)

### 4.1 인증/계정
| F-ID | 기능 | MVP |
|------|------|-----|
| F-AUTH-01 | 이메일 회원가입 / 이메일 인증 | O |
| F-AUTH-02 | 로그인 / JWT 발급 / Refresh | O |
| F-AUTH-03 | 로그아웃 / 토큰 블랙리스트 (이미 구현) | O |
| F-AUTH-04 | 비밀번호 재설정 | O |
| F-AUTH-05 | 계정 정보 수정 / 탈퇴 | O |
| F-AUTH-06 | 소셜 로그인 (카카오/구글) | Phase 2 |

### 4.2 발신번호·KISA·브랜드
| F-ID | 기능 | MVP |
|------|------|-----|
| F-SEND-01 | 발신번호 등록/심사/승인 | O |
| F-SEND-02 | KISA 식별코드 (`kisa_code`) 관리 | O |
| F-SEND-03 | 카카오 발신프로필 등록 (`kko_sender_key`) | O |
| F-SEND-04 | 카카오 템플릿 등록/검수 상태 조회 | O |
| F-SEND-05 | RCS 브랜드·대행사·템플릿 등록 | Phase 2 (초기 수동) |
| F-SEND-06 | RCS 이미지 업로드 (`RCS_IMAGE`) | Phase 2 |

### 4.3 메시지 발송
| F-ID | 기능 | 테이블 | MVP |
|------|------|--------|-----|
| F-MSG-01 | SMS 단건/다건 | `SEND_SMS_TRAN` | O |
| F-MSG-02 | LMS / MMS (첨부파일) | `SEND_MMS_TRAN` | O |
| F-MSG-03 | 알림톡 KAT (텍스트) | `SEND_KKO_TRAN` | O |
| F-MSG-04 | 알림톡 KAI (이미지) | `SEND_KKO_TRAN` | O |
| F-MSG-05 | 브랜드 메시지 KBT/KBI/KBW/KBL/KBM/KBC/KBA/KBP | `SEND_KKO_TRAN` | Phase 2 |
| F-MSG-06 | RCS RSMS/RLMS | `SEND_RCS_TRAN` | Phase 2 |
| F-MSG-07 | RCS RSMMS/RCMMS (Medium/Small, 2~6장) | `SEND_RCS_TRAN` | Phase 2 |
| F-MSG-08 | RCS RTF (자유형/선택형) | `SEND_RCS_TRAN` | Phase 2 |
| F-MSG-09 | 그룹 발송 (`SEND_GROUP_TRAN`) | `SEND_GROUP_TRAN` | O |
| F-MSG-10 | 예약 발송 (`request_date`) | 전 발송 테이블 | O |
| F-MSG-11 | 대체 발송 체인 (`fb_type`, `origin_msg_id`) | 전 발송 테이블 | O |
| F-MSG-12 | 재시도 정책 (`retry_count`) | 전 발송 테이블 | O |

### 4.4 조회/리포트
| F-ID | 기능 | 원본 | MVP |
|------|------|------|-----|
| F-RPT-01 | 메시지 단건 조회 (상태·결과코드) | `SEND_*_TRAN` / `SEND_*_LOG` | O |
| F-RPT-02 | 조건 검색 (수신번호/기간/결과코드/채널) | 동일 | O |
| F-RPT-03 | 일간·주간·월간 통계 | `SEND_*_LOG` 집계 | O |
| F-RPT-04 | 채널/통신사(`result_net_id`)별 리포트 | `SEND_*_LOG` | O |
| F-RPT-05 | 실패 사유 Top-N (`result_code`) | `SEND_*_LOG` | O |
| F-RPT-06 | 대체발송 체인 추적 | `origin_msg_id` | O |

### 4.5 운영·부가
| F-ID | 기능 | 테이블 | MVP |
|------|------|--------|-----|
| F-OPS-01 | 수신 거부 관리 | `SEND_REJECT` | O |
| F-OPS-02 | Agent 이중화 Lock | `SEND_DUAL_LOCK` | O (Agent 쪽) |
| F-OPS-03 | Webhook (발송/결과 이벤트) | - | Phase 2 |
| F-OPS-04 | 요금/포인트 | - | Phase 2 |
| F-OPS-05 | 주소록 | `ADDRESS_BOOK` (신규) | O |
| F-OPS-06 | 메시지 템플릿 (앱 내 관리) | `MSG_TEMPLATE` (신규) | O |

### 4.6 MCP (차별점)
| F-ID | 기능 | MVP |
|------|------|-----|
| F-MCP-01 | MCP Streamable HTTP 서버 공개 | O |
| F-MCP-02 | API Key 기반 MCP 인증 | O |
| F-MCP-03 | 발송 Tool (채널별) | O |
| F-MCP-04 | 조회·통계 Tool | O |
| F-MCP-05 | 템플릿·주소록 Tool | O |
| F-MCP-06 | Resource (발신번호 목록, 결과코드 표 등) | O |
| F-MCP-07 | Prompt (예약 발송 가이드, 트러블슈팅) | Phase 2 |

> MCP Tool의 **완전한 스펙은 `MCP_DESIGN.md`** 에서 상세화.

---

## 5. 아키텍처

### 5.1 논리 아키텍처

```
 ┌────────────────────────────────────────────────────────────┐
 │                       Client Layer                         │
 │  Web (Next.js FSD)   AI Agent (Claude/Cursor)   REST SDK   │
 └────────────┬──────────────────────┬──────────────┬─────────┘
              │HTTPS                 │MCP (Streamable HTTP)
              ▼                      ▼
 ┌────────────────────────────────────────────────────────────┐
 │                    Spring Boot Backend                     │
 │  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐    │
 │  │ REST API    │  │ MCP Server   │  │ Auth (JWT+Key)  │    │
 │  └──────┬──────┘  └──────┬───────┘  └────────┬────────┘    │
 │         └──────┬─────────┴──────────────────┘              │
 │                ▼                                           │
 │        Message Dispatch Service                            │
 │  (SMS/MMS/KKO/RCS Router · Fallback · Scheduler)           │
 └─────────────┬──────────────────┬──────────────┬────────────┘
               │                  │              │
         ┌─────▼─────┐      ┌─────▼─────┐  ┌─────▼─────┐
         │  MySQL    │      │   Redis   │  │  S3/MinIO │
         │  (wisecan)│      │ Cache/RL  │  │ MMS/RCS   │
         └─────┬─────┘      └───────────┘  └───────────┘
               │ INSERT  SEND_*_TRAN
               ▼
         ┌──────────────────────────────────────┐
         │  External Send Agent (별도 프로세스) │
         │   (KT/LGU+/KKAO/RCS 중계사 연결)     │
         └──────────────────────────────────────┘
```

- **핵심 설계 원칙**: 백엔드는 DB의 `SEND_*_TRAN` 테이블에 **INSERT만** 수행한다. 실제 단말 전송은 외부 **Send Agent** 가 테이블을 폴링/컨슘하여 처리하며, 결과를 `SEND_*_LOG` 로 적재한다. 이 방식은 솔라피/문자 중계 표준 구조와 일치한다.
- 백엔드는 결과 코드·상태·통계만을 DB에서 조회/집계한다.

### 5.2 배포 구성 (단일 모듈 기준)
```
web-wisecan-unified/
├── backend/    (Spring Boot 3.4 + Spring AI 1.1.2 MCP webmvc)
├── frontend/   (Next.js + FSD)
└── docker-compose.yml (MySQL / Redis / MinIO / Backend / Frontend)
```
- 현재 `backend/build.gradle`의 단일 모듈 구조 유지. 트래픽·조직 성장 시 `init-multi-module.md` 규칙에 따라 `common / domain / api / mcp` 4 모듈로 분리 가능(Phase 3).

---

## 6. 도메인 모델

### 6.1 본 서비스(B2C SaaS) 전용 테이블 — `wisecan_unified` 스키마

| 테이블 | 목적 |
|--------|------|
| `member` | 회원 (이미 구현) |
| `api_key` | API Key (이미 구현) |
| `api_usage` | API 호출 이력 (이미 구현) |
| `sender_number` | 등록 발신번호 (심사 상태 포함) |
| `kko_sender_profile` | 카카오 발신프로필 (키/검수 상태) |
| `kko_template` | 카카오 템플릿 |
| `rcs_brand` | RCS 브랜드/대행사 정보 |
| `rcs_template` | RCS 템플릿 (msgbase_id) |
| `address_book` | 주소록 헤더 |
| `address_book_contact` | 주소록 연락처 |
| `msg_template` | 앱 내 저장 메시지 템플릿 (SMS/LMS 치환형) |
| `quota` | 월 발송 한도/요금제 (Phase 2) |

### 6.2 발송/로그 테이블 — 첨부 스펙 **그대로** 수용

> 본 스펙은 Send Agent 와의 계약이며, 컬럼·타입·기본값·제약을 임의 변경 금지.

#### 6.2.1 MyBatis Mapper 정책 (중요 결정)
- **one-table mapper(통합 `SEND_TRAN` / `SEND_LOG`) 대신 채널별 mapper** 방식 채택.
  - 이유: RCS/KKO 의 고유 컬럼이 매우 많아, 통합 테이블에서 NULL 비율이 과도하고 인덱스 설계가 비효율적.
- 따라서 다음 테이블을 **필수 생성**:
  - `SEND_GROUP_TRAN` (그룹 헤더)
  - `SEND_SMS_TRAN` / `SEND_SMS_LOG`
  - `SEND_MMS_TRAN` / `SEND_MMS_LOG` (LMS 포함)
  - `SEND_KKO_TRAN` / `SEND_KKO_LOG`
  - `SEND_RCS_TRAN` / `SEND_RCS_LOG`
  - `RCS_IMAGE`
  - `SEND_DUAL_LOCK`, `SEND_REJECT`
- 통합 `SEND_TRAN / SEND_LOG` 는 **내부 통계용 View** 로 제공 (`UNION ALL` 기반).

#### 6.2.2 컬럼 규약 (첨부 스펙에서 확정된 사항)
| 영역 | 규약 |
|------|------|
| 상태 (`message_state`) | `0 대기 / 1 fetched / 2 submitted / 3 finished / 4 logfail` |
| 일시 | `create_date`, `update_date`, `request_date`(예약), `result_deliver_date`(통신사 처리) |
| 식별자 | `msg_id BIGINT AUTO_INCREMENT`, `group_id INT` |
| 대체발송 체인 | `fb_type`, `origin_msg_id`, `origin_msg_type` |
| RCS 광고 | `msg_header=1` 이면 `msg_footer` 필수 |
| RCS 복사/재시도 | `msg_copyallowed(0/1)`, `expiry_option(1~4)` |
| 카카오 msg_sub_type | `KAT/KAI/KBT/KBI/KBW/KBL/KBC/KBP/KBM/KBA` |
| RCS msg_sub_type | `RSMS/RLMS/RSMMS/RCMMS/RTF` |
| MMS 첨부 | `file_count`, `file_path` (S3 경로 또는 파일명) |
| 예비 컬럼 | `etc_char_1..4`, `etc_int_1..4` — 백엔드가 trace_id/webhook 매핑 등에 사용 가능 |

#### 6.2.3 인덱스 권고 (백엔드 부가 설계)
| 테이블 | 인덱스 |
|--------|--------|
| 모든 `SEND_*_TRAN` | `(message_state, request_date)` — Agent fetch 쿼리용 |
| 모든 `SEND_*_LOG`  | `(user_id, create_date DESC)`, `(destaddr, create_date)`, `(result_code)` |
| `SEND_GROUP_TRAN` | `(member_no, create_date DESC)` |
| `RCS_IMAGE` | `(brand_id, use_yn)`, `(upload_state, update_date)` |
| `SEND_REJECT` | `UNIQUE(phone)` |

### 6.3 결과 코드 매핑 테이블 (신규 백엔드 개념)

첨부된 7.1~7.3 결과 코드를 **DB 상수 테이블** `result_code_dict` 로 이관하여, 다국어/설명 유지.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| code | VARCHAR(5) PK | 결과 코드 (예: `1000`, `4000`, `6001`) |
| category | ENUM | `AGENT_INTERNAL / SMS_MMS_KKO / KKO_BRAND` |
| name | VARCHAR(80) | `E_OK`, `NoSendAvailable` 등 |
| description_ko | VARCHAR(500) | 한글 설명 |
| severity | ENUM | `SUCCESS / CLIENT_ERROR / CARRIER_ERROR / SYSTEM_ERROR / UNKNOWN` |
| retriable | BOOLEAN | 재시도 권장 여부 |

- `E_OK(1000)` → severity=SUCCESS
- `E_TIMEOUT(3002), E_SYS_ERR(4000), E_TELECOM_ERR(4002)` → CARRIER_ERROR, retriable=true
- `E_INVALIDDST(1109), E_FORMAT_DESTIN_LEN(1115)` → CLIENT_ERROR, retriable=false
- `6000~6071` → KKO_BRAND, 대부분 CLIENT_ERROR
- 내부 코드 `0101~0403` → AGENT_INTERNAL

---

## 7. API 설계 (REST)

### 7.1 URL 규칙
- Base: `/api/v1`
- 인증: `Authorization: Bearer <JWT>` (웹 UI) 또는 `X-API-Key: wc_...` (SDK)

### 7.2 엔드포인트 요약

#### 인증 / 계정
| METHOD | PATH | 설명 |
|--------|------|------|
| POST | `/auth/register` | 회원가입 |
| POST | `/auth/login` | 로그인 |
| POST | `/auth/logout` | 로그아웃 (블랙리스트, 구현 완료) |
| POST | `/auth/refresh` | 토큰 갱신 |
| GET  | `/me` | 내 정보 |

#### API Key
| METHOD | PATH | 설명 |
|--------|------|------|
| POST | `/api-keys` | Key 발급 |
| GET | `/api-keys` | 목록 |
| PATCH | `/api-keys/{id}/revoke` | 폐기 |
| GET | `/api-keys/{id}/usage` | 사용량 |

#### 발신번호·KKO·RCS 메타
| METHOD | PATH | 설명 |
|--------|------|------|
| POST | `/senders` | 발신번호 등록 신청 |
| GET | `/senders` | 목록/심사 상태 |
| POST | `/kko/profiles` | 카카오 발신프로필 등록 |
| GET | `/kko/profiles` | 목록 |
| POST | `/kko/templates` | 카카오 템플릿 등록 |
| GET | `/kko/templates` | 목록/검수 상태 |
| POST | `/rcs/brands` | RCS 브랜드 등록 |
| POST | `/rcs/images` | RCS 이미지 업로드 (`RCS_IMAGE`) |

#### 메시지 발송
| METHOD | PATH | 설명 | 대상 테이블 |
|--------|------|------|-------------|
| POST | `/messages/sms` | SMS 발송 | `SEND_SMS_TRAN` |
| POST | `/messages/lms` | LMS 발송 | `SEND_MMS_TRAN` (msg_sub_type=LMS) |
| POST | `/messages/mms` | MMS 발송 (file_count≥1) | `SEND_MMS_TRAN` |
| POST | `/messages/kko/alimtalk` | 알림톡 KAT/KAI | `SEND_KKO_TRAN` |
| POST | `/messages/kko/brand` | 브랜드 메시지 KBT~KBA | `SEND_KKO_TRAN` |
| POST | `/messages/rcs` | RCS (sub_type 파라미터로 분기) | `SEND_RCS_TRAN` |
| POST | `/messages/group` | 그룹 발송 (N개 수신번호 일괄) | `SEND_GROUP_TRAN` + 하위 |

#### 조회·통계
| METHOD | PATH | 설명 |
|--------|------|------|
| GET | `/messages/{msgId}` | 단건 상세 (channel 자동 판별) |
| GET | `/messages` | 조건 검색 (채널/기간/상태/수신번호/결과코드) |
| GET | `/groups/{groupId}` | 그룹 집계 |
| GET | `/reports/daily` | 일별 통계 |
| GET | `/reports/failure-top` | 실패 사유 Top-N |
| GET | `/reports/carrier` | 통신사별 (`result_net_id`) |

#### 운영
| METHOD | PATH | 설명 |
|--------|------|------|
| POST | `/reject` | 수신거부 등록 |
| DELETE | `/reject/{phone}` | 해제 |
| GET | `/reject` | 목록 |

### 7.3 공통 응답 포맷
- 성공: `ApiResponse<T> { success, data, message, timestamp }`
- 실패: `ApiResponse { success:false, message, timestamp, errorCode, details? }`
- 결과 코드 필드(예: `E_OK`, `3002` 등)는 DTO 에 **원본 그대로** 전달하고, 별도로 `resultCodeMeta: { severity, retriable, description }` 를 함께 반환.

### 7.4 발송 DTO 예 (SMS)
```json
POST /api/v1/messages/sms
{
  "destaddr": "01012345678",
  "callback": "021234567",
  "senderId": 12,                 // sender_number.id (KISA 코드 자동 매핑)
  "sendMsg": "본문",
  "requestDate": "2026-04-15T09:00:00", // 선택 (예약)
  "billCode": "ORDER-0001"        // 선택
}
→ 201 { msgId: 100234, groupId: null, messageState: 0 }
```

---

## 8. 프론트엔드 설계 (FSD)

### 8.1 페이지 맵 (`app/`)
```
(public)
  /              → /login 리다이렉트
  /login
  /register
  /forgot-password
  /verify-email

(auth)
  /dashboard
  /senders                          # 발신번호
  /kko/profiles , /kko/templates
  /rcs/brands , /rcs/images
  /address-book
  /templates                        # 앱 내 메시지 템플릿
  /messages/compose                 # 발송 작성
  /messages/compose/sms
  /messages/compose/lms
  /messages/compose/mms
  /messages/compose/kko
  /messages/compose/rcs
  /messages                         # 발송 이력/검색
  /messages/[msgId]                 # 단건 상세 (상태 타임라인 + 결과코드)
  /groups/[groupId]                 # 그룹 집계
  /reports/daily
  /reports/failures
  /reports/carriers
  /settings
  /settings/api-keys
  /settings/mcp                     # MCP 연결 가이드 (Claude/Cursor 설정 JSON 생성)
  /reject                           # 수신거부 관리
```

### 8.2 FSD 레이어
- `shared/` — `ky` 기반 API 클라이언트, 결과코드 배지 컴포넌트, 날짜 유틸.
- `entities/` — `member`, `apiKey`, `sender`, `kkoTemplate`, `rcsBrand`, `message`, `resultCode`.
- `features/` — `auth`, `api-key`, `compose-sms`, `compose-lms`, `compose-mms`, `compose-alimtalk`, `compose-brand`, `compose-rcs`, `address-book`, `template`, `message-search`, `reports-daily`, `reject`, `mcp-connect`.
- `widgets/` — `header`, `sidebar`, `compose-shell`(발송 작성 공통 셸), `message-detail-timeline`, `group-progress`.

### 8.3 핵심 UX 규칙
1. 발송 작성 페이지는 **실시간 바이트 카운터**와 **LMS/MMS 자동 전환 경고** 제공.
2. KKO 알림톡 작성 시 **템플릿 일치 검증 시뮬레이터** (코드 6029, 6030 사전 방지).
3. 메시지 상세는 상태 타임라인 (`create → fetched → submitted → finished`) 과 결과 코드 의미(색상·재시도 여부) 표시.
4. 대체발송 체인(`origin_msg_id`)을 **트리 뷰**로 표시.
5. RCS 발송 작성 시 광고성(`msg_header=1`) 선택 시 `msg_footer` 필수 입력 강제.

---

## 9. 보안 · 인증 · 권한

| 영역 | 정책 |
|------|------|
| 웹 로그인 | Email + BCrypt, JWT Access(15분) + Refresh(7일), 블랙리스트 Redis |
| API 접근 | API Key (prefix+hash), Redis Rate Limit (token bucket) |
| MCP 접근 | `X-API-Key` 헤더 또는 OAuth-style Bearer — 상세는 `MCP_DESIGN.md` |
| 발신번호 보호 | 사용자별 소유 발신번호만 선택 가능 (Service 레벨 검증) |
| 카카오/RCS 키 | `kko_sender_key`, `rcs_*_key` 는 DB에 AES-GCM 암호화 저장 |
| CORS | 프론트 도메인 화이트리스트 |
| 감사 로그 | `api_usage` + MDC(trace_id) + 요청/응답 바디 마스킹 |
| 민감정보 마스킹 | `destaddr` 마지막 4자리 마스킹 로그, 메시지 본문 100자 트렁케이트 |

---

## 10. 메시지 발송 파이프라인

### 10.1 동기 INSERT 흐름
1. REST API / MCP Tool → `MessageDispatchService.dispatch(request)`
2. 권한·할당량·수신거부(`SEND_REJECT`) 검증
3. 채널·sub_type 에 맞춰 해당 `SEND_*_TRAN` 에 INSERT (`message_state=0`)
4. (옵션) 대체발송이 요청된 경우 `fb_*` 컬럼 동일 행에 저장
5. 응답: `msgId`, `groupId`, 상태 `0`

### 10.2 비동기 결과 반영
- Send Agent 가 외부 전송 후 `message_state`, `result_code`, `result_deliver_date`, `result_net_id` 업데이트
- 백엔드는 **Polling 없이 DB 조회로만 응답**. Webhook(Phase 2)은 Outbox 패턴으로 추가.

### 10.3 예약 발송
- `request_date` > now 이면 Agent 가 fetch 하지 않음 → 자동 예약 성립.
- 취소: `message_state=0` 인 행만 `UPDATE message_state=4(logfail)` + 사유 코멘트 → `E_MSG_CANCEL(0111)` 반영.

### 10.4 대체 발송 체인
- 원 메시지 결과가 실패 severity 이면 백엔드 스케줄러(@Scheduled 1분)가 `fb_type` 존재 건을 찾아 fallback INSERT.
- fallback row 의 `origin_msg_id`, `origin_msg_type` 설정.

### 10.5 그룹 발송
- 먼저 `SEND_GROUP_TRAN` INSERT → `group_id` 획득
- 수신자 수만큼 채널별 `SEND_*_TRAN` INSERT (각 row 에 `group_id` 연결)
- 통계(`total_count`, `succ_count`, `succ_fb_count` ...)는 Agent 또는 야간 배치가 갱신.

---

## 11. 결과 코드 표준화 & UX

### 11.1 서버 측 표준화 컴포넌트
- `ResultCodeService` — `code` → `{category, severity, retriable, description}` 반환 (Redis 캐시).
- `MessageMapper.toDetailDto` 호출 시 자동 병합.

### 11.2 severity 매핑 규칙 (주요 예)
| 코드 | 의미 | severity | retriable |
|------|------|----------|-----------|
| 1000 | E_OK | SUCCESS | - |
| 1101~1104 | 계정/IP | CLIENT_ERROR | false |
| 1109 | 결번 | CLIENT_ERROR | false |
| 1115/1116 | 착신번호 포맷 | CLIENT_ERROR | false |
| 2000~2007 | 메시지 형식/미디어 | CLIENT_ERROR | false |
| 2050 | KISA 코드 | CLIENT_ERROR | false |
| 3000~3005 | 단말기/음영/OFF | CARRIER_ERROR | true |
| 4000~4004 | 서버/통신사 | SYSTEM_ERROR | true |
| 5301~5304 | 발신번호 세칙 | CLIENT_ERROR | false |
| 6000~6071 | KKO 전용 | CLIENT_ERROR 대부분 | false |
| 0101~0403 | Agent 내부 | SYSTEM_ERROR | 일부 true |

### 11.3 UI 표현
- `ResultCodeBadge` 컴포넌트: severity별 색상 (SUCCESS 녹색 / CLIENT 빨강 / CARRIER 주황 / SYSTEM 회색 / UNKNOWN 보라)
- 툴팁에 설명 + 재시도 권장 여부

---

## 12. 데이터 · 로그 · 통계

### 12.1 통계 소스
- 실시간: `SEND_*_TRAN` (당일 이전 상태 `0/1/2`)
- 종결: `SEND_*_LOG`
- 통합 뷰: `v_send_all` = `UNION ALL` of 4 LOG 테이블 (msg_type 구분)

### 12.2 집계 전략
- 백엔드는 **GROUP BY DATE(create_date)** 수준 쿼리만 직접 수행.
- 대용량(월 1억건 이상) 구간부터는 ClickHouse 또는 Materialized View 도입 (Phase 3).

### 12.3 보존 정책
- `SEND_*_TRAN` — 처리 완료 후 30일 뒤 파기 (Agent 담당)
- `SEND_*_LOG` — 3년 보존 (관계 법령)
- 개인정보(`destaddr`) — 만 3년 뒤 익명화 배치

---

## 13. 비기능 요구사항

| 구분 | 목표 |
|------|------|
| 가용성 | 99.9% (월 43분 이내 장애) |
| 발송 TPS | 초기 100 TPS, Phase 2 1,000 TPS |
| API p95 | < 300 ms (발송 INSERT) |
| MCP 응답 p95 | < 500 ms |
| 로그 검색 | 1년치 p95 < 2초 (인덱스 + 파티셔닝) |
| 관측 | Prometheus + Grafana + Actuator |
| 트레이싱 | Micrometer Tracing + OpenTelemetry (trace_id 를 `etc_char_1` 에 저장) |

---

## 14. 단계별 마일스톤

### Phase 1 — 기반 (Sprint 1~2, 4주)
- 인증/계정 완성 (MVP)
- API Key + Rate Limit
- 발신번호/KISA/KKO 프로필·템플릿 관리
- SMS/LMS/MMS 발송 + 조회 + 수신거부
- `SEND_SMS_TRAN/LOG`, `SEND_MMS_TRAN/LOG`, `SEND_GROUP_TRAN`, `SEND_REJECT`, `result_code_dict` 구축
- MCP v1 (SMS/LMS/MMS 발송·조회 Tool 8개)

### Phase 2 — 카카오·RCS (Sprint 3~5, 6주)
- KKO 알림톡(KAT/KAI) + 브랜드 메시지 (KBT~KBA)
- RCS RSMS/RLMS/RSMMS/RCMMS/RTF + `RCS_IMAGE`
- 대체발송·예약·그룹발송 고도화
- 일/주/월 리포트, 통신사별 리포트, 실패 Top-N
- MCP v2 (KKO/RCS/리포트 Tool 추가)

### Phase 3 — 운영·확장 (Sprint 6~8, 6주)
- Webhook, 요금제/포인트, 주소록 고도화
- 소셜 로그인
- 대용량 집계(ClickHouse) 검토
- 멀티 모듈 분리 검토

### 릴리즈 게이트
- 기능 게이트: 해당 Phase MVP 전체 구현 + DoD
- 품질 게이트: 단위 커버리지 70%, 통합 테스트 핵심 플로우 100%, 타입/빌드 에러 0
- 보안 게이트: JWT·API Key 해시·민감키 암호화·Rate Limit 동작 증빙
- 운영 게이트: docker-compose up 만으로 로컬 전체 기동

---

## 15. 리스크 · 완화책

| 리스크 | 영향 | 완화책 |
|--------|------|--------|
| 발신번호 심사 지연 | 초기 사용자 이탈 | Phase 1 수동 심사 + Phase 2 자동화 |
| Send Agent 장애 | 전 서비스 중단 | `SEND_DUAL_LOCK` 기반 Active-Active 이중화 |
| KKO/RCS 스펙 변경 | 검수 실패 | 검수 코드(6000~6071) 모니터링 대시보드 + 알람 |
| 대량 발송 시 DB 쓰기 폭주 | p95 악화 | 채널별 파티셔닝 + 일자별 파티션, HikariCP 튜닝 |
| MCP 남용 (AI 에이전트가 과도 호출) | 비용·스팸 | API Key 기반 Rate Limit + Daily Quota + 발신번호당 TPS |
| 결과 코드 미해석 | 운영 지표 왜곡 | `result_code_dict` 미매칭시 `UNKNOWN` 자동 등록 + 일 1회 미스 알림 |
| 기존 `pm/MASTER_PLAN.md` 와 방향 불일치 | 혼선 | 본 문서를 **유일한 기준선**으로 공지, 기존 문서는 `pm/archive/` 로 이동 |

---

## 16. 후속 작업

1. 본 설계 승인 → `pm/MASTER_PLAN.md` 리비전 또는 `pm/archive/` 이동.
2. WBS·Sprint 계획 재작성 (Phase 1 기준 Sprint 1~2).
3. `MCP_DESIGN.md` 승인 후 MCP Tool 스텁 구현 (PingTool 대체).
4. DB 마이그레이션 스크립트 작성 (`V2__send_tables.sql`, `V3__result_code_dict.sql`).
5. Send Agent 연동 규약 문서 (`AGENT_CONTRACT.md`) 별도 작성.

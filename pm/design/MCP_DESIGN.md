# MCP DESIGN — Wisecan Unified MCP Server

> 본 문서는 Wisecan Unified Messaging Platform 의 **MCP (Model Context Protocol) 서버 개발 설계서**입니다.<br>
> 상위 설계: `pm/design/OVERALL_DESIGN.md`<br>
> 작성일: 2026-04-14

---

## 1. MCP 개요 · 본 서비스에서의 역할

### 1.1 MCP 란?
- Anthropic 이 주도한 **AI 에이전트–도구 간 표준 프로토콜**.
- 한 번 구현하면 Claude Desktop, Cursor, Gemini CLI, VS Code Copilot Chat 등에서 **설정 추가만으로** 사용 가능.
- 제공 개념: **Tool (함수 호출)**, **Resource (읽기 전용 데이터)**, **Prompt (템플릿)**.

### 1.2 본 서비스 포지션
- Wisecan Unified 는 SMS/LMS/MMS/KKO/RCS 를 단일 API로 제공한다.
- 여기에 **MCP 서버를 내장**하여, 사용자가 **자신의 AI 에이전트**로 메시지를 발송·조회·분석하게 한다.
- 경쟁 서비스(솔라피·NHN 클라우드 등) 대비 핵심 차별점.

### 1.3 사용 시나리오
| 시나리오 | MCP Tool | 기대 효과 |
|----------|----------|-----------|
| "오늘 13시에 주문완료 SMS 100건 예약해" | `send_sms` 반복 호출 (requestDate) | 작업 자동화 |
| "어제 실패한 알림톡 원인별 집계" | `report_failure_top` | 운영 대시보드 대체 |
| "01012345678 수신자 최근 발송 내역" | `search_messages` | 고객센터 지원 |
| "발신번호 목록" | `list_senders` (Resource) | 템플릿 작성 보조 |
| "알림톡 템플릿 검수 상태" | `list_kko_templates` | 승인 대기 추적 |

---

## 2. 기술 스택 · 프로토콜

| 항목 | 선택 | 이유 |
|------|------|------|
| 프레임워크 | Spring AI **1.1.2** (`spring-ai-starter-mcp-server-webmvc`) | 현재 `backend/build.gradle` 확정. WebMVC 기반 동기 처리, Streamable HTTP 지원 |
| 전송 방식 | **Streamable HTTP** | 원격 접근(Claude Desktop `url` 설정) 표준. stdio 는 로컬 전용이라 SaaS 부적합 |
| 인증 | Custom HTTP header (`X-API-Key`) via `ServerHttpObservationFilter` 앞단의 `McpAuthInterceptor` | B2C SaaS 기존 API Key 체계 재사용 |
| 직렬화 | Jackson (Spring Boot 기본) | - |
| 스키마 | `@Tool` + `@ToolParam` (Spring AI) | 자동 JSON Schema 생성 |

### 2.1 엔드포인트
| Path | 설명 |
|------|------|
| `/mcp` | MCP Streamable HTTP 메인 엔드포인트 (Spring AI 기본 경로) |
| `/mcp/health` | 단순 health (별도) |

- 사용자 연결 설정 예(Claude Desktop `claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "wisecan": {
      "url": "https://mcp.wisecan.io/mcp",
      "headers": { "X-API-Key": "wc_live_xxxxxxxxxxxx" }
    }
  }
}
```
- `/settings/mcp` 웹 페이지에서 사용자의 API Key 기반 **설정 JSON을 자동 생성·복사** 제공.

---

## 3. 아키텍처

```
 [AI Agent (Claude/Cursor/Gemini)]
          │  HTTPS Streamable
          ▼
 ┌────────────────────────────────────────────────┐
 │  Spring Boot — MCP Server (WebMVC)             │
 │  ┌────────────────────────────────────────┐    │
 │  │ McpAuthInterceptor (X-API-Key 검증)    │    │
 │  │   → ApiKey → MemberPrincipal 주입      │    │
 │  ├────────────────────────────────────────┤    │
 │  │ McpRateLimitInterceptor (Redis token)  │    │
 │  ├────────────────────────────────────────┤    │
 │  │ Spring AI MCP Dispatcher               │    │
 │  │   routes to @Tool methods              │    │
 │  └─────────┬──────────────────────────────┘    │
 │            ▼                                   │
 │  Tool Layer (com.wisecan.unified.mcp.tool.*)   │
 │  ─ MessagingTools / ReportTools / MetaTools    │
 │            ▼                                   │
 │  Service Layer (기존 REST 와 동일 서비스 재사용)│
 │            ▼                                   │
 │  Repository (JPA / MyBatis)                    │
 └────────────────────────────────────────────────┘
```

- **Tool 은 얇은 어댑터**. 비즈니스 로직은 REST 컨트롤러와 동일한 `*Service` 를 호출한다 (DRY).
- 결과는 DTO Record 로 반환 → Spring AI 가 JSON 으로 직렬화.

---

## 4. 패키지 구조

```
backend/src/main/java/com/wisecan/unified/
├── mcp/
│   ├── config/
│   │   └── McpConfig.java                 # ToolCallbackProvider 빈
│   ├── auth/
│   │   ├── McpAuthInterceptor.java        # X-API-Key → MemberPrincipal
│   │   └── McpRateLimitInterceptor.java
│   ├── tool/
│   │   ├── messaging/
│   │   │   ├── SmsTools.java
│   │   │   ├── MmsTools.java              # LMS/MMS
│   │   │   ├── KakaoTools.java            # KAT/KAI + KBT~KBA
│   │   │   └── RcsTools.java              # RSMS/RLMS/RSMMS/RCMMS/RTF
│   │   ├── report/
│   │   │   ├── SearchTools.java
│   │   │   └── ReportTools.java
│   │   ├── meta/
│   │   │   ├── SenderTools.java
│   │   │   ├── KkoMetaTools.java
│   │   │   ├── RcsMetaTools.java
│   │   │   └── RejectTools.java
│   │   └── common/
│   │       └── ToolSupport.java           # 공통 검증·매핑
│   ├── resource/
│   │   └── ResultCodeResource.java        # MCP Resource (결과코드 표)
│   └── dto/
│       ├── McpSendResult.java
│       ├── McpMessageDetail.java
│       └── McpReportRow.java
└── mcp/MCP_README.md
```

- 기존 단일 `com.wisecan.unified.mcp.PingTool` 을 해체하고 위 구조로 이관.
- `com.wisecan.unified.mcp.config.McpConfig` 의 `ToolCallbackProvider` 빈에 **모든 Tool 컴포넌트 주입**.

---

## 5. Tool 카탈로그 (MVP 기준)

> 명명 규칙: `snake_case`, 동사로 시작. 채널이 모호하지 않도록 prefix 포함.

### 5.1 발송 Tool (Phase 1)
| Tool | 설명 | 주요 파라미터 |
|------|------|---------------|
| `send_sms` | SMS 단건 발송 | destaddr, callback, sendMsg, senderId, requestDate?, billCode? |
| `send_lms` | LMS 발송 | + subject |
| `send_mms` | MMS 발송 (첨부) | + fileKeys[] (사전 업로드 ID) |
| `send_group_sms` | SMS 대량 | destaddrs[] (최대 1,000) |

### 5.2 발송 Tool (Phase 2)
| Tool | 설명 |
|------|------|
| `send_alimtalk` | KAT/KAI. 템플릿 코드 + 변수 맵 |
| `send_brand_message` | KBT/KBI/KBW/KBL/KBM/KBC/KBA/KBP 전용 파라미터 |
| `send_rcs` | sub_type 파라미터로 RSMS/RLMS/RSMMS/RCMMS/RTF 라우팅 |

### 5.3 조회 · 통계 Tool
| Tool | 설명 |
|------|------|
| `get_message` | msgId 로 단건 상세 (채널 자동) |
| `search_messages` | 기간/상태/수신번호/결과코드/채널 필터 + 페이지네이션 |
| `get_group_status` | 그룹 발송 진행률 |
| `report_daily` | 날짜 범위, 채널별 발송/성공/실패 |
| `report_failure_top` | 기간 내 결과코드 Top-N + 해석 포함 |
| `report_by_carrier` | `result_net_id` 기준 집계 |

### 5.4 메타 · 관리 Tool
| Tool | 설명 |
|------|------|
| `list_senders` | 내 발신번호 (심사 상태) |
| `list_kko_profiles` | 카카오 발신프로필 |
| `list_kko_templates` | 카카오 템플릿 + 검수 상태 |
| `list_rcs_brands` | RCS 브랜드/대행사 |
| `list_rcs_templates` | msgbase_id 목록 |
| `add_reject` / `remove_reject` / `list_rejects` | 수신거부 관리 |

### 5.5 파일 업로드 Tool (MMS/RCS 이미지)
| Tool | 설명 |
|------|------|
| `upload_file_prepare` | Presigned URL 발급 (S3) — 에이전트는 URL PUT 후 fileKey 로 발송 |
| `register_rcs_image` | 업로드된 파일을 `RCS_IMAGE` 레코드로 등록 |

### 5.6 Tool 총 개수 (MVP): 20개 내외, Phase 2 포함 30개 내외.

---

## 6. Tool 상세 스펙 (대표 예시)

### 6.1 `send_sms`

```java
@Component
@RequiredArgsConstructor
public class SmsTools {

    private final MessageDispatchService dispatchService;

    @Tool(description = """
        SMS 단건 발송. 수신번호/발신번호/본문을 받아 SEND_SMS_TRAN 에 접수한다.
        - sendMsg 는 90바이트(EUC-KR) 초과 시 오류. 길면 send_lms 사용 권장.
        - requestDate 가 미래이면 예약 발송.
        """)
    public McpSendResult sendSms(
        @ToolParam(description = "수신번호 01012345678 형식") String destaddr,
        @ToolParam(description = "발신번호 sender id (list_senders 로 조회한 id)") Long senderId,
        @ToolParam(description = "본문 (EUC-KR 기준 90바이트 이내)") String sendMsg,
        @ToolParam(required = false, description = "예약 일시 ISO-8601") String requestDate,
        @ToolParam(required = false, description = "과금 태그") String billCode
    ) {
        var req = SmsSendRequest.of(destaddr, senderId, sendMsg, requestDate, billCode);
        return McpSendResult.from(dispatchService.dispatchSms(currentMember(), req));
    }
}
```

#### 응답 DTO
```java
public record McpSendResult(
    Long msgId,
    String channel,            // SMS/LMS/MMS/KKO/RCS
    String subType,            // SMS/LMS/MMS/KAT/...
    int messageState,          // 0..4
    String messageStateLabel,  // "대기"
    String requestDate,
    Long groupId,
    String clientTraceId
) {}
```

### 6.2 `send_alimtalk` (Phase 2)
- 입력: `profileId`(kko_sender_key 매핑), `templateCode`, `variables: Map<String,String>`, `destaddr`, `fallback: {type, subject, msg}?`
- 백엔드는 템플릿 본문 + 변수로 `send_msg` 조립 + 템플릿 검증(에러코드 6029/6030/6067 사전 방지).
- `attach_data` JSON 은 백엔드가 템플릿 버튼/하이라이트/아이템 정보로 자동 생성 후 저장.

### 6.3 `send_rcs` (Phase 2)
- 입력: `subType` (RSMS/RLMS/RSMMS/RCMMS/RTF), `templateId`(msgbase_id), `brandId/brandKey`, `adFlag`(→ msg_header), `footer`(광고성 필수), `content`(채널별 JSON), `buttons`, `expiryOption`, `fallback?`.
- 백엔드가 `rcs_object`, `btn_object` 생성 → `SEND_RCS_TRAN` INSERT.

### 6.4 `search_messages`
- 입력: `channel?`, `from`, `to`, `destaddr?`, `resultCodes?: string[]`, `state?`, `page`, `size`.
- 출력: `{ total, items: McpMessageDetail[] }`
- 내부: 채널 파라미터로 대상 LOG 테이블 결정. 미지정 시 `v_send_all` View 조회.

### 6.5 `report_failure_top`
- 입력: `from`, `to`, `channel?`, `top` (기본 10).
- 출력: `[{ resultCode, count, severity, retriable, description }]`
- 결과코드는 `result_code_dict` 조인으로 의미 포함 → AI 에이전트가 자연어로 요약 가능.

### 6.6 Resource `result_codes`
- URI: `mcp://wisecan/resources/result-codes`
- 내용: `result_code_dict` 전체 JSON. 에이전트 컨텍스트에 로드하여 해석 보조.
- 갱신: 1일 1회 캐시.

---

## 7. 인증 · 권한

### 7.1 인증 흐름
```
AI Agent ──(X-API-Key: wc_live_xxx)──▶ /mcp
          ▼
 McpAuthInterceptor
   1. Key prefix 추출 → ApiKeyRepository 조회
   2. SHA-256(Key) 비교
   3. status=ACTIVE 확인
   4. MemberPrincipal ThreadLocal (RequestContextHolder) 저장
          ▼
 Tool 실행 시 ToolSupport.currentMember() 로 소유자 확인
          ▼
 모든 DB 쓰기는 owner 필드(member_no, user_id) 강제 주입
```

- **실패 시 응답**: JSON-RPC error `-32001` + `reason` (`invalid_api_key` / `revoked` / `rate_limited`).

### 7.2 권한 범위 (Scope)
API Key 생성 시 scope 지정:
- `messaging:send` — 발송 Tool
- `messaging:read` — 조회·통계 Tool
- `meta:read` — 발신번호·템플릿 조회
- `meta:write` — 템플릿 등록, 수신거부 관리
- `admin` — 전체

### 7.3 Rate Limit & Quota
- Redis token bucket:
  - 기본: 10 TPS / 분당 120 / 일 10,000 호출 (Tier 별 상향 조정).
  - `send_*` 계열은 추가로 발신번호당 TPS 제한(통신사 정책 준수).
- 초과 시 JSON-RPC error `-32002` + Retry-After.

---

## 8. 응답 · 결과코드 표준화

### 8.1 발송 Tool 공통 규약
- **즉시 응답**에는 `message_state=0(대기)` 까지만 포함. 전송 성공/실패는 `get_message` 로 비동기 조회.
- 에이전트에게 혼동을 주지 않도록 `messageStateLabel`, `nextAction: "call get_message after ~10s"` 힌트 동봉.

### 8.2 조회 Tool 공통 규약
- `resultCode` 가 있는 경우 항상 다음 필드를 동반:
```json
{
  "resultCode": "3002",
  "resultCodeName": "E_TIMEOUT",
  "severity": "CARRIER_ERROR",
  "retriable": true,
  "description": "전송 시간 초과"
}
```

### 8.3 MCP 자체 오류 코드 (JSON-RPC)
| code | 의미 |
|------|------|
| -32001 | 인증 실패 (`invalid_api_key`, `revoked`) |
| -32002 | Rate limit / quota exceeded |
| -32010 | 입력 검증 실패 (상세는 `data.fields[]`) |
| -32020 | 소유자 권한 없음 (다른 사용자의 msgId 등) |
| -32030 | 도메인 규칙 위반 (예: RCS `msg_header=1` 인데 footer 누락 → 사전 차단) |
| -32040 | 업스트림 장애 (DB/Agent) |

---

## 9. 로깅 · 관측

| 항목 | 방식 |
|------|------|
| 호출 이력 | `api_usage` 테이블에 `tool_name`, `status`, `response_time_ms`, `error_message` 저장 (기존 스키마 재사용) |
| 트레이싱 | Micrometer Tracing / OTel. `trace_id` 를 `SEND_*_TRAN.etc_char_1` 에 저장 → 메시지-호출 매핑 |
| 메트릭 | `mcp.tool.invocations{tool,status}`, `mcp.tool.duration{tool}` |
| 감사 | 발송 Tool 호출 시 요청 바디 해시 + 수신번호 마스킹 후 저장 |

---

## 10. 입력 검증 · 도메인 규칙 (Tool 단 사전 차단)

| 규칙 | 위반 시 |
|------|---------|
| `destaddr` E.164/국내 형식 | -32010 `invalid_destaddr` |
| `senderId` 소유자 일치 & ACTIVE | -32020 `sender_not_owned` |
| SMS 본문 90바이트 초과 | -32010 `sms_too_long` (→ LMS 권장) |
| MMS `file_count` 과 `fileKeys` 수 일치 | -32010 `file_count_mismatch` |
| RCS `msg_header=1` → footer 필수 | -32030 `rcs_footer_required` |
| KKO 템플릿 변수 미지정 | -32030 `kko_template_variable_missing` |
| `requestDate` 과거 or 90일 초과 | -32010 `request_date_invalid` |
| `SEND_REJECT` 에 포함된 destaddr | 사전 차단 + 결과코드 `1201` 풍의 내부 코드 반환 |

---

## 11. 테스트 전략

| 레벨 | 도구 | 범위 |
|------|------|------|
| 단위 | JUnit5 + Mockito | ToolSupport, 입력 검증, DTO 변환 |
| 슬라이스 | `@WebMvcTest` | McpAuthInterceptor |
| 통합 | `@SpringBootTest` + TestContainers(MySQL, Redis) | 실제 INSERT → `SEND_SMS_TRAN` |
| MCP E2E | MCP Inspector / `@modelcontextprotocol/inspector` CLI | stdio/HTTP 양방향 |
| 시나리오 | AI Agent 수동 (Claude Desktop) | "SMS 발송 후 결과 조회" 등 5 케이스 |

- CI 에서 `./gradlew test` + MCP Inspector 헤드리스 호출 스크립트를 포함.

---

## 12. 배포 · 운영

| 항목 | 내용 |
|------|------|
| 빌드 | 기존 `backend/build.gradle` 의 `spring-ai-starter-mcp-server-webmvc` 유지 |
| 설정 | `application.yml` 에 `spring.ai.mcp.server.*` 기본값 확인 (path=`/mcp`) |
| 공개 도메인 | `mcp.wisecan.io` (ALB/Nginx → backend) |
| TLS | Let's Encrypt |
| 문서 | `/settings/mcp` 에서 사용자별 설정 JSON 생성, 공개 문서는 `/docs/mcp` (Phase 2) |
| 버전 | Tool 스키마 Breaking change 시 `_v2` 신규 Tool 추가, 구버전 deprecated 표시 후 6개월 유지 |

---

## 13. 단계별 릴리즈

### MCP v1 (Phase 1 종료 시점)
- 발송: `send_sms`, `send_lms`, `send_mms`, `send_group_sms`
- 조회: `get_message`, `search_messages`, `report_daily`, `report_failure_top`
- 메타: `list_senders`, `add_reject`, `remove_reject`, `list_rejects`
- Resource: `result_codes`
- 인증/Rate Limit 완료

### MCP v2 (Phase 2 종료 시점)
- 카카오: `send_alimtalk`, `send_brand_message`, `list_kko_profiles`, `list_kko_templates`
- RCS: `send_rcs`, `list_rcs_brands`, `list_rcs_templates`, `register_rcs_image`, `upload_file_prepare`
- 리포트: `report_by_carrier`, `get_group_status`
- Prompt 템플릿 (예약 발송, 트러블슈팅)

### MCP v3 (Phase 3)
- Webhook 대체 MCP Event Stream (notification 메시지) 검토
- Billing/Quota Tool

---

## 14. 설계 결정 로그

| # | 결정 | 이유 |
|---|------|------|
| D1 | Streamable HTTP 채택 (stdio 제외) | SaaS 원격 접속이 기본. stdio 는 로컬 개발 한정 |
| D2 | 기존 `*Service` 재사용 (Tool 은 얇게) | REST/MCP 동일 비즈니스 규칙 보장 |
| D3 | 발송 Tool 은 비동기 모델 유지 | Agent 아키텍처 특성상 즉시 결과 확정 불가 |
| D4 | 채널별 Tool 분리 | Spring AI 의 스키마 자동 생성은 파라미터 수가 많으면 AI가 혼동. 채널 분리로 프롬프트 효율↑ |
| D5 | `result_code_dict` DB 테이블화 + Resource 노출 | AI 가 결과코드를 "몰라서" 틀린 설명을 하는 문제 방지 |
| D6 | API Key 재사용(웹/REST/MCP 공통) | 사용자 인지 부담 최소화. scope 로 권한 분리 |
| D7 | `etc_char_1` 에 trace_id 저장 | 첨부 테이블 스펙의 예비 컬럼을 변경 없이 활용 |

---

## 15. 오픈 이슈

1. **MCP 표준의 Streaming tool result** 지원 범위 — Spring AI 1.1.2 가 부분 지원. 장시간 리포트 Tool 은 청크 응답 필요 시 추후 검토.
2. **Binary 응답** (MMS 이미지 프리뷰) — 현재 MCP 표준은 base64 권고. 대용량은 Presigned URL 로 우회.
3. **Per-sender TPS 제어** — 통신사별 정책 차이 반영 필요.
4. **Tool 이름 네임스페이싱** — 타 MCP 서버와 충돌 방지 위해 `wisecan_send_sms` 등 prefix 옵션 검토.

---

## 16. 수용 기준 (DoD) — MCP v1

- [ ] `/mcp` 엔드포인트가 Claude Desktop 에서 연결 성공 (X-API-Key 인증)
- [ ] v1 Tool 13개 정상 호출 (단위·통합 테스트 통과)
- [ ] 모든 발송 Tool 이 `SEND_*_TRAN` 에 표준 스펙대로 INSERT
- [ ] `search_messages` 가 채널/기간/결과코드 필터 동작
- [ ] 결과코드 응답에 severity/retriable/description 포함
- [ ] Rate Limit/Quota 위반 시 -32002 + Retry-After
- [ ] MCP Inspector 로 자동 smoke test 가 CI 에서 성공
- [ ] `/settings/mcp` 웹 페이지에서 설정 JSON 1클릭 복사 가능
- [ ] `MCP_DESIGN.md` 의 모든 Tool 스펙이 코드와 일치 (매 릴리즈 점검)

---

## 17. 후속 작업

1. 기존 `PingTool` 삭제 및 `com.wisecan.unified.mcp.*` 패키지 재구성.
2. `McpAuthInterceptor`, `McpRateLimitInterceptor` 구현 (WebMVC `HandlerInterceptor`).
3. Tool 스텁 13개 생성 (Phase 1) — DTO/Service 시그니처만 우선 맞춤.
4. `result_code_dict` 시드 데이터 (첨부 7.1~7.3 전부) SQL 생성.
5. MCP Inspector 기반 CI smoke test 스크립트.
6. `/settings/mcp` 웹 페이지 (frontend feature `mcp-connect`).

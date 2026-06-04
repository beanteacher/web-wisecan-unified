# 옵저버빌리티 — WiseCan 통합 메시징 서비스

> 작성일 2026-06-04

---

## 1. 범위와 정본 관계

### 1.1. 본 문서가 정본인 항목

- Prometheus 메트릭 카탈로그 (§2)
- Alertmanager 룰·라우팅 (§3)
- Grafana 대시보드 구성 (§4)
- 운영자 콘솔 Grafana 임베드 아키텍처 (§5)
- 운영자 ↔ Grafana 권한 매핑 (§6)

### 1.2. 본 문서가 정본이 아닌 항목 — 인용만 한다

| 항목 | 정본 | 본 문서 사용처 |
|---|---|---|
| KPI 임계값 (적재 99.9% / 가용성 99.5% / 전환율 15% / CLI 위임 50%) | `01_PRD §K3`·`§K2` | §2 각 메트릭의 임계 비고에서 인용 |
| 운영 게이트 SLA 임계값 (P95 1s, 자동충전 실패율 1%, 24h SLA 등) | `04_PROJECT_PLAN §8.3` | §3 알림 룰의 임계값 인용 |
| 회원 알림 디바운스 Redis 키 (`alert:fired:*`, `alert:keyexpiry:*`) | `05_DATA_MODEL §결제·키 Redis 키` | §2.5 참고 표시만 |
| `BALANCE_THRESHOLD_ALERT` 테이블 | `05_DATA_MODEL §7` | 본 문서 언급 없음 (회원 알림이라 Prometheus 영역 밖) |
| 체험 모드 가상 발송·결제 차단 정책 | `02_FEATURE_SPEC §2.2`·`§2.3` | §2.5 카운터 발화 시점 인용 |

### 1.3. 범위 제외 — 본 서비스 책임이 아닌 영역

- **발송 송출 이후 전 구간** — 적재 이후 송출·딜리버리·중계사 응답 지연·실제 도달률은 외부 발송 시스템(자체 관제 보유)이 책임진다 (`02_FEATURE_SPEC §14`, `RQ-ARCH-002·003`). 본 서비스의 Prometheus는 **테이블 적재까지만** 본다.
- **인프라 메트릭** — Oracle·Redis·JVM·node_exporter·자체 IDC 전원/네트워크 메트릭은 인프라팀 별도 스택. 본 문서는 애플리케이션 도메인 메트릭에만 한정.

---

## 2. Prometheus 메트릭 카탈로그

라벨 표기 컨벤션: `result` ∈ `success|fail|reject`, `channel` ∈ `sms|lms|mms|kakao|rcs`, `key_type` ∈ `test|prod`.

### 2.1. 발송 적재

| 메트릭 | 타입 | 라벨 | 용도 |
|---|---|---|---|
| `send_api_total` | Counter | `channel`,`key_type`,`result` | 가용성 분모 |
| `send_api_5xx_total` | Counter | `channel`,`endpoint` | 가용성 분자 — **목표 `01_PRD §K3` 가용성 99.5%** |
| `send_api_duration_seconds` | Histogram | `channel`,`endpoint` | P95 ≤ 1,000ms (`04 §8.3`) |
| `send_table_insert_total` | Counter | `channel`,`result` | **적재 성공률 99.9% (`01_PRD §K3`)** |
| `send_validation_reject_total` | Counter | `reason` | `CALLER_NOT_REGISTERED` / `SPAM_KEYWORD_DETECTED` / `AD_DISCLOSURE_MISSING` / `TEXT_TOO_LONG` / `INSUFFICIENT_BALANCE` |
| `send_batch_rows_total` | Counter | `result` | 일괄 CSV 100k 행 처리 결과 |
| `encoding_conversion_loss_total` | Counter | — | UTF-8 → EUC-KR 무손실 검증, **>0 즉시 P0** |
| `scheduled_send_enqueue_total` | Counter | `result` | 예약 적재 결과 |
| `insufficient_balance_branch_total` | Counter | `path` | `autocharge`/`postpaid`/`partial`/`cancel` 분기 분포 (`02 §11`) |

> 적재 이후 송출 지연·딜리버리 성공률·중계사 응답 지연은 본 카탈로그에서 제외 (§1.3).

### 2.2. 결제·캐시

| 메트릭 | 타입 | 라벨 | 용도 |
|---|---|---|---|
| `payment_attempt_total` | Counter | `method`,`result` | 7수단(`02 §10.1`) |
| `pg_request_duration_seconds` | Histogram | `method` | 우리 → PG 호출 구간 |
| `autocharge_attempt_total` | Counter | `result` | **실패율 ≤ 1% (`04 §8.3`)** |
| `autocharge_lock_blocked_total` | Counter | — | Redlock 중복 실행 방지 효과 |
| `refund_request_total` | Counter | `status` | 신청/승인/반려 |
| `cash_expiry_pending_amount` | Gauge | `days_left` | 30/7/1일 임박 잔액 합계 |
| `invoice_overdue_total` | Gauge | — | 후불 연체 건수 |

### 2.3. 외부 연동 (우리가 호출하는 부분만)

| 메트릭 | 타입 | 라벨 | 용도 |
|---|---|---|---|
| `kisa_register_request_total` | Counter | `result` | 자동연계 성공률 ≥ 99% (`04 §2` M1 게이트) |
| `kisa_request_duration_seconds` | Histogram | — | KISA 응답 지연 |
| `kakao_template_register_total` | Counter | `vendor`,`result` | LG CNS / KT / 인포뱅크 — **운영자 전용, 회원 비노출 (`02 §12.4`)** |
| `rcs_brand_register_total` | Counter | `result` | RCS 브랜드 등록 |
| `external_circuit_breaker_state` | Gauge | `system` | `open=2`/`half=1`/`closed=0` |

### 2.4. 인증·키·보안

| 메트릭 | 타입 | 라벨 | 용도 |
|---|---|---|---|
| `login_attempt_total` | Counter | `result`,`mfa` | 5회 실패 잠금 추적 |
| `account_lockout_total` | Counter | — | 잠금 발화 |
| `api_key_auth_total` | Counter | `result`,`scope` | `SCOPE_NOT_GRANTED` 분리 |
| `api_key_cache_invalidation_latency_seconds` | Histogram | — | **1초 내 다중 노드 무효화 검증 (`02 §5.4`)** |
| `mcp_blocked_action_total` | Counter | `action` | MCP 비노출 정책 차단 시도 (보안 게이트 `04 §8.4`) |
| `ratelimit_block_total` | Counter | `key_type`,`limit_type` | 분당 RL / 일일 한도 |
| `abuse_auto_block_total` | Counter | `reason` | 이상 패턴 자동 차단 |

### 2.5. KPI 카운터 (Grafana 전용, 알림 없음)

> `05_DATA_MODEL §8.2` 의 트라이얼 메트릭 정의는 본 절을 정본으로 한다 (해당 문서는 본 절을 인용).

| 메트릭 | 라벨 | 발화 시점 |
|---|---|---|
| `trial_session_started_total` | `ip_class`,`referrer` | `/try` 진입 (`02 §2.1`) |
| `trial_session_converted_total` | — | 가입 완료 + 토큰 보유 (`02 §2.3`) |
| `trial_session_expired_total` | — | Redis `trial:*` expire 이벤트 구독 |
| `trial_abuse_blocked_total` | `reason` | 핑거프린트 임계 초과 차단 (`02 §2.1` 사전조건) |
| `cli_invocation_total` | `command` | CLI 명령 진입 |
| `mcp_invocation_total` | `tool` | MCP 도구 호출 |

**산출 지표**
- 체험 → 가입 전환율 = `trial_session_converted_total / trial_session_started_total` → **≥ 15% (`01_PRD §K3 보조`)**
- AI Agent CLI 위임 비율 = `cli_invocation_total / (cli_invocation_total + mcp_invocation_total)` → **≥ 50% (`01_PRD §K2`)**

> 회원 알림(잔액 임계·키 만료 임박)은 본 카탈로그가 아니라 도메인 이벤트 + Redis 디바운스로 처리 (`05_DATA_MODEL §결제·키 Redis 키`).

### 2.6. 운영자 워크로드 SLA

| 메트릭 | 타입 | 라벨 | 용도 |
|---|---|---|---|
| `admin_review_queue_size` | Gauge | `type` | `business`/`callback`/`key` 큐 길이 |
| `admin_review_age_seconds` | Histogram | `type` | 대기 시간 분포 |
| `cs_ticket_open_age_hours` | Histogram | — | 1:1 문의 24h 처리 SLA (`04 §8.3`) |

---

## 3. Alertmanager 룰·라우팅

### 3.1. 알림 룰 (15종)

임계값은 모두 `04 §8.3` 운영 게이트 또는 `01_PRD §K3` 가 정본. 본 표는 메트릭 표현식만 정의한다.

| 알림 | 심각도 | 표현식 (요약) | 지속 | 정본 임계 |
|---|---|---|---|---|
| `SendApiAvailabilityBelow995` | P1 | `1 - rate(send_api_5xx_total[5m]) / rate(send_api_total[5m]) < 0.995` | 10m | `04 §8.3` |
| `SendTableInsertSuccessBelow999` | **P0** | `rate(...result="success"[5m]) / rate(...[5m]) < 0.999` | 5m | `01_PRD §K3` |
| `SendApiP95Over1s` | P1 | `histogram_quantile(0.95, ...) > 1` | 10m | `04 §8.3` |
| `EncodingLossDetected` | **P0** | `increase(encoding_conversion_loss_total[5m]) > 0` | 즉시 | — (불변) |
| `InsufficientBalanceCancelSpike` | P2 | `cancel` 분기 비율 평소 대비 3σ 초과 | 15m | — (선행 지표) |
| `AutochargeFailureRateOver1Percent` | P1 | 실패율 > 0.01 | 10m | `04 §8.3` |
| `PgRequestErrorSpike` | P1 | PG 5xx + 타임아웃 폭증 | 10m | — |
| `RefundQueueBacklog` | P2 | 대기 건수 > 임계 | 30m | CS SLA |
| `KisaRegisterSuccessBelow99Percent` | P1 | 성공률 < 0.99 | 15m | `04 §2` M1 게이트 |
| `RelayVendorErrorSpike` | P1 | `vendor` 라벨별 격리 | 10m | — |
| `CircuitBreakerOpen` | P1 | `external_circuit_breaker_state == 2` | 즉시 | — |
| `LoginBruteForceSpike` | P1 | 동일 IP/계정 잠금 폭증 | 5m | — |
| `ApiKeyCacheInvalidationOver1s` | **P0** | P95 > 1s | 10m | `02 §5.4` |
| `McpBlockedActionAttempted` | **P0** | `increase(mcp_blocked_action_total[1m]) > 0` | 즉시 | `04 §8.4` |
| `AbnormalSendBurst` | P1 | 키별·회원별 임계 초과 | 5m | — |
| `BusinessReviewSla24hBreach` | P2 | 24h 초과 건 발생 | 즉시 | `04 §8.3` |
| `CsTicket24hBreach` | P2 | 24h 초과 건 발생 | 즉시 | `04 §8.3` |

### 3.2. 라우팅 (Alertmanager `route`)

| 심각도 | 채널 | 대상 |
|---|---|---|
| **P0** | PagerDuty(전화) + Slack `#alerts` | BE 온콜 + PM |
| **P1** | Slack `#alerts` + 이메일 | BE 온콜 |
| **P2** | Slack `#dev` | 담당 에이전트 |
| **회원 알림** (잔액 임계·만료 임박) | 인앱 + 이메일 | 회원 직접 — 본 라우팅 대상 외, 도메인 이벤트로 처리 (`05_DATA_MODEL §결제·키`) |

---

## 4. Grafana 대시보드 구성

도메인별로 1대시보드 1주제 원칙. 모든 패널은 §5 임베드에서 `d-solo` 단위로 운영자 콘솔에 박힌다.

| 대시보드 UID | 제목 | 핵심 패널 | 권한 |
|---|---|---|---|
| `send-pipeline` | 발송 적재 파이프라인 | 가용성·P95·적재 성공률·검증 거부 사유 분포·잔액 분기 분포 | `ADMIN`+ |
| `payment` | 결제·캐시 | 자동충전 실패율·PG 응답·환불 백로그·만료 임박 잔액 | `ADMIN`+ |
| `external` | 외부 연동 | KISA 성공률·중계사별 오류·서킷 상태 | `ADMIN`+ |
| `security` | 인증·보안 | 로그인 실패·키 무효화 지연·MCP 차단 시도·이상 패턴 | `SUPER_ADMIN` 전용 |
| `kpi` | KPI (K2·K3) | 적재 성공률·가용성·전환율·CLI 위임 비율 | `ADMIN`+ |
| `admin-queue` | 운영자 SLA | 심사 큐 길이·대기 시간·CS 24h 위반 | `ADMIN`+ |
| `routing` | 라우팅 (회원 비노출) | 중계사별 매핑·라우팅 메타 분포 | `SUPER_ADMIN` 전용 (`02 §12.4`) |

---

## 5. 운영자 콘솔 Grafana 임베드

### 5.1. 통합 방식 비교

| 방식 | 장점 | 단점 | 채택 |
|---|---|---|---|
| A. Anonymous + iframe | 구현 1시간 | URL 노출 시 무방비 | ✕ |
| B. Auth Proxy (헤더 인증) | 운영자 SSO 재사용, 로그아웃 = 차단 | 리버스 프록시 필요 | **◎ 인터랙티브 임베드 기본** |
| C. Service Account + 백엔드 PNG 렌더 | 프론트가 Grafana를 모름 → 최강 보안 | 인터랙션 불가 | **△ 월간 리포트·PDF 전용** |
| D. JWT/SAML SSO | 권한 1:1 매핑 깔끔 | **Grafana Enterprise 유료** | ✕ (OSS 전제) |

채택: **B + C 조합.** 인터랙티브 대시보드는 B, 정적 리포트(PDF 다운로드)는 C.

### 5.2. 권장 아키텍처 (Auth Proxy)

```
운영자 브라우저
   │ (운영자 로그인 + 2FA + 신뢰 IP 통과 세션 쿠키 — 04 §8.4)
   ▼
[Next.js admin.wisecan.com /admin/observability]
   │   iframe src="/grafana/d-solo/{uid}?panelId=N&kiosk"
   ▼
[Spring Gateway]
   │  ① 세션 검증 (ADMIN / SUPER_ADMIN)
   │  ② 신뢰 IP 화이트리스트 확인
   │  ③ X-WEBAUTH-USER / X-WEBAUTH-ROLE 헤더 주입
   ▼
[Grafana — auth.proxy 모드, 외부 직접 접근 차단]
```

**핵심 원칙** — Grafana 포트는 사설망에서만 열고, 외부 노출은 게이트웨이가 단일 진입점. Grafana 자체 로그인 폼은 비활성. 운영자 세션이 끊기면 iframe 응답도 즉시 401.

### 5.3. Grafana 설정 (`grafana.ini`)

```ini
[server]
root_url = https://admin.wisecan.com/grafana/
serve_from_sub_path = true

[security]
allow_embedding = true
cookie_samesite = none
cookie_secure = true
x_frame_options =                 ; Spring 게이트웨이가 CSP frame-ancestors 로 통제

[auth]
disable_login_form = true
disable_signout_menu = true

[auth.proxy]
enabled = true
header_name = X-WEBAUTH-USER
header_property = username
auto_sign_up = true
sync_ttl = 60
headers = Role:X-WEBAUTH-ROLE
enable_login_token = false

[auth.anonymous]
enabled = false
```

### 5.4. Spring Gateway 필터 (요약)

```java
@Component
@RequiredArgsConstructor
public class GrafanaProxyFilter implements GlobalFilter, Ordered {

    private final AdminSessionService sessionService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        if (!req.getPath().value().startsWith("/grafana/")) return chain.filter(exchange);

        AdminPrincipal admin = sessionService.resolve(req)
                .orElseThrow(() -> new WiseException(ErrorCode.UNAUTHORIZED));

        if (!admin.trustedIp(req.getRemoteAddress())) {
            throw new WiseException(ErrorCode.UNTRUSTED_IP);
        }

        String role = switch (admin.role()) {
            case SUPER_ADMIN -> "Admin";
            case ADMIN       -> "Editor";
            default          -> "Viewer";
        };

        ServerHttpRequest mutated = req.mutate()
                .header("X-WEBAUTH-USER", admin.loginId())
                .header("X-WEBAUTH-ROLE", role)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override public int getOrder() { return -1; }
}
```

라우팅 (`application.yml`):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: grafana
          uri: http://grafana-internal:3000
          predicates: [Path=/grafana/**]
          filters:
            - RewritePath=/grafana/(?<segment>.*), /$\{segment}
```

### 5.5. Next.js admin 페이지 (`/admin/observability` = `PG-AD-017`)

```tsx
// app/(admin)/observability/page.tsx
import { GrafanaPanel } from '@/widgets/observability';

export default function ObservabilityPage() {
  return (
    <div className="grid grid-cols-2 gap-4">
      <GrafanaPanel uid="send-pipeline" panelId={2} title="적재 성공률" />
      <GrafanaPanel uid="send-pipeline" panelId={5} title="API P95" />
      <GrafanaPanel uid="payment"       panelId={3} title="자동충전 실패율" />
      <GrafanaPanel uid="admin-queue"   panelId={1} title="심사 큐 SLA" />
    </div>
  );
}
```

```tsx
// widgets/observability/ui/GrafanaPanel.tsx
'use client';

interface Props { uid: string; panelId: number; title: string; }

export function GrafanaPanel({ uid, panelId, title }: Props) {
  const src = `/grafana/d-solo/${uid}?panelId=${panelId}&theme=dark&kiosk`;
  return (
    <section className="rounded-lg border border-slate-700 bg-slate-900">
      <h3 className="px-4 py-2 text-sm text-slate-300">{title}</h3>
      <iframe
        src={src}
        title={title}
        className="h-72 w-full border-0"
        sandbox="allow-same-origin allow-scripts"
      />
    </section>
  );
}
```

**URL 패턴**
- 전체 대시보드 임베드: `/grafana/d/{uid}/{slug}?kiosk`
- 단일 패널 임베드: `/grafana/d-solo/{uid}/{slug}?panelId=N` ← **기본**

### 5.6. CSP / 보안 헤더

```http
Content-Security-Policy:
  default-src 'self';
  frame-src 'self';
  frame-ancestors 'self';
  connect-src 'self';
  img-src 'self' data:;
```

Grafana 가 동일 도메인 하위 경로(`/grafana/...`) 이므로 `frame-src 'self'` 만으로 충분. CORS 이슈 없음.

---

## 6. 권한 매핑 / 감사 로그

### 6.1. 운영자 권한 → Grafana 역할

| 운영자 권한 | Grafana 역할 | 가능한 동작 |
|---|---|---|
| `SUPER_ADMIN` | Admin | 대시보드 생성/수정, 데이터소스 관리, 라우팅·보안 대시보드 접근 |
| `ADMIN` | Editor | 대시보드 필터·기간 변경, 일반 대시보드 접근 |
| 그 외 | (접근 불가) | 게이트웨이에서 403 |

`sync_ttl = 60` 이므로 권한 회수는 1분 내 반영. 운영자 강제 비활성화(`02 §12.3`) 시 Grafana 세션도 자동 끊김.

### 6.2. 감사 로그

- Grafana 자체 로그(`/var/log/grafana/grafana.log`) + Spring 게이트웨이 access log 양쪽 보관.
- 보존 기간 **5년** (`04 §8.4` 감사 로그 append-only 정책 준수).
- `SUPER_ADMIN` 전용 대시보드(`security`, `routing`) 접근은 별도 카운터 `grafana_sensitive_dashboard_access_total{uid,role}` 로 트래킹.

---

## 7. 한계 / 로드맵

- OSS Grafana 의 role 동기화가 헤더 기반이라 즉시 권한 회수에 최대 `sync_ttl(60s)` 지연. 보안 강화 필요 시 Enterprise JWT auth 로 이전 검토.
- 폴더 단위 권한은 Grafana 안에서 별도 관리해야 하며, 운영자 권한과 자동 매핑되지 않음 — `SUPER_ADMIN` 전용 대시보드(`security`, `routing`)는 Grafana 폴더 권한을 수동 설정한다.
- 발송 송출 이후 메트릭은 외부 관제 책임이라 본 서비스에는 들어오지 않음. 만약 SLA 클레임 추적이 필요해지면 외부 관제로부터 일배치 CSV 적재 → 별도 Grafana 데이터소스를 신설하는 것이 1차 안.
- 인프라(Oracle/Redis/JVM/IDC) 메트릭은 인프라팀 별도 스택. 본 서비스 도메인 메트릭과 합치려면 Grafana mixed datasource 로 묶는 것이 2차 안.

---

## 8. 산출물 매핑

| 산출물 | 경로 | 비고 |
|---|---|---|
| Prometheus scrape 설정 | `backend/src/main/resources/prometheus/` | W-003 산출물 |
| Alertmanager 룰 | `backend/src/main/resources/alerts/` | §3.1 표 정본 |
| Grafana 대시보드 JSON | `infra/grafana/dashboards/` | §4 표 1:1 매핑 |
| Spring `GrafanaProxyFilter` | `backend/src/main/java/.../global/gateway/` | §5.4 |
| Next.js observability 페이지 | `frontend/src/app/(admin)/observability/`, `frontend/src/widgets/observability/` | `PG-AD-017` |

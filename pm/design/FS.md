# FS — WiseCan 통합 메시징 서비스 기능 명세서 (Functional Requirements Specification)

> 작성일: 2026-04-30 (최종 수정 2026-04-30, v2)
> 상태: 1차 초안

## 본 문서의 위치 — 표준 분류

본 FS는 **IEEE 830 / ISO/IEC/IEEE 29148** 표준 정의에 따른 **Functional Requirements Specification — 기능 요구사항만** 다루는 명세서다.

| 약어 | 정식 명칭 | 본 저장소 파일 | 다루는 영역 |
|---|---|---|---|
| SRS | Software Requirements Specification | `pm/design/SRS.md` | **기능 + 비기능 통합** (상위 문서) |
| **FRS / FS** | Functional Requirements Specification | `pm/design/FS.md` (본 문서) | **기능 요구사항만** (페이지·API·CLI·상태 머신·도메인 모델 등 기능 설계 포함) |
| NFR | Non-Functional Requirements | `pm/design/NFR.md` | 비기능 요구사항만 (성능·보안·가용성·확장성·법규) |
| PRD | Product Requirements Document | `pm/plan/PRD.md` | 비전·타깃·KPI·MVP 범위 (비즈니스) |

표준 관계: `SRS ⊇ FRS + NFR + 제약·외부 인터페이스 등`. 본 저장소는 별도 DS/TS 문서를 두지 않고 본 FS가 **기능 설계 영역(페이지·API·상태 머신 등)까지 흡수**하는 한국 실무 관행을 따른다.

## 본 FS의 구성

| 절 | 영역 | 비고 |
|---|---|---|
| §1 | 개요 / SRS·NFR·PRD과의 관계 | 본 절 |
| §2 | 페이지 인벤토리 | 기능 단언의 UI 매핑 |
| §3 | 사용자 플로우 | 기능 단언의 시나리오 |
| §4 | 도메인 모델 | 기능 단언의 데이터 |
| §5 | API Key 스코프 카탈로그 | RQ-KEY-101 구체화 |
| §6 | 진입점별 인터페이스 (SDK·MCP·CLI) | RQ-SDK·RQ-MCP·RQ-CLI 구체화 |
| §7 | 발송 파이프라인 | RQ-SEND 구체화 |
| §8 | 상태 머신 | 기능 단언의 상태 전이 |
| §9 | 권한 매트릭스 | 역할 × 액션 |
| §10 | 에러 코드 | 거부 사유 표준화 |
| §11 | 입력 검증 규칙 | 기능 단언의 입력 |
| §12 | 비기능 요구사항 — 본 FS 범위 외 (NFR.md 참조) | 비기능은 NFR.md 권위 출처 |
| §13 | A/B 의사결정별 구현 영향 | 기능 측 구현 비교 |
| §14 | 변경 이력 | 본 FS |

## 본 FS가 다루지 않는 영역

- **비기능 요구사항** (성능·보안·가용성·확장성·운영·법규) → `pm/design/NFR.md`
- **비전·KPI·MVP 범위** → `pm/plan/PRD.md`
- **기능 단언(`RQ-*`) 카탈로그 자체의 정의** → `pm/design/SRS.md` (본 FS는 SRS의 RQ-* 단언을 페이지·API·상태 머신으로 구체화하는 역할)

---

## 1. 개요

### 1.1. SRS·NFR·PRD과의 관계

| 문서 | 다루는 영역 | 본 FS와의 관계 |
|---|---|---|
| `SRS.md` (요구사항 명세서) | 기능 + 비기능 통합 (상위) | 본 FS는 SRS의 RQ-* 기능 단언을 구체화 |
| `NFR.md` (비기능 명세서) | 성능·보안·가용성 등 | 본 FS 범위 외 — 비기능은 NFR.md 권위 출처 |
| `PRD.md` (제품 요구사항) | 비전·타깃·KPI·MVP 범위 | 본 FS는 PRD의 MVP 범위에 부합 |

- 본 FS의 모든 절은 충족 대상 RQ-ID를 명시한다.
- FS가 RQ-* 의미를 변경하지 않는다 — 의미가 충돌하면 SRS가 우선이며, FS를 갱신한다.
- FS는 기능 구현의 자유도가 있는 영역(API URL 패턴·내부 자료구조·응답 코드 번호 등)을 정의한다.
- 비기능 단언(`NFR-*`)은 본 FS에 직접 두지 않고 NFR.md를 참조만 한다.

### 1.2. 작성 규칙

- 모든 기능 단언은 **(요구) RQ-XXX-NNN** 또는 **(요구 묶음) RQ-XXX-001~016** 처럼 출처 RQ-ID를 병기.
- 외부 발송 시스템 책임 영역은 본 FS 범위 외 — "외부 시스템 인터페이스 문서" 로만 참조.
- 비기능 측면(응답 시간·가용성·인코딩 정책·데이터 격리 등)이 기능 단언과 연결될 때는 `NFR-*` ID로 참조한다 (예: "발송 API 응답 시간은 NFR-PERF-001 적용").

---

## 2. 페이지 인벤토리

> 페이지는 **회원 콘솔 / 어드민 콘솔 / 비회원·체험 / 공개** 4개 도메인으로 분리. 도메인별 인증 정책이 다르다.

### 2.1. 공개 / 비회원

| 페이지 ID | 경로 | 인증 | 출처 RQ |
|---|---|---|---|
| PG-PUB-001 | `/` (랜딩) | — | — |
| PG-PUB-002 | `/signup` (회원 가입 — 개인) | — | RQ-AUTH-001~005 |
| PG-PUB-003 | `/signup/business` (회원 가입 — 사업자) | — | RQ-AUTH-101~106 |
| PG-PUB-004 | `/login` | — | RQ-AUTH-301 |
| PG-PUB-005 | `/find-id` / `/reset-password` | — | RQ-AUTH-303·304 |
| PG-PUB-006 | `/faq` | — | RQ-OPS-001 |
| PG-PUB-007 | `/notices` | — | RQ-OPS-005 |
| PG-PUB-008 | `/docs/sdk`, `/docs/cli`, `/docs/mcp` (가이드) | — | RQ-SDK-002, RQ-CLI-002, RQ-MCP-001 |
| PG-PUB-009 | `/downloads/sdk`, `/downloads/cli` | — | RQ-SDK-001, RQ-CLI-001 |
| PG-TRIAL-001 | `/try` (체험 진입 + 워터마크 배너) | 익명 체험 세션 | RQ-TRIAL-001~016 |

### 2.2. 회원 콘솔 (체험 모드는 동일 UI를 더미 데이터로 노출)

| 페이지 ID | 경로 | 인증 | 출처 RQ |
|---|---|---|---|
| PG-DASHBOARD-001 | `/dashboard` (대시보드) | 회원 | — |
| PG-PROFILE-001 | `/profile` (마이페이지) | 회원 | RQ-MEMBER-001~003 |
| PG-WITHDRAWAL-001 | `/withdrawal` (탈퇴) | 회원 | RQ-MEMBER-004 |
| PG-SECURITY-001 | `/security/two-factor` (2차 인증·신뢰 IP) | 회원 | RQ-AUTH-305~311 |
| PG-CO-001 | `/company/members` (하위 계정 관리) | COMPANY_MASTER | RQ-MEMBER-101~109 |
| PG-CO-002 | `/company/master-roles` (마스터 권한 부여 — A안) | COMPANY_MASTER | RQ-MEMBER-201~204 |
| PG-CO-003 | `/company/tree` (트리 조회 — B안) | COMPANY_MASTER | RQ-MEMBER-302 |
| PG-CALLBACK-001 | `/callbacks` (발신번호 목록) | 회원 + `callback:read` | RQ-CALLBACK-101~104 |
| PG-CALLBACK-002 | `/callbacks/registration` | 회원 + `callback:manage` | RQ-CALLBACK-001~006 |
| PG-KEY-001 | `/keys` (키 목록·스코프) | 회원 | RQ-KEY-001~205 |
| PG-MESSAGE-001 | `/messages/sms`, `/messages/lms`, `/messages/mms`, `/messages/kakao`, `/messages/rcs` | 회원 + `send` | RQ-SEND-001~011 |
| PG-MESSAGE-002 | `/messages/batch` (CSV 일괄) | 회원 + `send` | RQ-SEND-007 |
| PG-MESSAGE-003 | `/messages/scheduled` (예약) | 회원 + `send` | RQ-SEND-010·011 |
| PG-HIST-001 | `/histories` (이력 목록) | 회원 + `history:read` | RQ-SEND-201~204 |
| PG-HIST-002 | `/histories/<id>` (상세) | 회원 + `history:read` | RQ-SEND-202 |
| PG-TPL-001 | `/templates/kakao` | 회원 + `template:read` | RQ-TPL-005~008·010 |
| PG-TPL-002 | `/templates/rcs-brand` | 회원 + `brand:read` | RQ-TPL-001~003 |
| PG-PAY-001 | `/billing/charge` (충전) | 회원 | RQ-PAY-001~005 |
| PG-PAY-002 | `/billing/auto-charge` (자동충전) | 회원 | RQ-PAY-101~109 |
| PG-PAY-003 | `/billing/postpaid` (후불) | 회원 | RQ-PAY-201~206 |
| PG-PAY-004 | `/billing/subscriptions` (구독) | 회원 | RQ-PAY-401~411 |
| PG-PAY-005 | `/billing/refunds` | 회원 | RQ-PAY-501~503 |
| PG-PAY-006 | `/billing/tax` (세금계산서·현금영수증) | 회원 | RQ-PAY-504~507 |
| PG-INQUIRY-001 | `/inquiries` (1:1 문의) | 회원 | RQ-OPS-002~004 |

### 2.3. 어드민 콘솔 (별도 도메인 — 회원 도메인과 분리)

| 페이지 ID | 경로 | 인증 | 출처 RQ |
|---|---|---|---|
| PG-AD-001 | `/admin/login` (운영자 전용, 2차 인증 강제) | — | RQ-ADMIN-001·002 |
| PG-AD-002 | `/admin/operators` (운영자 계정·권한) | SUPER_ADMIN | RQ-ADMIN-003~009 |
| PG-AD-003 | `/admin/review/business` (사업자 가입 심사) | ADMIN | RQ-ADMIN-101~110 |
| PG-AD-004 | `/admin/review/callback` (발신번호 심사) | ADMIN | RQ-ADMIN-108·109 |
| PG-AD-005 | `/admin/members` (회원 관리·통제) | ADMIN | RQ-ADMIN-201~207 |
| PG-AD-006 | `/admin/abuse` (이상 패턴·발송 통제) | ADMIN | RQ-ADMIN-301~308 |
| PG-AD-007 | `/admin/routing` (카카오·RCS 라우팅) | ADMIN | RQ-ADMIN-401~405 |
| PG-AD-008 | `/admin/finance` (환불·세금·캐시·구독·후불) | ADMIN | RQ-ADMIN-501~516 |
| PG-AD-009 | `/admin/policies` (자동 승인·테스트 한도·쿨링 오프 정책) | SUPER_ADMIN | RQ-ADMIN-611·612·613 |
| PG-AD-010 | `/admin/keys/review` (운영 키 수동 검토 큐) | ADMIN | RQ-ADMIN-603 |
| PG-AD-011 | `/admin/sdk` / `/admin/cli` / `/admin/mcp` (버전·매뉴얼·코퍼스) | ADMIN | RQ-ADMIN-604~609 |
| PG-AD-012 | `/admin/cs` (1:1 문의·챗봇·FAQ·공지·외주 위임) | ADMIN | RQ-ADMIN-701~707 |
| PG-AD-013 | `/admin/dashboard` (운영 대시보드·통계) | ADMIN | RQ-ADMIN-801~803, RQ-TRIAL-015 |
| PG-AD-014 | `/admin/system` (인코딩·약관·감사 로그) | SUPER_ADMIN | RQ-ADMIN-804~807 |

---

## 3. 사용자 플로우

### 3.1. 개인 회원가입 → 발송

```
[비회원] /signup
   ↓ 휴대폰 본인 인증 (RQ-AUTH-001)
   ↓ 아이디(이메일)/비밀번호/이름 입력 (RQ-AUTH-002)
   ↓ 약관 동의 (RQ-AUTH-004)
   ↓ 가입 즉시 로그인 가능 (이메일 인증은 비동기 RQ-AUTH-003)
[회원] /callers/registration
   ↓ 발신번호 입력 → 본인 인증 정보로 즉시 등록 (RQ-CALLBACK-001)
   ↓ 시스템: KISA 사전 등록 자동 연계 (RQ-CALLBACK-004)
[회원] /keys
   ↓ Test Key 발급 (RQ-KEY-001) — 채널별 테스트 한도 자동 부여 (RQ-TEST-005)
   ↓ 스코프 선택 (RQ-KEY-101) — 카탈로그는 §5
[회원] /messages/sms 또는 SDK/CLI
   ↓ 발신번호·수신번호·텍스트 입력
   ↓ 시스템: 정합성 검증 → 인코딩 변환 → 발송 테이블 적재 (§7)
   ↓ 응답: 발송 ID + 접수 상태 (RQ-SEND-104)
[운영자 키 전환 시]
   ↓ /keys → "운영 키로 전환" → 자동 승인 시도 (§8.3)
```

### 3.2. 사업자 가입 → 심사 → 승인

```
[비회원] /signup/business
   ↓ 휴대폰 본인 인증 + 사업자 정보 입력 (RQ-AUTH-101)
   ↓ 사업자 등록증 업로드 (RQ-AUTH-102)
   ↓ 신청 완료 → 상태 = "심사중" (RQ-AUTH-103)
[운영자] /admin/review/business
   ↓ 신청 상세 조회 (RQ-ADMIN-102)
   ↓ 승인 OR 반려(사유 작성) OR 보완 서류 요청 (RQ-ADMIN-103·104·105)
[비회원]
   ↓ 반려 시: 사유 조회 (RQ-AUTH-104) → 보완 재제출 (RQ-AUTH-105)
   ↓ 승인 시: 로그인 가능 (RQ-AUTH-106) → 회사 마스터 권한 부여
```

### 3.3. 비회원 체험 모드

```
[비회원] / → "체험하기" 클릭
   ↓ 시스템: 익명 체험 세션 발급 (RQ-TRIAL-002)
   ↓ 더미 회원사 컨텍스트 + 더미 발신번호·키·발송 이력·잔액 사전 적재 (RQ-TRIAL-003)
[비회원/체험 세션] /try → 회원과 동일 UI 노출 (RQ-TRIAL-004)
   ↓ 상시 워터마크 "체험 모드" (RQ-TRIAL-009)
   ↓ 발송 시도 → 실송출 차단, 가상 결과 즉시 반환 (RQ-TRIAL-005)
   ↓ 결제 시도 → 차단, 시뮬레이션 결과만 (RQ-TRIAL-006)
   ↓ "회원가입하고 실제로 사용하기" CTA 노출 (RQ-TRIAL-010)
[비회원]
   ↓ 가입 완료 → 더미 데이터는 폐기, 운영 계정 새로 시작 (RQ-TRIAL-014)
```

### 3.4. 운영 키 전환 자동 승인

```
[회원] /keys → "운영 키로 전환" (RQ-KEY-008)
   ↓ 시스템: 자동 승인 조건 평가
        ✓ 가입 심사 완료
        ✓ 발신번호 KISA 사전 등록 완료
        ✓ 결제 수단 / 캐시 잔액 확보
        ✓ 채널별 등록 충족
        ✓ 어뷰징 패턴 미감지
   ↓ 모두 충족 → 즉시 자동 승인 + 운영 키 활성화 (RQ-KEY-009)
   ↓ 위험 신호 감지 → 자동 승인 보류, 수동 검토 큐로 라우팅 (RQ-KEY-010)
[회원]
   ↓ 결과 통보(메일/인앱): 즉시 승인 / 수동 검토 보류 / 거부 (RQ-KEY-011)
[운영자 — 보류 시] /admin/keys/review
   ↓ 보류 사유 + 어뷰징 신호 검토 → 승인/반려 (RQ-ADMIN-603)
```

### 3.5. 잔액 부족 분기 처리

```
[회원/SDK/CLI] 발송 요청
   ↓ 시스템: 적재 전 사전 평가 (RQ-PAY-006)
        잔액 - 차감 예정액 ≥ 0  →  적재 진행 (정상)
        잔액 - 차감 예정액 < 0  →  분기 라우팅 ↓
   ↓ 자동결제 활성화? (RQ-PAY-008)
        Y → 부족분 자동결제 안내 → 회원 동의 시 결제 후 적재
        N → ↓
   ↓ 후불 모델 가입? (RQ-PAY-009)
        Y → 부족분 후불 처리 + 청구서 누적 → 적재
        N → ↓
   ↓ 부분 발송 / 전체 취소 분기 (RQ-PAY-010)
        부분 발송 선택  → 가능한 N건 적재, M건 INSUFFICIENT_BALANCE 반환 (RQ-PAY-011)
        전체 취소 선택  → 적재 중단
```

### 3.6. 구독 가입 → 쿨링오프 → 해지

```
[회원] /billing/subscriptions → 등급 선택 → 가입 (RQ-PAY-402)
   ↓ 등급 포인트 적립 + 결제 주기 시작
[발송 시] 포인트 차감 우선 → 부족 시 캐시 차감 (RQ-PAY-406·407)
[회원] 해지 클릭 (시점 T)
   ↓ 시스템: T - 가입시각 ≤ 1일?
        Y (쿨링오프 내) → 결제 금액 - 사용분(차감 포인트의 정가 환산) = 잔액 환불 (RQ-PAY-408)
        N (경과)        → 환불 없음, 결제 주기 종료까지 사용 보장 → 종료일 자동 만료 + 자동 갱신 중지 (RQ-PAY-410)
   ↓ 해지 결과 안내(환불 가능 여부·예정 금액·사용 종료일) + 회원 명시 확인 (RQ-PAY-411)
```

---

## 4. 도메인 모델

### 4.1. 핵심 엔티티 관계 (개략)

```
Company (회사)              Account (계정 — MEMBER / COMPANY_MASTER / COMPANY_MEMBER / ADMIN / SUPER_ADMIN)
   │   1                          │   N
   │   ───────────────────────────┘
   │
   ├─ N CallerId (발신번호 — KISA 등록 상태 보유)
   ├─ N ApiKey (test/prod, scopes[])
   ├─ N Send (발송 — 발송 테이블; 외부 발송 시스템이 polling)
   ├─ 1 CashBalance
   ├─ 0..1 PointBalance (구독)
   ├─ 0..1 AutoChargeSetting (자동충전)
   └─ 0..1 PostpaidContract (후불)

KakaoTemplate / RcsBrand — Company 단위 등록·심사 상태 보유
SubscriptionPlan / Subscription — 구독 1차 도입
Invoice / BillingCycle — 후불 1차 도입
```

### 4.2. 회사 마스터 모델 — A안 (권한 부여형) DB 스키마 ✅ (확정)

> **2026-04-30 §A 결정: A안 채택**. 1차 구현은 본 절 스키마를 기준으로 한다. B안(§4.3)은 비교 참고용이며 1차 범위 외.

```sql
-- §14.A A안
CREATE TABLE companies (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  business_no VARCHAR(20)
);

CREATE TABLE accounts (
  id BIGINT PRIMARY KEY,
  company_id BIGINT REFERENCES companies(id),  -- nullable: 개인 회원
  email VARCHAR(255) UNIQUE,
  password_hash VARCHAR(255),
  status VARCHAR(20)  -- ACTIVE / SUSPENDED / WITHDRAWN
);

CREATE TABLE account_role (
  account_id BIGINT REFERENCES accounts(id),
  role VARCHAR(32),  -- MEMBER / COMPANY_MASTER / COMPANY_MEMBER / ADMIN / SUPER_ADMIN
  granted_by BIGINT REFERENCES accounts(id),
  granted_at TIMESTAMP,
  PRIMARY KEY (account_id, role)
);

-- 발신번호·키는 company_id 단위 종속 + 등록자 기록
CREATE TABLE caller_ids (
  id BIGINT PRIMARY KEY,
  company_id BIGINT REFERENCES companies(id),
  registered_by BIGINT REFERENCES accounts(id),
  number VARCHAR(20),
  kisa_status VARCHAR(20)
);
```

특징: 권한 이관 시 `account_role` 행 갱신만으로 마스터 변경, 계정 자체는 유지.

### 4.3. 회사 마스터 모델 — B안 (트리 종속형) DB 스키마 ❌ (1차 범위 외, 비교 참고용)

> §A 결정으로 1차 구현은 §4.2 A안 스키마 사용. 본 절은 의사결정 비교용으로만 보존한다.

```sql
-- §14.A B안
CREATE TABLE companies (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  business_no VARCHAR(20),
  master_account_id BIGINT  -- 회사 마스터 1인 고정
);

CREATE TABLE accounts (
  id BIGINT PRIMARY KEY,
  company_id BIGINT REFERENCES companies(id),
  is_company_master BOOLEAN DEFAULT FALSE,
  email VARCHAR(255) UNIQUE,
  password_hash VARCHAR(255)
);

-- 발신번호·키는 회사 마스터 계정에 직접 종속
CREATE TABLE caller_ids (
  id BIGINT PRIMARY KEY,
  master_account_id BIGINT REFERENCES accounts(id),
  number VARCHAR(20),
  kisa_status VARCHAR(20)
);
```

특징: 담당자 퇴사 시 `accounts.password_hash` 갱신 + 신규 담당자에게 재배포(RQ-MEMBER-301). 법인 대표번호는 마스터에 등록되어 하위 계정이 자동 상속(RQ-MEMBER-303).

### 4.4. 옵션 결제 모델

| 옵션 | 도입 조건 | 추가 테이블 |
|---|---|---|
| **자동충전** (1차 도입) | RQ-PAY-101 활성화 | `auto_charge_setting (account_id, threshold, payment_method, daily_limit, monthly_limit)` |
| **후불** (1차 도입) | RQ-PAY-201 신청·승인 | `postpaid_contract`, `billing_cycle`, `invoice`, `credit_limit`, `collateral` |
| **구독** (1차 도입) | RQ-PAY-402 가입 | `subscription_plan (tier, monthly_fee, points_granted)`, `subscription (account_id, plan_id, started_at, status, period_end)`, `point_balance (account_id, balance, refilled_at)`, `cash_balance (account_id, balance)` 분리 |

차감 우선순위: 포인트 → 캐시 (RQ-PAY-407).

---

## 5. API Key 스코프 카탈로그

> SRS §5.3에서 본 절로 이전. SRS는 RQ-KEY-101·201~205 단언만 보유.

### 5.1. 스코프 코드

| 스코프 코드 | 의미 | 캐시 차감 |
|---|---|---|
| `send` | 모든 채널 발송 (`send:*` 상위 묶음) | Y |
| `send:sms` / `send:lms` / `send:mms` / `send:kakao` / `send:rcs` | 채널 한정 발송 | Y |
| `history:read` | 발송 이력 목록·상세·필터 검색 | N |
| `callback:read` | 발신번호 목록·상태 조회 | N |
| `callback:manage` | 발신번호 등록·삭제 (CLI·웹 명시 명령 전용 — MCP 비노출) | N |
| `key:read` | 자신의 API Key 목록·환경 조회 | N |
| `balance:read` | 캐시 잔액·테스트 한도·사용 내역 | N |
| `template:read` | 카카오 템플릿 목록·심사 상태 | N |
| `brand:read` | RCS 브랜드 목록 | N |

### 5.2. 조회 범위 (Read Scope)

| 코드 | 동작 | 적용 |
|---|---|---|
| `scope:key` (기본) | 해당 키로 직접 발송한 건만 조회 | 키 유출 시 노출 범위 축소 |
| `scope:member` (옵션) | 같은 회원의 다른 키 / 웹 콘솔 발송 포함 전체 | 회사 마스터의 통합 모니터링 키 |

### 5.3. 권장 프리셋 (RQ-KEY-205)

| 프리셋 | 포함 스코프 | 용도 |
|---|---|---|
| 발송 전용 | `send`, `callback:read` | 외부 백엔드의 알림 발송 |
| 조회 전용 | `history:read`, `balance:read` | 모니터링 대시보드 |
| 통합 모니터링 | `history:read` + `scope:member` | 회사 마스터 |
| 풀 권한 | 모든 스코프 + `scope:member` | 관리자 도구 |

### 5.4. 스코프 미허용 호출 응답

```json
HTTP 403
{
  "error": "SCOPE_NOT_GRANTED",
  "required_scope": "send:kakao",
  "granted_scopes": ["send:sms", "history:read"]
}
```

---

## 6. 진입점별 인터페이스 상세

### 6.1. SDK (Python — 1차)

```python
# 초기화
from wisecan import Client
client = Client(api_key="WSC-PROD-...", env="prod")  # env: "test" or "prod" (RQ-SDK-004)

# 발송 (RQ-SDK-005)
result = client.send_sms(callback="0212345678", destaddr="01012345678", text="안녕하세요")
result = client.send_lms(callback="...", destaddr="...", text="...", title="...")
result = client.send_mms(callback="...", destaddr="...", text="...", image_path="cat.jpg")
result = client.send_kakao(callback="...", destaddr="...", template_id="TPL_001", variables={...})
result = client.send_rcs(callback="...", destaddr="...", brand_id="BRAND_001", payload={...})

# 결과
result.send_id        # 발송 ID
result.status         # ACCEPTED / REJECTED
result.reason         # REJECTED 시 사유
```

### 6.2. MCP

#### 6.2.1. 인증 (RQ-MCP-004)

AI Agent 클라이언트 설정 시 환경 변수 또는 클라이언트 설정으로 API Key 등록.

```json
{
  "mcpServers": {
    "wisecan": {
      "url": "https://mcp.wisecan.com",
      "headers": { "Authorization": "Bearer WSC-PROD-..." }
    }
  }
}
```

#### 6.2.2. 도구 정의 (요약)

| 도구명 | 입력 키 | 출처 RQ |
|---|---|---|
| `wsc.docs.search` | `topic`, `lang` | RQ-MCP-001 |
| `wsc.snippet.search` | `lang`, `task` | RQ-MCP-002 |
| `wsc.send.sms` / `lms` / `mms` / `kakao` / `rcs` | callback/destaddr/text/options | RQ-MCP-003 |
| `wsc.history.list` | `from_date`, `to_date`, `channel`, `destaddr` | RQ-MCP-007 |
| `wsc.history.get` | `send_id` | RQ-MCP-007 |
| `wsc.caller.list` | — | RQ-MCP-008 |
| `wsc.key.list` | — | RQ-MCP-009 |
| `wsc.balance.read` | — | RQ-MCP-010 |
| `wsc.usage.read` | `from_date`, `to_date` | RQ-MCP-011 |
| `wsc.template.kakao.list` | — | RQ-MCP-012 |
| `wsc.brand.rcs.list` | — | RQ-MCP-013 |

#### 6.2.3. MCP 비노출 (RQ-MCP-014)

다음 액션은 **MCP에 도구로 노출하지 않는다.** 회원이 의도적으로 웹 콘솔 또는 CLI 명시 명령으로만 수행.

- API Key 발급·폐기·재발급
- 발신번호 등록·삭제
- 결제·충전·환불
- 키 권한(스코프) 변경
- 회사 하위 계정 관리

### 6.3. CLI (`wsc`)

> SRS §6.3에서 본 절로 이전. SRS는 RQ-CLI-001~405 단언만 보유.

#### 6.3.1. 설치 (RQ-CLI-001)

```bash
# macOS
brew install wisecan/tap/wsc

# Ubuntu / Debian
curl -fsSL https://wisecan.com/cli/install.sh | sh

# Windows
winget install wisecan.wsc

# Python 사용자
pip install wisecan-cli   # 동일 바이너리 wrapping
```

#### 6.3.2. 인증·환경 (RQ-CLI-003~006)

```bash
wsc auth login                        # 대화형: API Key 입력 → OS 키체인 저장 (RQ-CLI-403)
wsc config use-env test               # test ↔ prod 전환 (RQ-CLI-004)
wsc config show                       # 설정 확인 (RQ-CLI-005)
export WISECAN_API_KEY=WSC-PROD-...   # 환경 변수 인증 (RQ-CLI-006)
```

설정 파일: `~/.wisecan/config.toml` (chmod 600 또는 OS 키체인).

#### 6.3.3. 발송 명령

```bash
# SMS / LMS / MMS (RQ-CLI-101~103)
wsc send sms --callback 0212345678 --destaddr 01012345678 --text "안녕하세요"
wsc send lms --callback ... --destaddr ... --text "..." --title "공지"
wsc send mms --callback ... --destaddr ... --text "..." --image cat.jpg

# 카카오 알림톡 / RCS (RQ-CLI-104·105)
wsc send kakao --template-id TPL_001 --destaddr 01012345678 --vars '{"name":"홍길동"}'
wsc send rcs   --brand-id BRAND_001  --destaddr 01012345678 --payload payload.json

# 일괄·예약·폴백 (RQ-CLI-106~108)
wsc send batch --file recipients.csv --channel sms --callback ...
wsc send sms --callback ... --destaddr ... --text ... --schedule-at "2026-05-01T09:00:00+09:00"
wsc send kakao --template-id ... --destaddr ... --fallback lms
```

응답 (stdout, RQ-CLI-109):
```
SEND_ID=snd_01HQABC123  STATUS=ACCEPTED
```

`--output json` 시:
```json
{ "send_id": "snd_01HQABC123", "status": "ACCEPTED", "destaddr": "01012345678" }
```

#### 6.3.4. 조회·관리 명령

```bash
wsc history list --from-date 2026-04-01 --channel kakao   # RQ-CLI-201·203
wsc history get snd_01HQABC123                            # RQ-CLI-202

wsc caller list                                           # RQ-CLI-204
wsc caller register --number 0212345678 --type personal   # RQ-CLI-205
wsc caller delete clr_01HQABC                             # RQ-CLI-206

wsc key list                                              # RQ-CLI-207
wsc key create --env test --scopes send,history:read      # RQ-CLI-208
wsc key revoke key_01HQABC                                # RQ-CLI-209

wsc balance                                               # RQ-CLI-210 (캐시 + 채널별 테스트 한도)
wsc usage  --from-date 2026-04-01                         # RQ-CLI-211

wsc template list                                         # RQ-CLI-212
wsc brand list                                            # RQ-CLI-213
```

#### 6.3.5. 가이드·도움말 (RQ-CLI-301~303)

```bash
wsc --help
wsc send --help
wsc docs sdk-init                # 정적 가이드, 무인증
wsc snippet python send-sms      # 정적 코드 스니펫, 무인증
```

#### 6.3.6. 운영 편의·보안

- **자동 업데이트** (RQ-CLI-401·402): 새 버전 발견 시 stderr 안내, `wsc update` 로 갱신.
- **종료 코드** (RQ-CLI-404): `0` 성공 / `1` 인증 실패 / `2` 잔액 부족 / `3` 발송 실패 / `4` 입력 검증 실패 / `5` 네트워크.
- **디버그** (RQ-CLI-405): `--verbose` / `--debug`, 민감 헤더는 마스킹.

### 6.4. MCP ↔ CLI 동등성 매트릭스

> SRS §6.4에서 본 절로 이전.

| 기능 | MCP | CLI | 동등성 |
|---|---|---|---|
| 도입 가이드 조회 | `wsc.docs.search` (RQ-MCP-001) | `wsc docs <topic>` (RQ-CLI-302) | 동일 코퍼스 (RQ-ADMIN-609) |
| 코드 스니펫 | `wsc.snippet.search` (RQ-MCP-002) | `wsc snippet <lang> <task>` (RQ-CLI-303) | 동일 코퍼스 |
| 메시지 발송 (모든 채널) | `wsc.send.*` (RQ-MCP-003) | `wsc send *` (RQ-CLI-101~108) | ✓ 캐시 차감 |
| 인증 키 등록 | MCP 클라이언트 설정 (RQ-MCP-004) | `wsc auth login` (RQ-CLI-003) | ✓ |
| 발송 이력 조회 | `wsc.history.list/get` (RQ-MCP-007) | `wsc history list/get` (RQ-CLI-201~203) | ✓ |
| 발신번호 목록 | `wsc.caller.list` (RQ-MCP-008) | `wsc caller list` (RQ-CLI-204) | ✓ |
| API Key 목록 | `wsc.key.list` (RQ-MCP-009) | `wsc key list` (RQ-CLI-207) | ✓ |
| 잔액·테스트 한도 | `wsc.balance.read` (RQ-MCP-010) | `wsc balance` (RQ-CLI-210) | ✓ |
| 사용 내역 | `wsc.usage.read` (RQ-MCP-011) | `wsc usage` (RQ-CLI-211) | ✓ |
| 카카오 템플릿 | `wsc.template.kakao.list` (RQ-MCP-012) | `wsc template list` (RQ-CLI-212) | ✓ |
| RCS 브랜드 | `wsc.brand.rcs.list` (RQ-MCP-013) | `wsc brand list` (RQ-CLI-213) | ✓ |
| **보안 민감 액션** (키 발급/폐기·발신번호 등록/삭제·결제·충전·환불·스코프 변경) | **노출 X** (RQ-MCP-014) | `wsc caller register/delete`, `wsc key create/revoke` 등 명시 명령 (RQ-CLI-205·206·208·209) | 비대칭 (의도) |

### 6.5. 토큰 경제학 (참고)

> SRS §6 도입부에서 본 절로 이전. 본 서비스가 측정·과금하지 않는 외부 비용 데이터.

| 진입점 | 호출당 평균 토큰 (참고: Jira 티켓 1건 조회 기준) | 상대 비용 |
|---|---|---|
| MCP | 4,000~6,000 | 1.0× |
| CLI (`--output plain`) | 600~900 | 0.15~0.20× |

권장 사용 분리:
- 자연어 단발 명령 → MCP
- 자동화 루프·대량 처리 → AI Agent에서 CLI 위임 (`!wsc history list --output json`)
- 자기 백엔드 코드 → SDK
- 셸/CI → CLI 직접

---

## 7. 발송 파이프라인

### 7.1. 책임 경계

```
┌─────────────────────────────────────────┐    ┌─────────────────────────────────┐
│           본 서비스 (W*)                 │    │       외부 발송 시스템          │
│                                         │    │                                 │
│  [요청 인입] SDK / MCP / CLI / 웹       │    │                                 │
│       │                                 │    │                                 │
│       ▼                                 │    │                                 │
│  정합성 검증                             │    │                                 │
│  (인증, 스코프, 한도, KISA, 스팸 필터,    │    │                                 │
│   광고 의무 표기, 잔액 사전 평가)         │    │                                 │
│       │                                 │    │                                 │
│       ▼                                 │    │                                 │
│  인코딩 변환 (UTF-8 → EUC-KR)            │    │                                 │
│       │                                 │    │                                 │
│       ▼                                 │    │                                 │
│  발송 테이블 INSERT  ──────────────────────► (polling) ─────►  채널 라우팅      │
│  (채널·발신번호·수신·메타·예약·폴백)      │    │                  중계사 1:1     │
│       │                                 │    │                  폴백 실행      │
│       ▼                                 │    │                  실 송출        │
│  발송 ID + ACCEPTED 응답                 │    │                                 │
└─────────────────────────────────────────┘    └─────────────────────────────────┘
```

### 7.2. 발송 테이블 컬럼 카탈로그 (외부 시스템 인터페이스 — 최소 항목)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `send_id` | VARCHAR | ULID, 본 서비스가 채번 |
| `account_id` | BIGINT | 발송 회원 |
| `api_key_id` | BIGINT | 어느 키로 발송했나 (NULL = 웹 콘솔) |
| `channel` | VARCHAR | SMS / LMS / MMS / KAKAO / RCS |
| `callback` | VARCHAR | 발신번호 (KISA 등록 검증 통과한 값) |
| `destaddr` | VARCHAR | 수신번호 |
| `payload` | TEXT | EUC-KR 변환 후 본문 |
| `image_url` | VARCHAR | MMS 시 |
| `template_id` | VARCHAR | 카카오 시 |
| `brand_id` | VARCHAR | RCS 시 |
| `fallback` | VARCHAR | 폴백 채널 (NULL 가능) |
| `scheduled_at` | TIMESTAMP | 예약 시각 (NULL = 즉시) |
| `routing_meta` | JSON | 운영자 매핑 정보 (RQ-ADMIN-401, 회원 비노출) |
| `created_at` | TIMESTAMP | 적재 시각 |
| `status` | VARCHAR | ACCEPTED (본 서비스 종착) / 외부 시스템 갱신 영역 |

### 7.3. 회원 노출 추상화 원칙 (RQ-SEND-308)

회원에게 응답·UI·로그·이력 어디에서도 노출하지 않는 항목:

- 카카오·RCS 중계사 식별자 (LG CNS / KT / 인포뱅크)
- `routing_meta` 컬럼 내용
- 라우팅 매핑 변경 이력 (운영자 전용 — RQ-ADMIN-401·402)
- 내부 채널 ID

회원이 인지하는 채널 단위는 `SMS / LMS / MMS / 카카오 알림톡 / RCS` 까지로 한정.

---

## 8. 상태 머신

### 8.1. 사업자 가입 심사

```
[SUBMITTED] ── 운영자 검토 ──► [UNDER_REVIEW]
                                │
                ┌───────────────┼─────────────────┐
                ▼               ▼                 ▼
          [APPROVED]      [REJECTED]        [REQUEST_SUPPLEMENT]
                            │                     │
                            └─ 신청자 재제출 ◄─────┘
                               (RQ-AUTH-105)
```

### 8.2. 발신번호 등록

```
[REGISTERED]   ── 개인 (RQ-CALLBACK-001, 본인 인증 즉시)
[SUBMITTED] ── 운영자 검토 ──► [UNDER_REVIEW]   ── 임직원 (RQ-CALLBACK-002)
                                  │
                  ┌───────────────┴─────────┐
                  ▼                         ▼
            [REGISTERED]              [REJECTED]
                  │
                  ▼
           [EXPIRED] (자동 만료) / [DELETED] (회원 삭제)
```

### 8.3. 운영 키 전환 (RQ-KEY-008~011)

```
[TEST_KEY] ── 회원 전환 신청 ──► [AUTO_CHECK]
                                   │
                ┌──────────────────┼──────────────────┐
                ▼                  ▼                  ▼
          [AUTO_APPROVED]    [HOLD_FOR_REVIEW]     [AUTO_DENIED]
          (즉시 운영 키 활성)    │                  (조건 미충족 통보)
                                ▼
                          (운영자 큐 — RQ-ADMIN-603)
                                │
                       ┌────────┴────────┐
                       ▼                 ▼
                 [APPROVED]         [REJECTED]
```

### 8.4. 발송 라이프사이클 (본 서비스 영역)

```
[REQUEST] ── 정합성 검증 ──► [VALID] ── 인코딩 변환 ──► [READY] ── 적재 ──► [ACCEPTED]
              │ FAIL                                                          (응답)
              ▼
         [REJECTED]
         (필요 스코프 / 잔액 부족 / KISA 미등록 / 스팸 / 광고 표기 등)
```

이후 상태(SENT / DELIVERED / FAILED)는 외부 발송 시스템이 갱신.

### 8.5. 카카오 템플릿

```
[SUBMITTED] ──► [UNDER_REVIEW] ──► [APPROVED] / [REJECTED]
                                       │
                                       ▼
                                   [DELETED]
```

### 8.6. 환불 신청 (RQ-PAY-501~503, RQ-ADMIN-501~503)

```
[REQUESTED] ──► [UNDER_REVIEW] ──► [APPROVED & PAID] / [REJECTED]
     │
     └─ 회원 취소 ──► [CANCELLED]  (처리 전에만 가능)
```

### 8.7. 구독

```
[ACTIVE]
  │ ─ 회원 해지 (T)
  ▼
T - started_at ≤ 1일?
  Y → [REFUNDED] (RQ-PAY-408)  사용분 차감 후 잔액 환불
  N → [GRACE_UNTIL_PERIOD_END] ── 결제 주기 종료 ──► [EXPIRED]
                                                      자동 갱신 중지 (RQ-PAY-410)
```

---

## 9. 권한 매트릭스

### 9.1. 회원 페르소나 × 액션 (요약)

| 액션 | MEMBER (개인) | COMPANY_MASTER | COMPANY_MEMBER |
|---|---|---|---|
| 본인 정보 조회·수정 | ✓ | ✓ | ✓ |
| 회사 하위 계정 생성·관리 | — | ✓ | — |
| 회사 마스터 권한 부여 (A안) | — | ✓ | — |
| 발신번호 등록 (개인) | ✓ | — | — |
| 발신번호 등록 (임직원·법인) | — | ✓ (대표번호) | ✓ (임직원, 운영자 심사) |
| API Key 발급·폐기 | ✓ | ✓ | 마스터가 부여한 권한 시 ✓ |
| 키 스코프 설정 | ✓ | ✓ | — (마스터 부여) |
| 결제·환불·세금계산서 | ✓ | ✓ | — |
| 발송 (스코프 보유 시) | ✓ | ✓ | ✓ |
| 발송 이력 조회 | scope 적용 | scope 적용 | scope 적용 |

### 9.2. 운영자 권한 위임

| 역할 | SUPER_ADMIN | ADMIN (역할 위임) |
|---|---|---|
| 운영자 계정 생성·삭제 | ✓ | — |
| 운영자 역할 부여 | ✓ | — |
| 자동 승인 정책 / 테스트 한도 / 쿨링 오프 정책 | ✓ | — |
| 사업자 가입 심사 | ✓ | 위임 시 ✓ |
| 발신번호 등록 심사 | ✓ | 위임 시 ✓ |
| 회원 통제 (비활성화·차단) | ✓ | 위임 시 ✓ |
| 발송 통제·스팸 키워드 | ✓ | 위임 시 ✓ |
| 환불 처리 | ✓ | 위임 시 ✓ |
| 감사 로그 조회 | ✓ | — (RQ-ADMIN-009·807) |

### 9.3. 키 스코프 × 진입점

| 진입점 | 발송 (`send*`) | 조회 (`*:read`) | 관리 (`callback:manage`, 키 발급/폐기 등) |
|---|---|---|---|
| 웹 콘솔 | ✓ | ✓ | ✓ |
| SDK | ✓ | ✓ | ✓ (단, 키 발급/폐기는 자기 코드 호출이라 일반적으로 미사용) |
| MCP | ✓ | ✓ | **차단** (RQ-MCP-014) |
| CLI | ✓ | ✓ | ✓ (명시 명령) |

---

## 10. 에러 코드 / 응답

### 10.1. 인증·권한 (1xxx)

| 코드 | 의미 | HTTP |
|---|---|---|
| `AUTH_REQUIRED` | API Key 누락 | 401 |
| `AUTH_INVALID` | API Key 무효·폐기 | 401 |
| `AUTH_EXPIRED` | 키 만료 | 401 |
| `SCOPE_NOT_GRANTED` | 키에 필요한 스코프 미부여 | 403 |
| `SCOPE_RANGE_DENIED` | `scope:member` 미허용으로 다른 키 발송 조회 차단 | 403 |
| `MFA_REQUIRED` | 운영자 2차 인증 미완료 | 403 |

### 10.2. 발신번호·키 (2xxx)

| 코드 | 의미 | HTTP |
|---|---|---|
| `CALLER_NOT_REGISTERED` | KISA 사전 등록 안된 발신번호 | 400 |
| `CALLER_NOT_OWNED` | 다른 회원·키 권한의 발신번호 사용 시도 | 403 |
| `KEY_NOT_FOUND` | 키 ID 부재 | 404 |
| `KEY_REVOKED` | 폐기된 키 | 403 |

### 10.3. 발송 검증 (3xxx)

| 코드 | 의미 | HTTP |
|---|---|---|
| `INSUFFICIENT_BALANCE` | 잔액 부족 (RQ-PAY-006·011) | 402 |
| `TEST_LIMIT_EXCEEDED` | 채널별 테스트 한도 초과 (RQ-TEST-007) | 429 |
| `KEY_DAILY_LIMIT_EXCEEDED` | 키별 일일 한도 초과 (RQ-KEY-104) | 429 |
| `SPAM_KEYWORD_DETECTED` | 스팸 키워드 적중 (RQ-SEC-003) | 422 |
| `AD_DISCLOSURE_MISSING` | 광고 의무 표기 누락 (RQ-SEC-002) | 422 |
| `TEMPLATE_NOT_APPROVED` | 카카오 템플릿 미승인 | 422 |
| `BRAND_NOT_REGISTERED` | RCS 브랜드 미등록 | 422 |
| `IMAGE_INVALID` | MMS 이미지 형식·용량 오류 | 422 |
| `TEXT_TOO_LONG` | 채널별 길이 초과 | 422 |

### 10.4. 결제 (4xxx)

| 코드 | 의미 | HTTP |
|---|---|---|
| `PAYMENT_FAILED` | PG 결제 실패 | 402 |
| `AUTO_CHARGE_FAILED` | 자동충전 실패 (RQ-PAY-108) | 402 |
| `POSTPAID_OVERDUE` | 후불 연체 발송 차단 (RQ-PAY-206) | 402 |
| `REFUND_NOT_ELIGIBLE` | 쿨링 오프 경과 (RQ-PAY-408) | 422 |

### 10.5. 시스템 (5xxx)

| 코드 | 의미 | HTTP |
|---|---|---|
| `SEND_TABLE_INSERT_FAILED` | 적재 실패 (재시도) | 500 |
| `ENCODING_FAILED` | UTF-8 → EUC-KR 변환 실패 | 500 |
| `EXTERNAL_SYSTEM_UNAVAILABLE` | 외부 발송 시스템 polling 정지 (모니터링 — RQ-ADMIN-805) | 503 |

### 10.6. 부분 발송 응답 (RQ-PAY-011)

```json
HTTP 207 (Multi-Status)
{
  "accepted": [
    { "send_id": "snd_01...", "to": "01011112222" },
    { "send_id": "snd_02...", "to": "01033334444" }
  ],
  "rejected": [
    { "to": "01055556666", "reason": "INSUFFICIENT_BALANCE" }
  ],
  "balance_after": 0
}
```

---

## 11. 입력 검증 규칙 (요약)

### 11.1. 회원 가입 (개인)

| 필드 | 규칙 |
|---|---|
| 이메일 | RFC 5322, 중복 차단, 인증은 비동기 (RQ-AUTH-003) |
| 비밀번호 | 10자 이상, 영문·숫자·특수문자 중 2종 이상 |
| 휴대폰 | 본인 인증 통과한 번호로 자동 채움 |
| 이름 | 2~30자 |

### 11.2. 발신번호 등록

| 필드 | 규칙 |
|---|---|
| 번호 | E.164 또는 한국 국번 정규식, 중복 등록 차단 |
| 등록 유형 | personal / company_employee / company_main |
| 서류 | 임직원·법인은 PDF/JPG ≤ 10MB |

### 11.3. 발송 요청

| 채널 | 텍스트 길이 | 추가 |
|---|---|---|
| SMS | ≤ 90바이트 (EUC-KR 기준) | — |
| LMS | ≤ 2,000바이트 (EUC-KR) | 제목 ≤ 60바이트 |
| MMS | LMS와 동일 + 이미지 1장 (JPEG/PNG ≤ 300KB, 720x1280px) | — |
| 카카오 알림톡 | 등록된 템플릿 필수, 변수 매핑 검증 | 템플릿 승인 상태 = APPROVED |
| RCS | 등록된 브랜드 필수 | 페이로드 스키마 검증 |

수신자 일괄 (CSV): UTF-8, 헤더 `destaddr,vars`(선택), ≤ 100,000행/요청.

---

## 12. 비기능 요구사항 — 본 FS 범위 외

비기능 요구사항(성능·가용성·신뢰성·보안·인코딩·로깅·데이터 격리·확장성·사용성·호환성·유지보수성)은 **`pm/design/NFR.md`** 를 권위 출처로 한다.

본 FS는 표준 정의의 기능 명세서(FRS)이므로 비기능 단언을 직접 포함하지 않는다. 본 FS의 §3~§11에서 기능 단언이 비기능 단언을 참조해야 하는 경우는 `NFR-*` ID로만 참조한다 (예: "발송 API 평균 응답 ≤ 500ms — `NFR-PERF-001`").

이전 버전(v1)에서 본 절에 있던 비기능 요약 표는 **NFR.md §2~§13으로 모두 이전됨**. 주요 매핑:

| FS v1 항목 | 이전 위치 |
|---|---|
| 가용성 — 발송 API SLA 99.5% | `NFR-AVAIL-001` (NFR.md §3.1) |
| 성능 — 발송 API 평균 응답 ≤ 500ms | `NFR-PERF-001` (NFR.md §2.1) |
| 인코딩 — 외부 인입 UTF-8 / 발송 테이블 EUC-KR | `NFR-DATA-001·002` (NFR.md §9.1) |
| 로깅 — API 30일 / 발송 1년 / 감사 5년 | `NFR-DATA-101~106` (NFR.md §9.2) |
| 보안 — TLS 1.2+ 통신 | `NFR-SEC-001` (NFR.md §6.1) |
| 보안 — CLI API Key 저장 (OS 키체인 / chmod 600) | `NFR-SEC-201` (NFR.md §6.3) |
| 보안 — 운영자 신뢰 IP + 2차 인증 | `NFR-SEC-104·105` (NFR.md §6.2) |
| 데이터 격리 — 체험 모드 운영 DB와 분리 | `NFR-DATA-301` (NFR.md §9.4) |
| 확장성 — 글로벌 발송 확장 | `NFR-SCALE-101~105` (NFR.md §5.2) |

---

## 13. A/B 의사결정별 구현 영향

> SRS §14에서 본 절로 이전. SRS는 정책 옵션 선언만 보유.

### 13.1. §A 회사 마스터 권한 — 구현 비교

| 항목 | A안 (권한 부여형) | B안 (트리 종속형) |
|---|---|---|
| **DB** | `accounts` + `account_role` 다대다 | `companies.master_account_id` + `accounts.is_company_master` |
| **발신번호 종속** | `caller_ids.company_id` | `caller_ids.master_account_id` |
| **권한 이관** | `account_role` 행 갱신 | 마스터 계정 비밀번호 변경 |
| **법인 대표번호 상속** | 권한 보유자만 사용 | 모든 하위 계정 자동 상속 |
| **세분 권한 추가** | 유연 (역할 추가) | 트리 1단 고정 → 정책 분기 |
| **감사 추적** | `account_role.granted_by` | 마스터 비밀번호 변경 이력 |

### 13.2. §B 자동 충전 — 1차 도입 확정 ✅

| 1차 도입 구현 |
|---|
| `auto_charge_setting (account_id, threshold, payment_method, daily_limit, monthly_limit, started_at)` |
| PG 정기결제 계약 + 30일 제한 |
| 운영자 모니터링 페이지 (RQ-ADMIN-509) |

### 13.3. §C 구독 — 1차 도입 확정 ✅

| 1차 도입 구현 |
|---|
| `subscription_plan`, `subscription`, `point_balance`, `cash_balance` 분리 |
| 차감 우선순위 = 포인트 → 캐시 (RQ-PAY-407) |
| 쿨링 오프 환불 (1일 / RQ-PAY-408) — 사용분 차감 후 잔액 환불 |
| 결제 주기 종료까지 사용 보장 (RQ-PAY-410) |
| 운영자 포인트 조정·초기화 (RQ-ADMIN-512~516) |

### 13.4. §E 후불 — 1차 도입 확정 ✅

| 1차 도입 구현 |
|---|
| `postpaid_contract`, `billing_cycle`, `invoice`, `credit_limit`, `collateral` |
| 신용 검증 + 보증보험 등록 (RQ-PAY-202·203) |
| 청구서·연체 운영 (RQ-ADMIN-510, RQ-PAY-206) |

---

## 14. 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-04-30 | 오민성 (1차) | SRS v20에서 "어떻게 구현하나" 영역(§5.3 스코프 카탈로그 매핑·§6 토큰 경제학 표·§6.3 CLI 명령 syntax·§6.4 MCP↔CLI 매트릭스·§7.4 발송 시퀀스·§14 A/B 데이터 모델 비교)을 본 FS로 이전. 페이지 인벤토리·사용자 플로우·도메인 모델·상태 머신·권한 매트릭스·에러 코드 사전·입력 검증·비기능 요구사항 신규 작성. |
| 2026-04-30 | 오민성 (v2) | **표준 분류 정렬**. v1의 "WHAT vs HOW" 분리는 IEEE 830 / ISO/IEC/IEEE 29148 표준상 부정확한 표현으로 폐기. (1) 본 FS를 표준 정의의 **Functional Requirements Specification(FRS) — 기능 요구사항만** 다루는 명세서로 재정의. 별도 DS/TS 문서를 두지 않고 본 FS가 기능 설계 영역(페이지·API·CLI·상태 머신)까지 흡수하는 한국 실무 관행 명시. (2) 헤더에 SRS / FRS / NFR / PRD 4문서 관계와 본 FS의 영역·비영역을 명시. (3) **§12 비기능 요구사항 표를 NFR.md (§2~§13)로 모두 이전**, 본 절은 참조 인덱스 + 매핑표로 축약. (4) §1 "SRS와 FS의 관계" 표를 표준 정의 기반(SRS·NFR·PRD 3축)으로 갱신, "WHAT/HOW" 표현 제거. (5) RQ-* 및 페이지 ID 변경 없음 — 모든 인용처 그대로 호환. |
| 2026-04-30 | 오민성 (v3) | **경로 명사·복수형 통일 + callback/destaddr 명명 + §B/§C/§E 도입 확정 반영**. (1) **페이지 경로 일괄 정리**: `/my/*` → `/dashboard·/profile·/withdrawal·/security/two-factor`, `/callers` → `/callbacks`, `/send/*` → `/messages/*`, `/history` → `/histories`, `/billing/subscription` → `/billing/subscriptions`, `/billing/refund` → `/billing/refunds`, `/ask` → `/inquiries`, `/notice` → `/notices`, `/download/*` → `/downloads/*`, `/admin/review/caller` → `/admin/review/callback`, `/admin/policy` → `/admin/policies`, `/trial` → `/try`. PG-* ID 일부도 함께 명사화 (PG-MY-* → PG-DASHBOARD/PROFILE/WITHDRAWAL/SECURITY-001, PG-CALLER-* → PG-CALLBACK-*, PG-SEND-* → PG-MESSAGE-*, PG-OPS-001 → PG-INQUIRY-001). (2) **발신번호 = callback / 수신번호 = destaddr** — 페이지 경로·스코프(callback:read·manage)·발송 테이블 컬럼(from_number/to_number → callback/destaddr)·CLI 옵션(--from/--to → --callback/--destaddr)·SDK 매개변수(from_/to → callback/destaddr)·MCP 도구·CSV 헤더 일괄 치환. (3) **§13.2 / §13.3 / §13.4 §B/§C/§E 표를 단일 안(1차 도입)만 남기고 미도입 컬럼 제거**. §3.6 / §4.4 / §8.7 헤딩에서 "(§? A안 도입 시)" 분기 표기 제거. (4) `/login`·`/find-id`·`/reset-password`·`/billing/charge`·`/billing/auto-charge`·`/billing/postpaid` 등 관례·명사 표현은 보존. RQ-* / NFR-* ID 변경 없음 — 모든 인용처 호환. |

# NFR — WiseCan 통합 메시징 서비스 비기능 명세서 (Non-Functional Requirements)

> 작성일: 2026-04-30
> 상태: 1차 초안 (v1)
>
> **상위 / 짝 문서**
> - `pm/design/SRS.md` — 요구사항 명세서 (Software Requirements Specification): 기능 + 비기능을 모두 포괄하는 상위 문서. 본 NFR을 §17에서 요약·인용
> - `pm/design/FS.md` — 기능 명세서 (Functional Requirements Specification): 기능 요구사항만 (페이지·API·CLI·상태 머신 등 기능 설계까지 포함)
> - `pm/plan/PRD.md` — 비전·타깃·KPI·MVP 범위

---

## 1. 개요

### 1.1. 본 문서의 위치 — 표준 분류 기준

업계 표준 (IEEE 830 / ISO/IEC/IEEE 29148) 분류에 따른 문서 구분:

| 약어 | 정식 명칭 | 다루는 영역 |
|---|---|---|
| **SRS** | Software Requirements Specification | **기능 + 비기능 요구사항 전체** (상위 통합 문서) |
| **FRS / FS** | Functional Requirements Specification | **기능 요구사항만** (SRS의 하위) |
| **NFR** | Non-Functional Requirements | 성능·보안·가용성·확장성·운영·법규 (SRS의 하위) |
| **DS / TS** | Design Specification / Technical Specification | 화면·API·DB 스키마·상태 머신 등 구현 영역 (요구사항이 아닌 설계 산출물) |

표준 관계: `SRS ⊇ FRS + NFR + 제약·외부 인터페이스 등`. NFR은 SRS의 한 축이며, FRS와 동격이다.

### 1.2. 본 NFR의 역할

본 문서는 **표준 정의의 NFR 부분**에 해당한다. 검증 방식은 SLA 측정·보안 점검·복구 리허설·접근성 검사 등이다.

- PRD §5.3 (운영 품질/SLA)와 현 `pm/design/FS.md` §12 (비기능 요약 표), `pm/design/SRS.md` 의 RQ-ARCH·RQ-SEC·RQ-TRIAL 중 비기능 측면을 통합·승격해 본 문서로 분리한다.
- 기능 요구사항(`RQ-*`)이 본 NFR을 참조할 때는 `NFR-*` ID로 역참조 가능하다 (§14 매핑).
- 본 NFR이 기능 요구사항과 충돌하면 기능 요구사항의 단언을 우선하고 본 문서를 갱신한다.

### 1.3. 본 저장소의 3문서 체계 (2026-04-30 정리 완료)

본 저장소는 표준 분류 중 **D 패턴 (SRS + FS + NFR)** 을 채택한다. 별도 DS/TS 문서는 두지 않으며, FS가 기능 설계 영역까지 흡수하는 한국 실무 관행을 따른다.

| 본 저장소 파일 | 표준 분류 | 다루는 영역 | 역할 |
|---|---|---|---|
| `pm/design/SRS.md` | Software Requirements Specification | 기능 + 비기능 + 제약 통합 | 상위 통합 문서 — RQ-* 기능 단언 카탈로그 + NFR-* 요약 인용 |
| `pm/design/FS.md` | Functional Requirements Specification | 기능 요구사항 + 기능 설계 | 기능 명세 — 페이지·API·CLI·상태 머신·도메인 모델·에러 코드·입력 검증 |
| `pm/design/NFR.md` (본 문서) | Non-Functional Requirements | 비기능 요구사항 | 비기능 명세 — 성능·보안·가용성·확장성·법규·SLA·검증 계획 |

상호 참조 관계:

- `SRS.md §17` → 본 NFR의 핵심 단언·NFR Prefix 인덱스·미확정 결정을 인용 (SRS가 비기능을 포괄한다는 표준 정의 충족)
- `FS.md` → 비기능 영역은 본 NFR을 권위 출처로 참조 (FS 본문에 비기능 표 미보유)
- 본 NFR `§14` → 본 NFR이 강제·보강하는 SRS의 `RQ-*` 역참조
- 본 NFR `§15` → 각 NFR의 검증 방식

표준 관계: `SRS ⊇ FRS + NFR + 제약·외부 인터페이스 등`. 본 NFR은 SRS의 한 축이며 FRS와 동격이다.

### 1.4. ID 체계

| Prefix | 분류 | 설명 |
|---|---|---|
| `NFR-PERF` | 성능 | 응답 시간·처리량·동시성 |
| `NFR-AVAIL` | 가용성 | SLA·다운타임·DR |
| `NFR-RELY` | 신뢰성 | 적재 성공률·재시도·멱등성 |
| `NFR-SCALE` | 확장성 | 수평 확장·글로벌 확장 |
| `NFR-SEC` | 보안 | 인증·인가·암호화·키 관리·감사 |
| `NFR-PRIV` | 개인정보 | 마스킹·보존·파기·국외 이전 |
| `NFR-COMP` | 컴플라이언스 | KISA·정보통신망법·광고 표기·전자세금계산서 |
| `NFR-DATA` | 데이터 | 인코딩·보존·백업·격리 |
| `NFR-OPS` | 운영 | 로깅·모니터링·알림·배포·롤백 |
| `NFR-USE` | 사용성 | UI 응답·접근성·다국어 |
| `NFR-COMPAT` | 호환성 | 브라우저·OS·디바이스·SDK 런타임 |
| `NFR-MAINT` | 유지보수성 | 코드 컨벤션·문서·테스트 커버리지 |

ID 끝 3자리는 일련번호.

---

## 2. 성능 (NFR-PERF)

> 측정 출처가 없는 "빠르다", "최적화" 같은 표현은 금지. 모든 항목은 P95/P99 또는 절대값으로 단언한다.

### 2.1. 발송 API 응답 시간

| NFR-ID | 항목 | 목표 | 측정 단위 | 출처 |
|---|---|---|---|---|
| NFR-PERF-001 | 발송 API 평균 응답 시간 (적재 완료까지) | ≤ 500ms | P50 | PRD §5.3 |
| NFR-PERF-002 | 발송 API P95 응답 시간 | ≤ 1,000ms | P95 | — |
| NFR-PERF-003 | 발송 API P99 응답 시간 | ≤ 2,000ms | P99 | — |
| NFR-PERF-004 | 일괄 발송 (≤ 100,000행 CSV) 적재 완료 | ≤ 60초 | 절대값 | FS §11.3 |
| NFR-PERF-005 | 예약 발송 적재→실제 송출 시각 편차 | ≤ 60초 | 절대값 | RQ-SEND-010 |

### 2.2. 조회·웹 응답 시간

| NFR-ID | 항목 | 목표 |
|---|---|---|
| NFR-PERF-101 | 발송 이력 목록 조회 (1페이지 50건) | ≤ 800ms (P95) |
| NFR-PERF-102 | 발송 상세 조회 (단건) | ≤ 500ms (P95) |
| NFR-PERF-103 | 잔액·테스트 한도 조회 | ≤ 300ms (P95) |
| NFR-PERF-104 | 어드민 대시보드 (RQ-ADMIN-801) 첫 페인트 | ≤ 2,000ms |
| NFR-PERF-105 | 회원 콘솔 대시보드 (`/my`) 첫 페인트 | ≤ 1,500ms |

### 2.3. CLI / SDK / MCP 응답

| NFR-ID | 항목 | 목표 |
|---|---|---|
| NFR-PERF-201 | CLI 발송 명령 (`wsc send sms`) end-to-end | ≤ 1,500ms (P95) |
| NFR-PERF-202 | CLI 콜드 스타트 (바이너리 기동) | ≤ 200ms |
| NFR-PERF-203 | CLI `--help` / `docs` (정적, 무인증) | ≤ 100ms |
| NFR-PERF-204 | MCP 도구 호출 응답 | ≤ 1,500ms (P95) |
| NFR-PERF-205 | SDK `client.send_sms()` 호출 (네트워크 제외) | ≤ 50ms |

### 2.4. 처리량 (Throughput)

| NFR-ID | 항목 | 목표 (MVP) | 비고 |
|---|---|---|---|
| NFR-PERF-301 | 발송 API 동시 요청 처리 | ≥ 200 req/s | 발송 테이블 INSERT 기준 |
| NFR-PERF-302 | 일괄 발송 처리량 | ≥ 5,000 건/초 (적재) | RQ-SEND-007 |
| NFR-PERF-303 | 동시 접속 회원 수 (웹) | ≥ 500 sessions | 콘솔 동시 사용자 |
| NFR-PERF-304 | 인코딩 변환 (UTF-8 → EUC-KR) 단건 처리 | ≥ 10,000 msg/s | RQ-SEND-305, 메모리 처리 |

### 2.5. 응답 시간 측정 규칙

- 측정 도구: Prometheus + Grafana (`http_request_duration_seconds` histogram).
- 윈도우: 5분 롤링 평균 / 1시간 P95 / 1일 P99.
- 위반 임계 — P95가 24시간 연속 목표 초과 시 `NFR-OPS-301` 알림 발송.

---

## 3. 가용성 (NFR-AVAIL)

### 3.1. SLA 목표

| NFR-ID | 서비스 | 가용성 목표 | 월간 허용 다운타임 | 출처 |
|---|---|---|---|---|
| NFR-AVAIL-001 | 발송 API (외부 인입) | 99.5% | ≤ 3시간 39분 | PRD §5.3 |
| NFR-AVAIL-002 | 회원 콘솔 (웹) | 99.5% | ≤ 3시간 39분 | — |
| NFR-AVAIL-003 | 어드민 콘솔 | 99.0% | ≤ 7시간 18분 | 운영자 한정 |
| NFR-AVAIL-004 | MCP 서버 | 99.0% | ≤ 7시간 18분 | — |
| NFR-AVAIL-005 | CLI 다운로드·docs (정적) | 99.9% | ≤ 43분 | CDN 배포 |
| NFR-AVAIL-006 | 체험 모드 (`/try`) | 99.0% | ≤ 7시간 18분 | RQ-TRIAL-008 (운영 DB와 분리) |

### 3.2. 계획된 점검 (Maintenance Window)

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-AVAIL-101 | 정기 점검 시간대 | 새벽 02:00~04:00 KST (월 1회 이내) |
| NFR-AVAIL-102 | 점검 사전 공지 | 최소 7일 전 공지사항(RQ-OPS-005) + 회원 이메일 |
| NFR-AVAIL-103 | 점검 시간 SLA 제외 | 사전 공지된 점검 시간은 SLA 측정 제외 |

### 3.3. 장애 복구 (RPO/RTO)

| NFR-ID | 시나리오 | RTO (복구 시간) | RPO (데이터 손실 허용) |
|---|---|---|---|
| NFR-AVAIL-201 | 단일 애플리케이션 서버 장애 | ≤ 5분 (LB 헬스체크 + 자동 페일오버) | 0 (무손실) |
| NFR-AVAIL-202 | DB 마스터 장애 | ≤ 15분 (Standby DB promotion) | ≤ 1분 (동기 복제 binlog) |
| NFR-AVAIL-203 | 물리 랙·스위치 장애 | ≤ 30분 (랙 분산 이중화) | ≤ 5분 |
| NFR-AVAIL-204 | IDC 전체 장애 | **MVP 범위 외 — 단일 IDC + 노드·랙·전원·네트워크 이중화로 대응**. 다중 IDC DR(멀티 리전)은 성숙기 진입 시점에 별도 결정 | — |
| NFR-AVAIL-205 | 외부 발송 시스템 장애 | 적재는 정상 (RQ-SEND-301), 송출은 외부 책임 | 발송 테이블 보존 |

### 3.4. 외부 의존성 가용성 분리

본 서비스 SLA에서 제외되는 외부 의존성:

| 외부 시스템 | 영향 | 측정 |
|---|---|---|
| KISA 사전 등록 API | 발신번호 등록 자동 연계(RQ-CALLBACK-004) 일시 실패 시 수동 fallback | 별도 모니터링 (NFR-OPS-302) |
| 카카오·RCS 중계사 | 외부 발송 시스템 책임 영역 (FS §7.1) | 본 서비스 SLA 제외 |
| PG (결제) | 결제 실패는 NFR-RELY-205 정책으로 처리 | 별도 측정 |
| 외부 발송 시스템 (테이블 polling) | 송출 가용성은 외부 책임 | 본 서비스는 적재 가용성만 보장 |

### 3.5. 인프라 구성 — 자체 서버 + 단일 IDC 이중화 (MVP 확정 정책)

본 서비스는 클라우드 멀티 리전이 아닌 **자체 서버 + 단일 IDC 이중화 구성**을 기본 정책으로 한다. 다중 IDC DR(멀티 리전)은 MVP 범위 외이며, 성숙기 진입 시점에 별도 결정한다 (§16.2).

| NFR-ID | 영역 | 이중화 방식 |
|---|---|---|
| NFR-AVAIL-301 | 애플리케이션 서버 | LB 후단 Active-Active (≥ 2대), 헬스체크 자동 페일오버 |
| NFR-AVAIL-302 | DB | Master + Standby 동기 복제, 자동 promotion (NFR-AVAIL-202 충족) |
| NFR-AVAIL-303 | Read Replica | 조회 계열(`history:read` 등) 부하 분산용 별도 Replica |
| NFR-AVAIL-304 | 캐시 (Redis) | 클러스터 모드, 단일 노드 장애 무중단 (NFR-SCALE-004 동일 정책) |
| NFR-AVAIL-305 | 스토리지 | RAID + 일별 백업 (NFR-DATA-201) |
| NFR-AVAIL-306 | 네트워크 | 듀얼 NIC + 다중 ISP, 코어 스위치 이중화 |
| NFR-AVAIL-307 | 전원 | UPS + 발전기 |
| NFR-AVAIL-308 | 백업 보관 | 별도 IDC 또는 오프사이트 콜드 보관 (RTO 제약 없음, NFR-DATA-202) |
| NFR-AVAIL-309 | 모니터링 | 헬스체크 + 자동 알림 (NFR-OPS-201~204 연계) |

**범위 외 (성숙기에 재검토)**: 다중 IDC 액티브, 핫 스탠바이, 멀티 리전 DR.

---

## 4. 신뢰성 (NFR-RELY)

### 4.1. 발송 정확성

| NFR-ID | 항목 | 목표 | 출처 |
|---|---|---|---|
| NFR-RELY-001 | 발송 테이블 적재 성공률 (정합성 통과 건 기준) | ≥ 99.9% | PRD §5.3 |
| NFR-RELY-002 | 인코딩 변환 (UTF-8→EUC-KR) 무손실률 | 100% (변환 실패는 적재 차단 + ENCODING_FAILED 응답) | RQ-ARCH-005, FS §10.5 |
| NFR-RELY-003 | 발송 ID 중복 발급률 | 0% (ULID 단일성 보장) | FS §7.2 |
| NFR-RELY-004 | 잔액 차감 일관성 (사전 평가 ↔ 적재 ↔ 차감) | 100% (트랜잭션 + 사전 평가 + 결과 응답 합치) | RQ-PAY-006·011 |

### 4.2. 재시도·멱등성

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-RELY-101 | 발송 API 멱등키 (Idempotency-Key) | 헤더로 24시간 중복 요청 차단. 같은 키 + 같은 페이로드 → 기존 send_id 반환 |
| NFR-RELY-102 | 발송 테이블 적재 재시도 | 일시 실패 시 지수 백오프 3회 (1s/3s/9s), 실패 시 SEND_TABLE_INSERT_FAILED |
| NFR-RELY-103 | 외부 인입 (Webhook 콜백) 재시도 | 회원 엔드포인트가 5xx 응답 시 5회 (1m/5m/30m/2h/12h) |
| NFR-RELY-104 | KISA 등록 자동 연계 재시도 | 외부 API 일시 장애 시 큐 적재 후 운영자 알림 (NFR-OPS-302) |

### 4.3. 결제 / 잔액 신뢰성

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-RELY-201 | 잔액 사전 평가 정확도 | 100% — 차감 후 잔액이 음수가 되는 경우 절대 적재하지 않음 |
| NFR-RELY-202 | 부분 발송 결과 응답 일관성 | 적재 성공 N건 + 거부 M건 합 = 요청 건수 (RQ-PAY-011) |
| NFR-RELY-203 | 자동충전 결제 실패율 | ≤ 1% | PRD §5.3, RQ-PAY-108 |
| NFR-RELY-204 | 구독 포인트 차감 우선순위 위반률 | 0% (RQ-PAY-407 — 포인트 → 캐시 순서) |
| NFR-RELY-205 | PG 결제 응답 미수신 시 정책 | 30초 타임아웃 + 결제 상태 비동기 polling, 사용자 잔액 변동 보류 |

### 4.4. 자동 승인 정확도

| NFR-ID | 항목 | 목표 |
|---|---|---|
| NFR-RELY-301 | 운영 키 자동 승인 false-positive | ≤ 1% (실제 어뷰저가 자동 승인되는 비율) |
| NFR-RELY-302 | 운영 키 자동 승인 false-negative | ≤ 10% (정상 회원이 보류 큐로 가는 비율) |
| NFR-RELY-303 | 자동 승인 비율 | ≥ 70% | PRD §5.1, RQ-KEY-009 |

---

## 5. 확장성 (NFR-SCALE)

### 5.1. 수평 확장

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-SCALE-001 | API 서버 stateless | 세션 상태는 Redis 또는 JWT만, 인스턴스 임의 추가/제거 가능 |
| NFR-SCALE-002 | 발송 테이블 INSERT 처리 | 파티션 키 = `created_at` 일별, 1일 1억 건까지 무중단 확장 |
| NFR-SCALE-003 | DB 읽기 부하 | Read Replica 분리, 조회 계열 (`history:read` 등)은 replica 사용 |
| NFR-SCALE-004 | 캐시 (Redis) | 잔액·테스트 한도·세션 키. 클러스터 모드, 단일 노드 장애 무중단 |

### 5.2. 글로벌 확장 (RQ-ARCH-006)

> MVP 미포함이지만 데이터 모델·인증·결제 흐름은 확장 가능하도록 설계.

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-SCALE-101 | 시간대 처리 | DB 저장은 UTC, 응답은 회원 timezone 변환 (기본 KST) |
| NFR-SCALE-102 | 통화 (Currency) 분리 | `cash_balance.currency` 컬럼 확보, MVP는 KRW 단일 |
| NFR-SCALE-103 | 발신번호 국가 코드 | E.164 저장, 국내/해외 검증 분기 가능 |
| NFR-SCALE-104 | 다국어 응답 | 에러 메시지·운영 알림은 i18n 키 기반, MVP는 ko-KR 단일 |
| NFR-SCALE-105 | 결제 PG 추상화 | 국내 PG / 글로벌 PG 어댑터 패턴, MVP는 국내 PG만 |

### 5.3. 단계별 용량 계획

| 단계 | 동시 회원 | 일 발송량 | 캐시 충전 일 합계 | 구간 정책 |
|---|---|---|---|---|
| MVP (출시 후 3개월) | 200 MAU | ≤ 100만 건 | ≤ 1억 원 | 단일 IDC + 노드·랙 이중화 (§3.5) + 오프사이트 콜드 백업 |
| 성장기 (출시 후 1년) | 2,000 MAU | ≤ 1,000만 건 | ≤ 10억 원 | 단일 IDC 풀 이중화 + Read Replica + 별도 IDC 콜드 백업 |
| 성숙기 (3년) | 10,000 MAU | ≤ 1억 건 | ≤ 100억 원 | 다중 IDC DR 검토 (NFR-AVAIL-204 재논의) |

---

## 6. 보안 (NFR-SEC)

### 6.1. 통신 보안

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-SEC-001 | 모든 외부 인입 통신 암호화 | TLS 1.2 이상 강제, TLS 1.0/1.1 거부 | FS §12 |
| NFR-SEC-002 | HSTS 적용 | `max-age=31536000; includeSubDomains; preload` | — |
| NFR-SEC-003 | 인증서 갱신 자동화 | 만료 30일 전 자동 갱신·알림 | — |
| NFR-SEC-004 | 내부 서비스 간 통신 | mTLS 또는 VPC 격리 | — |

### 6.2. 인증·인가

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-SEC-101 | 회원 비밀번호 해시 | bcrypt cost ≥ 12 또는 Argon2id |
| NFR-SEC-102 | 비밀번호 정책 | 10자 이상, 영문·숫자·특수문자 중 2종 이상 (FS §11.1) |
| NFR-SEC-103 | 로그인 실패 잠금 | 5회 실패 → 15분 잠금 + CAPTCHA 요구 |
| NFR-SEC-104 | 운영자 2차 인증 강제 | TOTP/SMS 강제, 비활성 옵션 없음 (RQ-ADMIN-002) |
| NFR-SEC-105 | 운영자 신뢰 IP 화이트리스트 | 어드민 도메인 접근 시 IP 검증 강제 (RQ-ADMIN-003) |
| NFR-SEC-106 | 세션 타임아웃 | 회원 12시간 / 운영자 1시간 / 체험 30분 |
| NFR-SEC-107 | API Key 길이·엔트로피 | ≥ 256bit, prefix `WSC-{ENV}-` + base62 |
| NFR-SEC-108 | API Key 저장 (서버) | 해시 또는 KMS 암호화, 평문 저장 금지 |

### 6.3. 키 관리 (회원 측)

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-SEC-201 | CLI API Key 저장 | OS 키체인 (macOS Keychain / Windows Credential Manager / Linux Secret Service) 또는 chmod 600 설정 파일 | RQ-CLI-403 |
| NFR-SEC-202 | API Key 응답 시 마스킹 | 발급 직후 1회만 풀텍스트 노출, 이후 조회는 prefix + `****` + suffix 4자리 |
| NFR-SEC-203 | 키 재발급 시 기존 키 즉시 무효화 | RQ-KEY-007, 재발급 후 1초 내 모든 노드 캐시 무효화 |
| NFR-SEC-204 | 키 스코프 미충족 호출 차단 | `SCOPE_NOT_GRANTED` 응답 (FS §10.1), 발송 테이블 적재 0건 |
| NFR-SEC-205 | MCP 보안 민감 액션 비노출 | 키 발급/폐기, 발신번호 등록/삭제, 결제, 스코프 변경 — MCP 도구로 노출 X (RQ-MCP-014) |

### 6.4. 입력 검증·웹 보안 (OWASP Top 10)

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-SEC-301 | SQL Injection 방어 | 모든 쿼리 PreparedStatement 또는 ORM, 동적 쿼리 금지 |
| NFR-SEC-302 | XSS 방어 | 응답 HTML 인코딩, CSP 헤더 (`default-src 'self'`) |
| NFR-SEC-303 | CSRF 방어 | 상태 변경 API는 SameSite=Strict 쿠키 또는 명시 헤더 |
| NFR-SEC-304 | 파일 업로드 검증 | MIME + 매직바이트 검사, 사업자 등록증 PDF/JPG ≤ 10MB (FS §11.2) |
| NFR-SEC-305 | MMS 이미지 검증 | JPEG/PNG ≤ 300KB / 720x1280 (FS §11.3) |
| NFR-SEC-306 | Rate Limiting | IP당 60 req/min, 키당 일일 한도(RQ-KEY-104) |
| NFR-SEC-307 | 체험 모드 어뷰징 차단 | 동일 IP/디바이스 단기 다수 발급 차단 (RQ-TRIAL-016) |

### 6.5. 감사 로그

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-SEC-401 | 운영자 모든 행위 기록 | 누가/언제/무엇을/이전값/이후값/IP 5필드 필수 | RQ-ADMIN-009 |
| NFR-SEC-402 | 감사 로그 보존 | 5년 (`NFR-DATA-302`) | FS §12 |
| NFR-SEC-403 | 감사 로그 무결성 | append-only, 해시 체인 또는 WORM 스토리지 |
| NFR-SEC-404 | 감사 로그 조회 권한 | SUPER_ADMIN 만 조회 가능 (RQ-ADMIN-807) |
| NFR-SEC-405 | 회원 행위 로그 | 키 발급/폐기, 발신번호 등록/삭제, 결제, 스코프 변경 — 회원 본인 조회 가능 (RQ-KEY-204) |

### 6.6. 침해 대응

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-SEC-501 | API Key 노출 의심 시 자동 폐기 | 운영자 강제 폐기 (RQ-ADMIN-207) + 회원 즉시 알림 |
| NFR-SEC-502 | 이상 패턴 자동 차단 false-negative | ≤ 5% (PRD §5.3, RQ-SEC-006) |
| NFR-SEC-503 | 침해 사고 통지 | 24시간 내 영향 회원 통지 + 72시간 내 KISA 신고 (정보통신망법) |

---

## 7. 개인정보 (NFR-PRIV)

### 7.1. 수집·이용 동의

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-PRIV-001 | 가입 시 동의 분리 | 필수/선택 약관 분리 표시 (RQ-AUTH-004) |
| NFR-PRIV-002 | 본인 인증 정보 | 본인 인증 기관에서 받은 휴대폰·이름은 변경 불가 영역으로 보관 |
| NFR-PRIV-003 | 마케팅 수신 동의 | 별도 옵트인 + 수신 거부 1-click 해제 |

### 7.2. 보관·마스킹

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-PRIV-101 | 수신번호 마스킹 (운영자 화면) | `010-****-1234` 기본, 사유 입력 후 풀 노출 (사유 + 운영자 ID + 시각 감사 로그) | — |
| NFR-PRIV-102 | API 요청·응답 로그 마스킹 | 본문·수신번호 끝 4자리만 보존 | FS §12 |
| NFR-PRIV-103 | 비밀번호 평문 저장 금지 | bcrypt/Argon2 해시만 보관 | NFR-SEC-101 |
| NFR-PRIV-104 | 결제 카드번호 저장 금지 | PG 토큰만 보관, 카드번호·CVC 본 서비스 미보관 |

### 7.3. 보존·파기

| NFR-ID | 데이터 | 보존 기간 | 파기 시점 | 출처 |
|---|---|---|---|---|
| NFR-PRIV-201 | 회원 가입·프로필 정보 | 회원 활성 + 탈퇴 후 30일 | 탈퇴 30일 후 자동 파기 | — |
| NFR-PRIV-202 | 발송 본문 (payload) | 1년 | 1년 경과 후 본문만 NULL, 메타는 잔존 | FS §12 |
| NFR-PRIV-203 | API 요청·응답 로그 | 30일 | 30일 후 자동 파기 | FS §12 |
| NFR-PRIV-204 | 결제·세금 증빙 | 5년 (전자상거래법) | 자동 파기 X — 운영자 수동 검토 후 | NFR-COMP-201 |
| NFR-PRIV-205 | 운영자 감사 로그 | 5년 | NFR-SEC-402 | RQ-ADMIN-807 |
| NFR-PRIV-206 | 체험 모드 데이터 | 세션 만료 즉시 폐기 | RQ-TRIAL-012·014 | RQ-TRIAL-008 |

### 7.4. 위탁·국외 이전

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-PRIV-301 | 처리 위탁 공시 | PG·본인인증·KISA 사전 등록·외부 발송 시스템 등 위탁 처리자 약관 명시 |
| NFR-PRIV-302 | 국외 이전 | MVP는 국내 리전(서울/부산)만, 국외 이전 없음 |
| NFR-PRIV-303 | 외주 고객센터 접근 | RQ-ADMIN-707 위임 시 1:1 문의 답변·FAQ 권한만, 회원 상세·결제 권한 없음 |

---

## 8. 컴플라이언스 / 법규 (NFR-COMP)

### 8.1. 메시지 발송 관련 법규

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-COMP-001 | KISA 발신번호 사전 등록 강제 | KISA 미등록 발신번호 발송 차단 | RQ-CALLBACK-201 |
| NFR-COMP-002 | 광고 의무 표기 검사 | (광고) 표기 + 무료 거부 번호 검사 | RQ-SEC-002 |
| NFR-COMP-003 | 야간 광고 발송 제한 (21:00~08:00) | 광고성 메시지는 시간 차단 검사 |
| NFR-COMP-004 | 개인정보보호법 준수 | 수집 최소화, 동의 분리, 보존 기간 준수 (NFR-PRIV-*) |
| NFR-COMP-005 | 정보통신망법 준수 | 침해 사고 시 24/72시간 신고 (NFR-SEC-503) |

### 8.2. 결제·세무 관련 법규

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-COMP-101 | 전자세금계산서 국세청 전송 | 사용 금액 기준 발급 후 국세청 전송 (RQ-PAY-505, RQ-ADMIN-504) |
| NFR-COMP-102 | 현금영수증 의무 발행 | 사용 금액 기준 (RQ-PAY-504) |
| NFR-COMP-103 | 결제대행(PG) 약관 준수 | 카드번호 비저장, 정기결제 30일 제한 (RQ-PAY-105) |
| NFR-COMP-104 | 청약 철회·쿨링 오프 | 구독 1일 (RQ-PAY-408 — 사용분 차감 후 잔액 환불) |

### 8.3. 회계 보존

| NFR-ID | 항목 | 보존 기간 |
|---|---|---|
| NFR-COMP-201 | 결제·환불·세금계산서 거래 기록 | 5년 (전자상거래법) |
| NFR-COMP-202 | 정산 보고서 | 5년 (RQ-ADMIN-508) |

---

## 9. 데이터 (NFR-DATA)

### 9.1. 인코딩

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-DATA-001 | 외부 인입 인코딩 | UTF-8 강제 | RQ-ARCH-004 |
| NFR-DATA-002 | 발송 테이블 적재 인코딩 | EUC-KR (외부 발송 시스템 규격) | RQ-ARCH-005, RQ-SEND-305 |
| NFR-DATA-003 | DB 기본 인코딩 | UTF-8MB4 (이모지 지원) |
| NFR-DATA-004 | CSV 업로드 | UTF-8 (BOM 허용), 헤더 `destaddr,vars` | FS §11.3 |
| NFR-DATA-005 | 인코딩 변환 실패 처리 | 적재 차단 + `ENCODING_FAILED` (HTTP 500), 재시도 1회 | FS §10.5 |

### 9.2. 보존 기간

| NFR-ID | 데이터 | 보존 기간 | 출처 |
|---|---|---|---|
| NFR-DATA-101 | API 요청·응답 (마스킹) | 30일 | FS §12 |
| NFR-DATA-102 | 발송 이력 (메타·결과) | 1년 (회원 조회 가능) | FS §12 |
| NFR-DATA-103 | 발송 본문 (payload) | 1년 (NFR-PRIV-202) |
| NFR-DATA-104 | 회원 정보 | 활성 + 탈퇴 30일 (NFR-PRIV-201) |
| NFR-DATA-105 | 결제·환불·세금 거래 | 5년 (NFR-COMP-201) |
| NFR-DATA-106 | 운영자 감사 로그 | 5년 (RQ-ADMIN-807) |
| NFR-DATA-107 | 체험 세션 데이터 | 세션 만료 즉시 폐기 (RQ-TRIAL-012) |

### 9.3. 백업·복구

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-DATA-201 | DB 정기 백업 | 일 1회 풀백업 + 5분 단위 binlog 증분 |
| NFR-DATA-202 | 백업 보관 | 7일 (Hot) + 90일 (Cold) + 5년 (Archive — 회계·감사) |
| NFR-DATA-203 | 백업 암호화 | 저장 시 AES-256, 키 분리 보관 (KMS) |
| NFR-DATA-204 | 백업 복구 리허설 | 분기 1회, RTO/RPO 검증 |

### 9.4. 데이터 격리

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-DATA-301 | 체험 모드 격리 | 운영 DB와 물리/논리 분리, 체험 행위가 운영 데이터에 영향 없음 | RQ-TRIAL-008 |
| NFR-DATA-302 | 회원사 간 격리 | 모든 조회 쿼리는 `account_id` 또는 `company_id` 필터 강제, 누락 시 컴파일 오류 |
| NFR-DATA-303 | 운영자·회원 도메인 분리 | 별도 도메인(`admin.wisecan.com` vs `wisecan.com`), 쿠키 도메인 분리 |
| NFR-DATA-304 | 회원 노출 추상화 | 중계사·라우팅·내부 채널 ID는 응답·UI·로그·이력 어디에도 노출 X | RQ-SEND-308 |

---

## 10. 운영성 (NFR-OPS)

### 10.1. 로깅

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-OPS-001 | 구조화 로그 (JSON) | 모든 서버 로그는 JSON 라인, `trace_id`/`account_id`/`api_key_id`/`send_id` 표준 필드 |
| NFR-OPS-002 | 로그 레벨 분리 | TRACE/DEBUG/INFO/WARN/ERROR/FATAL — 운영 환경 기본 INFO |
| NFR-OPS-003 | 분산 추적 | OpenTelemetry trace_id 전파, SDK→API→DB→외부 연동까지 |
| NFR-OPS-004 | PII 마스킹 강제 | 수신번호·본문·카드번호 자동 마스킹 (NFR-PRIV-102) |

### 10.2. 모니터링·메트릭

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-OPS-101 | 메트릭 수집 | Prometheus 표준 — `http_*`, `send_*`, `auth_*`, `payment_*`, `kisa_*` |
| NFR-OPS-102 | 대시보드 | Grafana — API SLA / 발송 적재율 / 잔액 / 어뷰징 / 외부 의존성 가용성 |
| NFR-OPS-103 | 골든 시그널 | Latency / Traffic / Errors / Saturation 4종 필수 |
| NFR-OPS-104 | 비즈니스 메트릭 | MAU, ARPU, 체험→가입 전환율, 자동 승인 비율 (PRD §5) |

### 10.3. 알림

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-OPS-201 | 알림 채널 | Slack(`#ops-alert`), 이메일, 운영자 SMS |
| NFR-OPS-202 | Severity 분류 | P1(즉시) / P2(1시간) / P3(영업시간) |
| NFR-OPS-203 | P1 임계 — 발송 API SLA 위반 | P95 1초 초과 5분 연속 → P1 |
| NFR-OPS-204 | P1 임계 — 적재 성공률 | < 99% 5분 연속 → P1 |
| NFR-OPS-205 | P2 임계 — 자동충전 실패 | RQ-ADMIN-509 알림 + 1시간 내 운영자 확인 |
| NFR-OPS-206 | P2 임계 — KISA 등록 실패 | NFR-RELY-104 fallback 큐 적재 + 운영자 알림 |
| NFR-OPS-207 | P3 임계 — 이상 패턴 | RQ-ADMIN-302 운영 시간 내 검토 |

### 10.4. 배포·롤백

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-OPS-301 | 무중단 배포 | Blue/Green 또는 Rolling, 배포 중 SLA 영향 0 |
| NFR-OPS-302 | DB 마이그레이션 | Backward-compatible 2단계 (Add → Backfill → Switch → Drop) |
| NFR-OPS-303 | 롤백 시간 | ≤ 5분 (이전 버전 트래픽 100% 복귀) |
| NFR-OPS-304 | 카나리 | 5% → 25% → 50% → 100% (각 단계 ≥ 30분 관찰) |
| NFR-OPS-305 | CLI 자동 업데이트 | RQ-CLI-401·402, stable/beta 채널 분리 (RQ-ADMIN-608) |

### 10.5. 환경 분리

| NFR-ID | 환경 | 정책 |
|---|---|---|
| NFR-OPS-401 | 프로덕션 | 외부 발송 시스템 실 송출, 실 결제 |
| NFR-OPS-402 | 스테이징 | 운영 동일 구성, 결제 PG 테스트 모드, 외부 발송 mock |
| NFR-OPS-403 | 개발 | 로컬 docker-compose, H2/MySQL, mock |
| NFR-OPS-404 | 환경 간 데이터 이동 금지 | 프로덕션 → 스테이징 복제 시 PII 익명화 필수 |

### 10.6. 외부 의존성 모니터링

| NFR-ID | 의존성 | 모니터링 항목 |
|---|---|---|
| NFR-OPS-501 | KISA 사전 등록 API | 응답 시간·실패율 |
| NFR-OPS-502 | 외부 발송 시스템 (테이블 polling) | 적재 → 송출 lag, 폴링 정지 (RQ-ADMIN-805) |
| NFR-OPS-503 | PG (결제) | 결제 성공률·평균 응답 시간 |
| NFR-OPS-504 | 카카오·RCS 중계사 | 외부 시스템 책임이지만 종합 발송 성공률은 본 서비스 대시보드에 표시 |

---

## 11. 사용성 (NFR-USE)

### 11.1. UI 응답·인터랙션

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-USE-001 | 페이지 첫 페인트 (LCP) | ≤ 2.0초 (P75) |
| NFR-USE-002 | 인터랙션 응답 (INP) | ≤ 200ms |
| NFR-USE-003 | 누적 레이아웃 변동 (CLS) | ≤ 0.1 |
| NFR-USE-004 | 발송 버튼 클릭 → 응답 표시 | ≤ 1.5초 (loading 상태 표시) |
| NFR-USE-005 | 폼 검증 피드백 | 입력 후 즉시 (≤ 100ms) inline 메시지 |

### 11.2. 접근성 (Accessibility)

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-USE-101 | WCAG 2.1 준수 수준 | AA (한국 웹접근성 인증 마크 대응 가능 수준) |
| NFR-USE-102 | 키보드 내비게이션 | 모든 인터랙션 키보드만으로 가능 |
| NFR-USE-103 | 색 대비 | 본문 ≥ 4.5:1, 큰 글씨 ≥ 3.0:1 |
| NFR-USE-104 | 스크린 리더 라벨 | 모든 인터랙션 요소 ARIA 라벨 또는 텍스트 라벨 |
| NFR-USE-105 | 폼 에러 안내 | 시각적 + 텍스트 + ARIA `aria-invalid` 동시 |

### 11.3. 다국어 / 로케일

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-USE-201 | 1차 언어 | 한국어 (ko-KR) |
| NFR-USE-202 | 시간대 표시 | 기본 KST, 회원 설정 가능 (NFR-SCALE-101) |
| NFR-USE-203 | 통화 표시 | KRW (`₩` 또는 `원`), 천단위 콤마 |
| NFR-USE-204 | i18n 확장 | 글로벌 확장 시 영어 추가 가능 구조 (NFR-SCALE-104) |

### 11.4. 디바이스·반응형

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-USE-301 | 회원 콘솔 PC | 1280px ~ 풀 |
| NFR-USE-302 | 회원 콘솔 Tablet | 768~1279px (사이드바 접이식) |
| NFR-USE-303 | 회원 콘솔 Mobile | 375~767px (하단 탭바, 발송·이력만 1차 지원) |
| NFR-USE-304 | 어드민 콘솔 | PC 전용 (1280px+), Mobile 미지원 |
| NFR-USE-305 | 체험 모드 | PC + Mobile, Tablet 동등 |

### 11.5. 체험 모드 사용성

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-USE-401 | 체험 모드 진입 시간 | 클릭 후 ≤ 2초 (더미 데이터 사전 적재) | RQ-TRIAL-001 |
| NFR-USE-402 | 체험 모드 워터마크 | 모든 페이지 상시 노출 | RQ-TRIAL-009 |
| NFR-USE-403 | 가입 전환 CTA | 발송 직후·결제 시도·이력 조회 시 노출 | RQ-TRIAL-010 |
| NFR-USE-404 | 온보딩 가이드 | 첫 진입 시 자동 표시, 종료 후 재호출 가능 | RQ-TRIAL-011 |

---

## 12. 호환성 (NFR-COMPAT)

### 12.1. 브라우저

| NFR-ID | 브라우저 | 지원 버전 |
|---|---|---|
| NFR-COMPAT-001 | Chrome | 최신 2개 메이저 |
| NFR-COMPAT-002 | Edge | 최신 2개 메이저 |
| NFR-COMPAT-003 | Safari | 최신 2개 메이저 (iOS 포함) |
| NFR-COMPAT-004 | Firefox | 최신 2개 메이저 |
| NFR-COMPAT-005 | IE 11 | 미지원 |

### 12.2. CLI / SDK 런타임

| NFR-ID | 항목 | 지원 | 출처 |
|---|---|---|---|
| NFR-COMPAT-101 | CLI macOS | 12 (Monterey) 이상, x86_64 + arm64 | RQ-CLI-001 |
| NFR-COMPAT-102 | CLI Linux | glibc ≥ 2.28 (Ubuntu 20.04+, Debian 11+, RHEL 8+) | RQ-CLI-001 |
| NFR-COMPAT-103 | CLI Windows | 10 1809 이상, x86_64 (winget 배포) | RQ-CLI-001 |
| NFR-COMPAT-104 | Python SDK | 3.9 이상 | RQ-SDK-001 |
| NFR-COMPAT-105 | MCP 클라이언트 | MCP 표준 프로토콜 준수 클라이언트 (Claude Desktop, Cursor 등) | RQ-MCP-004 |

### 12.3. 외부 시스템 인터페이스

| NFR-ID | 항목 | 정책 | 출처 |
|---|---|---|---|
| NFR-COMPAT-201 | 발송 테이블 스키마 변경 | Backward-compatible (컬럼 추가만), 외부 발송 시스템 polling 무중단 | FS §7.2 |
| NFR-COMPAT-202 | KISA API 어댑터 | 외부 변경 시 어댑터 격리, 수동 fallback 절차 보존 | PRD §10 |

---

## 13. 유지보수성 (NFR-MAINT)

### 13.1. 코드 품질

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-MAINT-001 | 백엔드 컨벤션 준수 | `.claude/rules/backend-*.md` |
| NFR-MAINT-002 | 프론트엔드 컨벤션 준수 | `.claude/rules/frontend-*.md` (FSD 아키텍처) |
| NFR-MAINT-003 | 테스트 커버리지 | 도메인·서비스 레이어 단위 ≥ 70%, 핵심 비즈니스(잔액·발송 적재·키 권한) ≥ 90% |
| NFR-MAINT-004 | TDD 적용 | 도메인 / API 변경은 RED → GREEN → REFACTOR 순서 (`.claude/rules/testing.md`) |
| NFR-MAINT-005 | 정적 분석 | ESLint, Spotless/Checkstyle, OWASP Dependency-Check, CI 차단 |

### 13.2. 문서

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-MAINT-101 | API 문서 | OpenAPI 3 자동 생성, 회원 콘솔에 노출 |
| NFR-MAINT-102 | SDK 매뉴얼 | RQ-SDK-002, RQ-ADMIN-605 |
| NFR-MAINT-103 | CLI 매뉴얼 | RQ-CLI-002, RQ-ADMIN-607 |
| NFR-MAINT-104 | MCP 가이드 코퍼스 | RQ-MCP-006, RQ-ADMIN-609 |
| NFR-MAINT-105 | 변경 이력 | SRS·FS·NFR 모두 § 변경 이력 표 운영 |

### 13.3. CI/CD

| NFR-ID | 항목 | 정책 |
|---|---|---|
| NFR-MAINT-201 | PR 자동 검증 | 빌드 + 테스트 + 정적 분석 + 보안 스캔 통과 시에만 머지 |
| NFR-MAINT-202 | main 보호 | 직접 push 금지, PR + 1인 이상 리뷰 + CI 통과 |
| NFR-MAINT-203 | 시크릿 관리 | KMS / Vault, 코드 내 평문 금지, pre-commit hook 차단 |

---

## 14. NFR ↔ 기능 요구사항 매핑 (역참조)

> 본 NFR을 강제하는 기능 요구사항(또는 본 NFR이 보강하는 기능 요구사항). 이 매핑이 깨지면 SRS·NFR 중 한쪽이 갱신되어야 한다.

| NFR 영역 | 핵심 RQ 참조 |
|---|---|
| 가용성·성능 | PRD §5.3 (SLA 5종) |
| 인코딩 | RQ-ARCH-004·005, RQ-SEND-305 |
| 데이터 격리 | RQ-TRIAL-008, RQ-SEND-308 |
| 회원 노출 추상화 | RQ-SEND-308, RQ-ADMIN-401·402 |
| 보안 / 감사 | RQ-ADMIN-002·003·009·807, RQ-MCP-014, RQ-CLI-403 |
| 잔액 신뢰성 | RQ-PAY-006·008·009·010·011 |
| 자동 승인 정확도 | RQ-KEY-009·010·011, RQ-ADMIN-603·611 |
| 침해 대응 | RQ-SEC-006, RQ-ADMIN-207 |
| 운영자 알림 | RQ-ADMIN-302·509, RQ-PAY-108 |
| 외부 의존성 모니터링 | RQ-ADMIN-805 (외부 발송 에이전트 상태) |
| 체험 모드 어뷰징·격리 | RQ-TRIAL-008·012·014·016 |
| 다국어·글로벌 확장 | RQ-ARCH-006 |

---

## 15. 측정·검증 계획 (개략)

> 각 NFR의 충족 여부를 어떻게 증명할 것인가를 한 줄씩 단언한다. 세부 도구·임계값은 운영 도입 단계에서 확정.

| NFR 영역 | 검증 방식 |
|---|---|
| 성능 | k6 부하 테스트 + 운영 Prometheus P95/P99 트렌드 |
| 가용성 | 외부 모니터링(StatusCake/Pingdom) + 내부 헬스체크 합산 |
| 신뢰성 (적재 성공률) | 운영 메트릭 `send_table_insert_success_total / total` |
| 보안 | 분기 1회 OWASP ZAP 스캔 + 연 1회 외부 모의해킹 |
| 개인정보 | 분기 1회 마스킹 샘플링 점검 + 보존 기간 자동 파기 잡 로그 |
| 컴플라이언스 | KISA·국세청·PG 약관 변경 점검 (분기 1회) |
| 데이터 | 백업 복구 리허설 (분기 1회) |
| 운영 | P1 알림 to-resolution MTTR 측정 + 사후 회고 |
| 사용성 | Lighthouse CI + 분기 1회 사용성 테스트 |
| 호환성 | 매 릴리즈 시 BrowserStack 매트릭스 |
| 유지보수 | CI 게이트(테스트 커버리지·정적 분석) 통과 강제 |

---

## 16. 가정 / 미확정 사항

### 16.1. 가정

- 외부 발송 시스템(통신사·중계사·송출 에이전트)의 가용성·SLA는 별도 합의되며, 본 서비스 SLA에서 제외된다.
- KISA 사전 등록 API는 회원의 발신번호 등록 시 자동 연계 가능한 형태로 제공된다 (PRD §9.1).
- 1차 인프라는 국내 단일 리전(서울 또는 부산), 다중 AZ.
- PG 사·본인인증 사·SMS 인증 게이트웨이는 운영자가 사전 계약 확보.

### 16.2. 1차 출시 전 결정 필요

| 안건 | 영향 NFR | 결정 시점 |
|---|---|---|
| 다중 IDC DR 도입 | NFR-AVAIL-204 (MVP는 단일 IDC + 이중화 §3.5 확정) | 성숙기 진입 시점 |

### 16.3. 결정 완료 항목

| 결정 일자 | 안건 | 결정 | 영향 NFR |
|---|---|---|---|
| 2026-04-30 | 인프라 구성 — 자체 서버 + 단일 IDC 이중화 | **MVP 확정**. 클라우드 멀티 리전 미적용. 노드·랙·DB·캐시·네트워크·전원 이중화 + 오프사이트 콜드 백업 (§3.5) | NFR-AVAIL-201~205, NFR-AVAIL-301~309 |
| 2026-04-30 | WCAG 2.1 접근성 등급 | **AA 확정**. AAA는 비대상 (민간 B2B SaaS, 디자인 자유도 우선). 회원·어드민·체험 콘솔 모두 동일 적용 | NFR-USE-101 |
| 2026-04-30 | §B 자동충전 도입 | **1차 도입 확정**. PG 정기결제 + 30일 제한 + 잔액 임계치 자동결제 + 운영자 모니터링 (RQ-ADMIN-509) | NFR-RELY-203 (자동충전 결제 실패율 ≤ 1%), NFR-OPS-205 (P2 알림) |
| 2026-04-30 | §C 구독 도입 | **1차 도입 확정**. 등급별 정액 + 포인트 차감 (포인트 → 캐시 우선순위) + 쿨링오프 1일 (사용분 차감 후 잔액 환불) | NFR-COMP-104 (1일 쿨링오프), NFR-RELY-204 (포인트 차감 우선순위 위반률 0%) |
| 2026-04-30 | §E 후불 도입 | **1차 도입 확정**. 신용 검증 + 보증보험 등록 회원 한정. 청구·연체 운영 (RQ-ADMIN-510) | NFR-RELY-201 (잔액 사전 평가 정확도), NFR-COMP-201 (5년 보존) |

---

## 17. 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-04-30 | 오민성 (v1) | FS §12 (비기능 요약 표) + PRD §5.3 (SLA) + 흩어진 RQ-ARCH·SEC·TRIAL의 비기능 측면을 통합해 NFR.md 1차 초안 작성. 12개 분류(성능·가용성·신뢰성·확장성·보안·개인정보·컴플라이언스·데이터·운영·사용성·호환성·유지보수성) 약 200건. NFR ↔ RQ 역참조표(§14)·검증 계획(§15)·가정(§16) 포함. |
| 2026-04-30 | 오민성 (v2) | **인프라 구성 / WCAG 2건 결정 반영**. (1) **자체 서버 + 단일 IDC 이중화 구성 확정** — 클라우드 멀티 리전 DR 미적용. §3.3 RPO/RTO 표의 AZ→랙·리전→IDC 용어 보정 (NFR-AVAIL-201~205), §3.5 인프라 구성 절 신규 추가 (NFR-AVAIL-301~309: LB 페일오버·DB Standby·Read Replica·Redis 클러스터·RAID·듀얼 NIC·UPS·오프사이트 콜드 백업·헬스체크), §5.3 단계별 용량 계획을 IDC 기반으로 갱신. 다중 IDC DR은 성숙기 트리거 항목으로 §16.2에 잔존. (2) **WCAG 2.1 AA 확정** — NFR-USE-101 결정 완료, AAA 비대상. (3) §16.2 미확정 표에서 두 항목 제거, **§16.3 결정 완료 항목 신규 추가** — 인프라 구성 + WCAG 2건 명시. NFR-* ID 추가 외 변경 없음 — 모든 인용처 그대로 호환. |
| 2026-04-30 | 오민성 (v3) | **§B/§C/§E 모두 1차 도입 확정**. (1) §16.2 미확정 결정 표에서 §B 자동충전 / §C 구독 / §E 후불 3행 모두 제거 — 잔존은 다중 IDC DR 1건뿐. (2) §16.3 결정 완료 항목에 자동충전·구독·후불 3건 추가, 영향 NFR(NFR-RELY-203·204·201, NFR-COMP-104·201, NFR-OPS-205) 명시. (3) **callback/destaddr 명명 통일** 반영 — RQ-CALLER → RQ-CALLBACK 일괄 치환, NFR-DATA-004 CSV 헤더 `to_number` → `destaddr`, NFR-AVAIL-006 `/trial` → `/try`. (4) NFR-PERF-105 회원 콘솔 대시보드 경로 `/my` → `/dashboard` 갱신. NFR-* ID 변경 없음. |

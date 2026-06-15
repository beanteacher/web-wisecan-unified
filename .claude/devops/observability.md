# 관측 (Observability)

> 메트릭·알림·대시보드의 **정본은 `../../pm/06_OBSERVABILITY.md`**. 본 문서는 실제 백엔드 설정과 정본을 잇는 운영 관점 요약이다.

---

## 1. 현재 실재 (코드 기준)

- **메트릭 노출**: actuator + micrometer-registry-prometheus.
  - `application.yml`: `management.endpoints.web.exposure.include: health, info, prometheus`.
  - 엔드포인트: `GET /actuator/health`, `/actuator/info`, `/actuator/prometheus`.
- **헬스체크**: `/actuator/health` → `{"status":"UP"}`. 로드밸런서/IDC 헬스 프로브로 사용.
- **로깅**: logback rolling(`logs/wisecan-unified.log`, 10MB/30일), 콘솔 `%clr` 패턴(Windows 출력 호환). 프로필별 레벨(local DEBUG/TRACE, product 축소).

---

## 2. KPI ↔ 메트릭 (정본 연계)

`01_PRD §K3` / `04_PROJECT_PLAN §8.3` 운영 게이트의 측정 출처. **알림 룰·메트릭 이름의 정본은 `06_OBSERVABILITY §2·§3`** 이며, 백엔드는 그 정의에 맞춰 메트릭을 노출한다.

| 항목 | 목표 | 메트릭(정본) | 알림(정본) |
|------|------|--------------|------------|
| 발송 API 가용성 | ≥ 99.5% | `send_api_5xx_total` / `send_api_total` | `SendApiAvailabilityBelow995` |
| 발송 API P95 | ≤ 1,000ms | `send_api_duration_seconds` | `SendApiP95Over1s` |
| 발송 테이블 적재 성공률 | ≥ 99.9% | `send_table_insert_total{result}` | `SendTableInsertSuccessBelow999` (P0) |
| 자동충전 실패율 | ≤ 1% | `autocharge_attempt_total{result}` | `AutochargeFailureRateOver1Percent` |
| 1:1 문의 처리 | ≤ 24h | `cs_ticket_open_age_hours` | `CsTicket24hBreach` |
| 사업자 전환 심사 | ≤ 24h | `admin_review_age_seconds{type="business"}` | `BusinessReviewSla24hBreach` |

> 위 메트릭은 발송·결제·관리자 도메인이 구현되며 추가된다. 커스텀 메트릭은 `MeterRegistry` 주입 후 `Counter`/`Timer`/`Gauge` 로 등록하고, **이름·라벨을 `06_OBSERVABILITY` 정의와 1:1로 맞춘다**(불일치 시 알림 룰이 동작하지 않음).

---

## 3. 권장 스택 (M0 셋업 대상)

`04_PROJECT_PLAN §M0 / W-003` — 자체 IDC 1차 셋업에서 구축:

```
[ Spring Boot /actuator/prometheus ]
            │ scrape
            ▼
       [ Prometheus ] ── rules ──▶ [ Alertmanager ] ──▶ 알림 채널
            │ query
            ▼
        [ Grafana ]  (대시보드 임베드: 06_OBSERVABILITY §임베드)
```

- Prometheus scrape 대상에 백엔드 `/actuator/prometheus` 등록.
- 알림 룰·Grafana 임베드·운영자 권한 매핑 정의는 모두 `06_OBSERVABILITY` 정본.
- 단일 IDC 가용성 한계는 리스크 `R-11`(노드·랙·전원·네트워크 4중 이중화 + 오프사이트 백업).

---

## 4. 운영 점검 체크리스트

```
[ ] /actuator/health UP, /actuator/prometheus 메트릭 노출 확인
[ ] 신규 도메인의 KPI 메트릭 이름이 06_OBSERVABILITY 정의와 일치
[ ] 알림 룰이 실제 메트릭에 바인딩되는지(레이블 포함) 검증
[ ] 로그에 민감정보(토큰/키/비밀번호) 미노출
[ ] product 프로필에서 로그 레벨·SQL 로깅이 과다하지 않음
[ ] 레이트리밋(redis) 동작 — 분당 한도 초과 시 차단 확인
```

> 메트릭/알림을 추가·변경하면 `06_OBSERVABILITY.md`(정본)를 먼저 갱신하고 코드를 맞춘다. 본 문서는 정본을 가리키는 포인터일 뿐, 사실을 중복 보유하지 않는다.

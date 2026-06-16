# GA 릴리즈 체크리스트 — WiseCan W-506

> 작성일 2026-06-16

베타(22주차 내부·초청 고객) 종료 후 GA 전환 시 아래 4개 게이트를 순서대로 통과해야 한다.
게이트 기준 출처: `pm/04_PROJECT_PLAN.md §8`.

---

## Gate 1. 기능 게이트 (§8.1)

| # | 항목 | 확인 방법 | 상태 |
|---|---|---|---|
| 1-1 | M1~M5 전 WBS DoD 충족 | TaskList 전체 completed 확인 | [ ] |
| 1-2 | 핵심 E2E 5종 그린 (가입·발송·결제·이력·OCR 검색) | CI E2E 테스트 결과 | [ ] |
| 1-3 | 보류 항목 비활성 처리 확인 (구독·세금계산서·특수부가·사설 연동) | Feature Flag / 설정 확인 | [ ] |
| 1-4 | 체험 모드 격리 확인 (운영 DB 영향 없음) | 체험 세션 발송 후 운영 DB 조회 | [ ] |
| 1-5 | MCP 비노출 화이트리스트 단위 테스트 통과 | `McpToolEquivalenceTest` green | [ ] |

---

## Gate 2. 품질 게이트 (§8.2)

| # | 항목 | 목표 | 확인 방법 | 상태 |
|---|---|---|---|---|
| 2-1 | 단위 테스트 커버리지 라인 | ≥ 70% | `./gradlew jacocoTestReport` | [ ] |
| 2-2 | 핵심 도메인 커버리지 (dispatch·billing·auth) | ≥ 85% | JaCoCo 리포트 | [ ] |
| 2-3 | 회귀 테스트 | 0 fail | CI 전체 테스트 통과 | [ ] |
| 2-4 | 정적 분석 critical | 0건 | SonarQube / SpotBugs | [ ] |

---

## Gate 3. 운영 게이트 — NFR 통과 (§8.3)

| # | NFR ID | 항목 | 목표 | 측정 메트릭 | 상태 |
|---|---|---|---|---|---|
| 3-1 | NFR-PERF-301 | 발송 API P95 응답 | ≤ 1,000ms | `send_api_duration_seconds` P95 | [ ] |
| 3-2 | NFR-PERF-302 | 발송 테이블 적재 성공률 | ≥ 99.9% | `send_table_insert_total{result}` | [ ] |
| 3-3 | NFR-AVAIL-001 | 발송 API 가용성 | ≥ 99.5% | `send_api_5xx_total` / `send_api_total` | [ ] |
| 3-4 | NFR-RELY-203 | 자동충전 결제 실패율 | ≤ 1% | `autocharge_attempt_total{result}` | [ ] |
| 3-5 | PRD §K3 보조 | 1:1 문의 처리 시간 | ≤ 24h | `cs_ticket_open_age_hours` | [ ] |
| 3-6 | PRD §K1 보조 | 사업자 전환 심사 평균 | ≤ 24h | `admin_review_age_seconds{type="business"}` | [ ] |

**부하 테스트 실행 확인**
- [ ] 시나리오 A (발송 P95) k6 threshold green — `load-test-scenario.md §2`
- [ ] 시나리오 B (적재 성공률) k6 threshold green — `load-test-scenario.md §3`
- [ ] 시나리오 C (복합 가용성) k6 threshold green — `load-test-scenario.md §4`
- [ ] Prometheus 검증 쿼리 결과 스크린샷 첨부 — `load-test-scenario.md §6`

---

## Gate 4. 보안 게이트 (§8.4)

| # | 항목 | 확인 방법 | 상태 |
|---|---|---|---|
| 4-1 | TLS 1.2+ 강제, HSTS 헤더 | curl -I https://api.wisecan.io | [ ] |
| 4-2 | mTLS 내부 통신 | 서비스 간 통신 인증서 확인 | [ ] |
| 4-3 | 운영자 2차 인증 + 신뢰 IP 강제 | 어드민 로그인 시나리오 테스트 | [ ] |
| 4-4 | API Key OS 키체인 저장 확인 | SDK 문서·테스트 확인 | [ ] |
| 4-5 | 감사 로그 append-only + 5년 보존 | AuditLog 엔티티 + DB 정책 확인 | [ ] |
| 4-6 | OWASP Top 10 셀프 점검 5종 | SQLi·XSS·CSRF·파일 업로드·Rate Limit | [ ] |

---

## 베타 → GA 전환 절차

```
1. 베타 기간 종료 (22주차 마지막)
2. Gate 1~4 순서대로 통과 확인
3. 부하 테스트 결과 문서화 (results-a.json, results-b.json, results-c.json)
4. Prometheus 알림 룰 전부 active 상태 확인 (06_OBSERVABILITY §3)
5. Grafana 대시보드 전 패널 정상 렌더 확인
6. 운영자 계정 최소화 (SUPER_ADMIN 2인 이하)
7. GA 선언 — 공개 URL 개방
8. 1주 안정화 모니터링 (Discord 알림 채널 상시 대기)
```

---

## 안정화 모니터링 항목 (GA 후 1주)

| 항목 | 주기 | 담당 |
|---|---|---|
| `SendApiAvailabilityBelow995` 알림 발화 여부 | 실시간 | 운영자 |
| `SendTableInsertSuccessBelow999` 알림 발화 여부 | 실시간 | 운영자 |
| `SendApiP95Over1s` 알림 발화 여부 | 실시간 | 운영자 |
| 일별 가입자 수 / 발송량 추이 | 1일 | PM |
| CS 1:1 문의 24h 이내 처리율 | 1일 | CS |
| 이상 패턴 탐지 건수 | 1일 | 보안 |

# 부하 테스트 시나리오 — WiseCan W-506

> 작성일 2026-06-16

로컬 빌드 실행 금지(SSL 인터셉션 환경). 본 파일은 CI/운영 환경에서 실행하는 시나리오 및 기준 문서다.
실행 도구: [k6](https://k6.io) (권장) 또는 Apache JMeter.

---

## 1. NFR 통과 기준

| ID | 항목 | 목표 | 측정 메트릭 | 알림 룰 |
|---|---|---|---|---|
| NFR-PERF-301 | 발송 API P95 응답 | ≤ 1,000ms | `send_api_duration_seconds` P95 | `SendApiP95Over1s` |
| NFR-PERF-302 | 발송 테이블 적재 성공률 | ≥ 99.9% | `send_table_insert_total{result}` | `SendTableInsertSuccessBelow999` |
| NFR-AVAIL-001 | 발송 API 가용성 | ≥ 99.5% | `send_api_5xx_total` / `send_api_total` | `SendApiAvailabilityBelow995` |
| NFR-RELY-203 | 자동충전 결제 실패율 | ≤ 1% | `autocharge_attempt_total{result}` | `AutochargeFailureRateOver1Percent` |

---

## 2. 시나리오 A — 발송 API 처리량 (NFR-PERF-301)

### 목표
- 동시 사용자 100명이 발송 API를 지속 호출할 때 P95 ≤ 1,000ms 달성
- 5분간 Ramp-up → 10분간 정상 부하 → 2분간 Ramp-down

### k6 스크립트

```javascript
// backend/src/test/load/scenario-a-send-api.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const sendDuration = new Trend('send_api_duration');

export const options = {
  stages: [
    { duration: '2m', target: 50 },   // ramp-up to 50 users
    { duration: '5m', target: 100 },  // ramp-up to 100 users
    { duration: '10m', target: 100 }, // steady state
    { duration: '2m', target: 0 },    // ramp-down
  ],
  thresholds: {
    // NFR-PERF-301: P95 ≤ 1,000ms
    'send_api_duration': ['p(95)<1000'],
    // NFR-AVAIL-001: 에러율 ≤ 0.5%
    'errors': ['rate<0.005'],
    // 기본 HTTP 실패율
    'http_req_failed': ['rate<0.005'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY  = __ENV.API_KEY  || 'test-api-key-stub';

export default function () {
  const payload = JSON.stringify({
    to: '01012345678',
    from: '0212345678',
    content: '부하 테스트 발송 메시지입니다.',
    channel: 'SMS',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': API_KEY,
    },
    timeout: '5s',
  };

  const start = Date.now();
  const res = http.post(`${BASE_URL}/send`, payload, params);
  const duration = Date.now() - start;

  sendDuration.add(duration);

  const ok = check(res, {
    'status is 200 or 202': (r) => r.status === 200 || r.status === 202,
    'response has requestId': (r) => {
      try { return JSON.parse(r.body).requestId != null; } catch { return false; }
    },
  });

  errorRate.add(!ok);
  sleep(0.5);
}
```

---

## 3. 시나리오 B — 발송 테이블 적재 성공률 (NFR-PERF-302)

### 목표
- 1,000건 발송 요청 중 999건 이상 `send_table` INSERT 성공
- Prometheus 메트릭 `send_table_insert_total{result="success"}` / `send_table_insert_total` ≥ 0.999

### k6 스크립트

```javascript
// backend/src/test/load/scenario-b-insert-success.js
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const successCount = new Counter('insert_success');
const failCount    = new Counter('insert_fail');

export const options = {
  vus: 20,
  iterations: 1000,   // 정확히 1,000건
  thresholds: {
    // NFR-PERF-302: 성공률 ≥ 99.9% → 1,000건 중 실패 ≤ 1건
    'insert_fail': ['count<2'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY  = __ENV.API_KEY  || 'test-api-key-stub';

export default function () {
  const payload = JSON.stringify({
    to: `0101234${String(Math.floor(Math.random() * 9000) + 1000)}`,
    from: '0212345678',
    content: `적재 성공률 테스트 #${__ITER}`,
    channel: 'SMS',
  });

  const res = http.post(`${BASE_URL}/send`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': API_KEY,
    },
  });

  if (check(res, { 'inserted': (r) => r.status === 200 || r.status === 202 })) {
    successCount.add(1);
  } else {
    failCount.add(1);
  }
}
```

---

## 4. 시나리오 C — 복합 부하 (가용성 검증)

### 목표
- 발송 + 이력 조회 + 잔액 조회를 혼합한 실사용 패턴으로 15분간 지속
- 가용성 ≥ 99.5% (`send_api_5xx_total` 기준)

```javascript
// backend/src/test/load/scenario-c-mixed.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const fivexxRate = new Rate('5xx_errors');

export const options = {
  stages: [
    { duration: '2m', target: 80 },
    { duration: '15m', target: 80 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    // NFR-AVAIL-001: 5xx 비율 ≤ 0.5%
    '5xx_errors': ['rate<0.005'],
    'http_req_duration': ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY  = __ENV.API_KEY  || 'test-api-key-stub';
const JWT      = __ENV.JWT      || 'test-jwt-token';

const commonHeaders = { 'Content-Type': 'application/json', 'X-API-Key': API_KEY };
const authHeaders   = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${JWT}` };

export default function () {
  const action = Math.random();

  if (action < 0.6) {
    // 60%: 발송 요청
    const res = http.post(`${BASE_URL}/send`, JSON.stringify({
      to: '01012345678', from: '0212345678', content: '복합 부하 발송', channel: 'SMS',
    }), { headers: commonHeaders });
    fivexxRate.add(res.status >= 500);
    check(res, { 'send ok': (r) => r.status < 500 });
  } else if (action < 0.85) {
    // 25%: 발송 이력 조회
    const res = http.get(`${BASE_URL}/send/history?page=0&size=10`, { headers: authHeaders });
    fivexxRate.add(res.status >= 500);
    check(res, { 'history ok': (r) => r.status < 500 });
  } else {
    // 15%: 잔액 조회
    const res = http.get(`${BASE_URL}/balance`, { headers: authHeaders });
    fivexxRate.add(res.status >= 500);
    check(res, { 'balance ok': (r) => r.status < 500 });
  }

  sleep(0.3 + Math.random() * 0.4);
}
```

---

## 5. 실행 방법 (CI/운영 환경)

```bash
# 환경변수 설정
export BASE_URL=https://api.wisecan.io
export API_KEY=<운영-테스트-키>
export JWT=<테스트-JWT>

# 시나리오 A (NFR-PERF-301)
k6 run --out json=results-a.json backend/src/test/load/scenario-a-send-api.js

# 시나리오 B (NFR-PERF-302)
k6 run --out json=results-b.json backend/src/test/load/scenario-b-insert-success.js

# 시나리오 C (복합 가용성)
k6 run --out json=results-c.json backend/src/test/load/scenario-c-mixed.js

# 결과 요약
k6 run --summary-export=summary.json backend/src/test/load/scenario-a-send-api.js
```

---

## 6. Prometheus 검증 쿼리 (부하 테스트 중/후)

```promql
# NFR-PERF-301: 발송 API P95
histogram_quantile(0.95, rate(send_api_duration_seconds_bucket[5m]))

# NFR-PERF-302: 적재 성공률
rate(send_table_insert_total{result="success"}[5m]) / rate(send_table_insert_total[5m])

# NFR-AVAIL-001: 가용성
1 - rate(send_api_5xx_total[5m]) / rate(send_api_total[5m])

# NFR-RELY-203: 자동충전 실패율
rate(autocharge_attempt_total{result="failure"}[5m]) / rate(autocharge_attempt_total[5m])
```

---

## 7. 통과/실패 판정 기준

| 시나리오 | NFR | 통과 조건 | 실패 시 조치 |
|---|---|---|---|
| A | NFR-PERF-301 | k6 `p(95)<1000` threshold green | DB 커넥션 풀 조정, 인덱스 추가 |
| B | NFR-PERF-302 | `insert_fail count<2` (1,000건 기준) | 트랜잭션 재시도 로직 점검 |
| C | NFR-AVAIL-001 | `5xx_errors rate<0.005` | 서킷 브레이커 임계 조정 |

> 부하 테스트는 로컬 실행 불가(SSL 인터셉션 환경) — CI 파이프라인 또는 스테이징 서버에서 실행.

# infra/ — 옵저버빌리티 스택 (W-003)

> 정본: `pm/06_OBSERVABILITY.md` · 빌드/실행 참조: `.claude/devops/observability.md`
> 메트릭 카탈로그·알림 룰·대시보드 정의는 모두 `06_OBSERVABILITY.md` 를 따른다. 본 폴더는 그 정의의 실제 구성 파일이다.

---

## 구성

```
infra/
├── docker-compose.observability.yml   # Prometheus + Alertmanager + Grafana 스택
├── alertmanager/
│   ├── alertmanager.yml               # 라우팅(§3.2) — 알림 채널은 모두 Discord
│   └── secrets/
│       └── discord_webhook_url.example  # 실제 파일(discord_webhook_url)은 gitignore
└── grafana/
    ├── provisioning/
    │   ├── datasources/datasource.yml   # Prometheus 단일 데이터소스
    │   └── dashboards/provider.yml      # dashboards/*.json 자동 로드
    └── dashboards/
        ├── kpi.json                     # §4 kpi
        └── send-pipeline.json           # §4 send-pipeline
```

> Prometheus scrape 설정과 알림 룰은 정본 경로(`06 §8`)대로 `backend/src/main/resources/{prometheus,alerts}/` 에 둔다.

---

## 기동

```bash
# 1) 백엔드를 호스트에서 먼저 띄운다 (Prometheus 가 backend:8080 → host 로 scrape)
cd backend && ./gradlew bootRun

# 2) Discord webhook 시크릿 생성 (커밋 금지)
cp infra/alertmanager/secrets/discord_webhook_url.example infra/alertmanager/secrets/discord_webhook_url
#   파일 내용을 실제 Discord 채널 webhook URL 로 교체

# 3) 옵저버빌리티 스택 기동
docker compose -f infra/docker-compose.observability.yml up -d
```

| 서비스 | URL | 비고 |
|--------|-----|------|
| Prometheus | http://localhost:9090 | Targets 에서 `wisecan-backend` UP 확인 |
| Alertmanager | http://localhost:9093 | 알림 → Discord |
| Grafana | http://localhost:3001 | admin / admin (env 로 변경), 대시보드 자동 프로비저닝 |

---

## 알림 → Discord

- 모든 알림은 Discord 로 전송한다(사용자 결정). 심각도 분기(P0 즉시 / P1 / P2)는 `06 §3.2` 구조를 유지하되 전송처만 Discord 로 통일.
- webhook URL 은 평문 커밋 금지 — `infra/alertmanager/secrets/discord_webhook_url` 파일(gitignore)로 주입한다.
- P0 는 group_wait 0s + 1h 반복, 그 외는 30s 그룹 대기 + 4h 반복.

---

## 현재 범위와 한계 (1차 셋업)

- 대시보드는 `kpi`, `send-pipeline` 2종을 우선 제공한다. 나머지(`payment`/`external`/`security`/`admin-queue`/`routing`, `06 §4`)는 해당 도메인(W-201+) 구현과 함께 추가한다.
- 알림 룰이 참조하는 다수 메트릭(`send_*`, `payment_*`, `admin_*` 등)은 발송·결제·관리자 도메인 구현 시 노출된다. 그 전까지는 no-data 로 발화하지 않는다(정상).
- 운영자 콘솔 Grafana 임베드(`06 §5` Auth Proxy / `GrafanaProxyFilter`)와 observability 페이지(`PG-AD-017`)는 운영자 콘솔(M5) 작업에서 구현한다.
- 인프라 메트릭(JVM/Redis/MySQL/node)은 본 스택 범위 밖(`06 §1.3`).

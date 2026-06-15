# 환경 · 프로필 · 인프라

> 실제 `backend/src/main/resources/application*.yml`, `backend/docker-compose.yml` 기준.

---

## 1. 포트 맵

| 구성 | 포트 | 출처 |
|------|------|------|
| 백엔드 (Spring Boot) | **8080** | `application.yml` `server.port` |
| 프론트 (Next dev) | 3000 | `next dev` 기본 (CORS allowed-origin 과 정합) |
| MySQL (host→container) | **3309 → 3306** | `docker-compose.yml` |
| Redis | 6379 | `docker-compose.yml` |
| MCP 엔드포인트 | 8080 `/mcp` | Spring AI (STREAMABLE) |
| 메트릭 | 8080 `/actuator/prometheus` | actuator |

---

## 2. 백엔드 프로필 (3종)

`SPRING_PROFILES_ACTIVE` 로 전환. 기본 `local`.

| 프로필 | 용도 | 특징 |
|--------|------|------|
| `local` | 로컬 개발 | SQL/바인딩/Security 상세 로그(DEBUG/TRACE), CORS `http://localhost:3000`, `ddl-auto: update` |
| `develop` | 개발 서버 | 스테이징성 검증 |
| `product` | 운영 | 로그 축소, 스키마 보존(`ddl-auto: validate` 권장) |

```bash
./gradlew bootRun                                    # local
./gradlew bootRun --args='--spring.profiles.active=develop'
SPRING_PROFILES_ACTIVE=product java -jar build/libs/*.jar
```

> `application.yml` 기본 `ddl-auto: update` 는 **로컬 편의용**이다. `develop`/`product` 에서는 `validate` 로 두고 스키마 변경은 마이그레이션(수기 DDL/Flyway)으로 관리한다(`../backend/principles.md §3`).

---

## 3. 환경변수 (운영 주입 대상)

`application.yml` 은 `${ENV:default}` 패턴을 쓴다. **운영 값은 환경변수/Secrets 로 주입하고 커밋하지 않는다.**

| 변수 | 기본(local) | 운영 |
|------|-------------|------|
| `SPRING_PROFILES_ACTIVE` | `local` | `product` |
| `DB_USER` / `DB_PASSWORD` | `root` / `root` | 운영 계정(최소 권한) |
| (DB URL) | `jdbc:mysql://localhost:3309/wisecan_unified` | 운영 호스트 |
| `wisecan.cors.allowed-origins` | `http://localhost:3000` | 운영 도메인 |

### ⚠️ 보안 정정 필요 (현재 코드)
- `application.yml` 의 **`jwt.secret` 이 평문 하드코딩**돼 있다(`wisecan-unified-jwt-secret-...`). 운영 전 반드시 `${JWT_SECRET}` 환경변수로 분리하고, 노출된 기존 값은 폐기·교체한다. 이 변경은 보안 영향이 크므로 **독립 커밋**으로 처리(`../backend/principles.md §1`).
- `docker-compose.yml` 의 `MYSQL_ROOT_PASSWORD: root` 는 로컬 전용. 운영 DB 에 사용 금지.

---

## 4. 로컬 인프라 기동

```bash
# MySQL(3309) + Redis(6379)
docker compose -f backend/docker-compose.yml up -d
docker compose -f backend/docker-compose.yml ps
docker compose -f backend/docker-compose.yml down        # 중지 (-v 로 볼륨 삭제)
```
- MySQL DB `wisecan_unified` 자동 생성, 데이터는 `mysql-data` 볼륨에 영속.
- 백엔드는 이 인프라가 떠 있어야 기동된다. 프론트는 백엔드(8080)가 있어야 API 가 동작.

---

## 5. 의존성 / 데이터 흐름

```
member ── api_key ── api_usage     (MySQL: 영속 데이터)
Redis: 토큰 블랙리스트 · 레이트리밋(분당 60, mode=redis) · 캐시(TTL 1h)
JWT: access 1h / refresh 7d
```
- 레이트리밋 `mode` 가 `redis` 면 Redis 필수, `memory` 면 단일 인스턴스 한정(스케일아웃 시 redis 로).
- 전체 도메인 ERD·Redis 키 카탈로그는 `../../pm/05_DATA_MODEL.md` 정본.

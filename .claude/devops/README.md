# DevOps / 인프라 참조 — WiseCan 통합 메시징 서비스

> 빌드·실행·환경·관측을 담당하는 개발자가 읽는 진입점이다.
> 본 폴더(`.claude/devops/`)는 **실제 구성 파일**(docker-compose, build.gradle, application-*.yml, package.json)을 정본으로 한다.
> 아직 도입되지 않은 항목(예: 정식 CI 파이프라인)은 **권장안**임을 명시한다 — placeholder 로 단정하지 않는다.

---

## 1. 무엇부터 읽나

1. `environments.md` — 프로필 3종, 로컬 인프라(docker-compose: MySQL·Redis), 포트·환경변수 맵.
2. `build-run.md` — 백엔드(Gradle)·프론트(pnpm) 빌드·실행 커맨드, 산출물.
3. `observability.md` — Prometheus/Grafana/Alertmanager(`06_OBSERVABILITY` 정본 연계), 헬스체크.

---

## 2. 시스템 한눈에

```
[ frontend ]  Next.js 16 (pnpm)  ──HTTP/JWT──┐
                                              ▼
[ backend ]   Spring Boot 3.4 (Gradle, JDK 21)
                ├── MySQL 8.0        (영속)
                ├── Redis 7          (토큰 블랙리스트·레이트리밋·캐시)
                ├── /actuator/prometheus  (메트릭)
                └── /mcp/**          (Spring AI MCP, API Key 인증)
```

- 로컬 인프라: `backend/docker-compose.yml` (MySQL `3309:3306`, Redis `6379:6379`).
- 백엔드 프로필: `local`(기본) / `develop` / `product`.
- 배포 환경(staging/prod)·IDC 셋업은 `04_PROJECT_PLAN §2 M0`, 가용성 목표는 `06_OBSERVABILITY` / NFR.

---

## 3. 핵심 안전 규칙

- **시크릿은 커밋 금지.** DB/Redis 비밀번호, JWT secret, PG·중계사 키는 환경변수/Secrets 로만 주입(`application-*.yml` 의 `${ENV:default}`). `.env`·실 credential 커밋 금지(`.claude/rules/git-workflow.md`).
- **`main` 직접 push 금지.** 사용자 확인 후 push(루트 `CLAUDE.md`). git user.email 검증(hooks).
- 운영 DB 스키마는 **추가적 변경 + 롤백 가능**(`../backend/principles.md §3`). DDL 적용 영향·롤백을 PR 에 첨부.
- 환경 분리: 테스트망/상용망, 테스트/상용 API Key 식별(`04_PROJECT_PLAN W-205`). 테스트 자격으로 상용 적재 차단.

---

## 4. 현재 상태 vs 권장 (정직하게)

| 영역 | 현재 실재 | 권장(미도입) |
|------|-----------|--------------|
| 로컬 인프라 | `docker-compose.yml`(MySQL·Redis) ✅ | — |
| 프로필 | `application-{local,develop,product}.yml` ✅ | — |
| 메트릭 노출 | actuator + micrometer-prometheus ✅ | Grafana 대시보드/알림 룰 배선(`06_OBSERVABILITY`) |
| CI | 저장소에 파이프라인 정의 없음 | `build-run.md §CI 권장` 의 GitHub Actions/Jenkins 안 |
| 배포 | 정의 없음 | bootJar + `next build` 산출물 기반 컨테이너/IDC 배포 |

> CI/CD·배포 자동화는 아직 코드로 존재하지 않는다. 도입 시 본 폴더를 갱신하고, 추측성 설정을 미리 커밋하지 않는다.

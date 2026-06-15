# 빌드 · 실행 · CI

> 실제 `backend/build.gradle`, `frontend/package.json` 기준.

---

## 1. 백엔드 (Gradle · JDK 21)

```bash
cd backend
./gradlew --version                     # toolchain 이 JDK 21 자동 검색
./gradlew bootRun                        # 로컬 실행 (profile=local, :8080)
./gradlew test                           # JUnit 5 + Security Test
./gradlew test --tests com.wisecan.unified.service.ApiKeyServiceTest
./gradlew clean bootJar                  # 산출물 build/libs/*.jar
./gradlew bootJar -x test                # CI 미러링(테스트 생략)
java -jar build/libs/*.jar               # jar 직접 실행
```
- 산출물: `backend/build/libs/wisecan-unified-0.0.1-SNAPSHOT.jar`.
- 테스트는 H2(runtime)로 슬라이스 검증. MySQL 전용 동작은 통합 테스트로 보강.

---

## 2. 프론트엔드 (pnpm · Next 16)

```bash
cd frontend
pnpm install            # lockfile 기반 설치 (npm/yarn 금지)
pnpm dev                # 개발 서버 (:3000)
pnpm build              # 프로덕션 빌드 (.next/)
pnpm start              # 빌드 산출물 실행
pnpm lint               # eslint
pnpm typecheck          # tsc --noEmit
pnpm test               # vitest run
```
- **pnpm 고정**(`pnpm-lock.yaml`/`pnpm-workspace.yaml`). 다른 매니저로 install 시 lockfile 깨짐.
- 빌드 전 `pnpm typecheck && pnpm lint && pnpm test` 그린 확인.

---

## 3. 통합 로컬 기동 순서

```bash
# 1. 인프라
docker compose -f backend/docker-compose.yml up -d
# 2. 백엔드 (터미널 A)
cd backend && ./gradlew bootRun
# 3. 프론트 (터미널 B)
cd frontend && pnpm install && pnpm dev
# 4. 확인
curl http://localhost:8080/actuator/health       # {"status":"UP"}
# 브라우저: http://localhost:3000
```

---

## 4. 빌드 게이트 (머지 전 권장 체크)

`04_PROJECT_PLAN §8 게이트` 와 정합:
```
백엔드: ./gradlew test            (커버리지 라인 ≥ 70%, 핵심 도메인 ≥ 85% 목표)
        ./gradlew bootJar         (빌드 성공)
프론트: pnpm typecheck && pnpm lint && pnpm test
공통:   회귀 0 fail, 정적 분석 critical 0
보안:   SecurityConfig/필터 변경 시 테스트 동반 + 독립 커밋
```

---

## 5. CI 권장안 (현재 미도입 — 정직)

저장소에 CI 파이프라인 정의가 **아직 없다.** 도입 시 아래 골격을 권장하고, 확정되면 본 문서를 실제 파일 경로로 갱신한다.

```
[backend ci]
  - checkout → setup JDK 21 → ./gradlew clean test bootJar
  - 캐시: ~/.gradle, build/
[frontend ci]
  - checkout → setup pnpm → pnpm install --frozen-lockfile
  - pnpm typecheck && pnpm lint && pnpm test && pnpm build
[gate]
  - main 직접 push 금지 → PR 필수, 사용자 승인 후 merge
  - 커밋 컨벤션(.claude/rules/git-workflow.md) 검사
```

> 배포(컨테이너/IDC)·시크릿 주입(JWT/DB)·무중단 전략은 도입 시점에 `environments.md` 와 함께 설계한다. **추측성 파이프라인을 미리 커밋하지 않는다.**

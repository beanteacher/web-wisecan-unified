# syntax=docker/dockerfile:1
# 백엔드 컨테이너 이미지 (bootJar 멀티스테이지)
# 환경변수 주입 규칙: .claude/devops/environments.md §3
#   SPRING_PROFILES_ACTIVE / DB_USER / DB_PASSWORD / JWT_SECRET / CORS_ALLOWED_ORIGINS
#   → 시크릿은 이미지에 굽지 않고 런타임(배포 환경 Secrets)에서 주입한다.

# ── build stage ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 1) 빌드 설정·wrapper 먼저 복사해 의존성 레이어를 캐싱한다
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 2) 소스 복사 후 bootJar 만 산출 (테스트는 CI 에서 수행)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ── runtime stage ───────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# actuator 헬스 프로브용 curl
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# non-root 실행
RUN useradd -r -u 10001 -g root appuser

# plain jar 는 비활성(build.gradle) — bootJar 하나만 존재
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENV JAVA_OPTS=""

# /actuator/health UP 을 컨테이너 헬스로 사용 (.claude/devops/observability.md §1)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

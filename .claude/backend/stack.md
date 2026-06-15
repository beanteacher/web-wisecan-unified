# 백엔드 라이브러리 스택 — WiseCan

> 실제 `backend/build.gradle` 기준. 버전 핀은 build.gradle 이 정본.

---

## 1. 스택 요약

| 카테고리 | 기술 | 비고 |
|----------|------|------|
| 런타임 | **Java 21** (toolchain 고정) | record, switch 패턴, virtual thread 사용 가능 |
| 프레임워크 | **Spring Boot 3.4.x** | Jakarta EE, Security 6 |
| 빌드 | Gradle Wrapper | `./gradlew`, `gradlew.bat` |
| 웹 | spring-boot-starter-web | REST 전용 (뷰 렌더링 없음) |
| 영속성 | spring-boot-starter-data-jpa + Hibernate | 엔티티 중심 |
| DB | **MySQL 8.0** (`com.mysql:mysql-connector-j`) | utf8mb4, 운영 표준 |
| 캐시/세션 | spring-boot-starter-data-redis + cache | 토큰 블랙리스트·레이트리밋·검증코드 |
| 보안 | spring-boot-starter-security + **JJWT 0.12** | Stateless JWT |
| 검증 | spring-boot-starter-validation | DTO Bean Validation |
| **AI/MCP** | **spring-ai-starter-mcp-server-webmvc** (Spring AI 1.1.x) | MCP 서버 내장, `@Tool` 도구 |
| 모니터링 | actuator + micrometer-registry-prometheus | `/actuator/prometheus` |
| 보일러플레이트 | Lombok | `@Getter`/`@Builder`/`@RequiredArgsConstructor`/`@Slf4j` |
| 테스트 | spring-boot-starter-test + spring-security-test + **H2(runtime)** | 슬라이스는 H2 |

---

## 2. 주요 커맨드

```bash
cd backend
./gradlew bootRun                                   # 로컬 실행 (기본 profile=local)
./gradlew bootRun --args='--spring.profiles.active=develop'
./gradlew test                                      # JUnit 5 + Security Test
./gradlew test --tests com.wisecan.unified.service.ApiKeyServiceTest
./gradlew clean bootJar                             # 배포 산출물 build/libs/*.jar
./gradlew bootJar -x test                           # CI 미러링(테스트 생략)
```

로컬 인프라(MySQL 3309 / Redis 6379):
```bash
docker compose -f backend/docker-compose.yml up -d
```
상세는 `../devops/environments.md`.

---

## 3. 카테고리별 사용 규칙

### 영속성 — JPA + MySQL
- `application.yml`:
  ```yaml
  spring:
    jpa:
      open-in-view: false        # 지연 로딩 경계를 서비스에 고정
      hibernate:
        ddl-auto: validate       # 운영. local 만 update/create 허용
  ```
- ID 는 `GenerationType.IDENTITY`. enum 은 `EnumType.STRING`.
- 동적 쿼리가 복잡해지면 `XxxRepositoryCustom` + `Impl` 로 분리(아래 §확장 QueryDSL).

### 보안 — Spring Security + JJWT 0.12
- 완전 Stateless(`SessionCreationPolicy.STATELESS`).
- 비밀번호는 `BCryptPasswordEncoder`.
- 2종 인증: **JWT**(웹) + **API Key**(자동화/MCP). API Key 는 SHA-256 해시 저장.
- 새 공개 경로는 `SecurityConfig.permitAll` 에 명시.

### Redis
- `StringRedisTemplate` 주입(`RedisConfig`). 토큰 블랙리스트(`TokenBlacklistService`), 레이트리밋, 검증 코드 TTL.
- 키 네임스페이스는 서비스·환경별 prefix 로 충돌 방지.

### Spring AI MCP Server
- `McpConfig` 가 `@Tool` 메서드를 MCP 도구로 등록(`mcp/PingTool` 참고).
- 도구 추가 시 **CLI/SDK 동등성** 유지(KPI K2), **보안 민감 액션은 비노출**(`architecture.md §3 mcp`).
- 엔드포인트는 `/mcp/**`, 인증은 `ApiKeyAuthFilter`.

### 모니터링 — Actuator + Micrometer
- 커스텀 메트릭은 `MeterRegistry` 주입 후 `Counter`/`Timer`/`Gauge`.
- 메트릭·알림 정의 정본은 `../../pm/06_OBSERVABILITY.md`. 발송 적재 성공률·발송 API P95 등 KPI 메트릭을 이 정의에 맞춰 노출.

### 검증 — Bean Validation
- DTO record 필드에 `@NotNull/@NotBlank/@Size/@Email/@Pattern`. 컨트롤러 `@Valid` 트리거. 실패는 `GlobalExceptionHandler` → 400.

### Lombok
- 엔티티: `@Getter` + `@NoArgsConstructor` + 생성자 `@Builder`. `@Data`·엔티티 전체 `@Setter` 금지.
- 서비스/컴포넌트: `@RequiredArgsConstructor` + `@Slf4j`.
- DTO: record(Lombok 불필요).

---

## 4. 새 라이브러리 도입 판단

```
1. Spring Boot 스타터에 있나? → 스타터 우선.
2. 유사 기능이 이미 있나? → 기존 표준 재사용(principles.md §5 자산표).
3. annotation processor 필요? → Lombok 과 충돌 없는지 확인
   (compileOnly.extendsFrom annotationProcessor 설정).
4. Stateless JWT/무중단 배포 제약을 깨나? → 서버 세션 기반 라이브러리 금지.
```

---

## 5. 확장 예정 / 도입 시 합의 사항

현재 build.gradle 에 **없지만** 도메인 확장(발송·결제) 시 검토할 항목. 도입하면 본 문서와 `conventions.md` 를 갱신한다.

- **QueryDSL** — 발송 이력·관리자 콘솔의 동적 검색이 늘면 도입. `RepositoryCustom/Impl` 패턴 + `src/main/generated` Q클래스.
- **MapStruct** — DTO 변환이 폭증하면 `from()` 수동 팩토리에서 전환 검토(현재는 record `from()` 로 충분).
- **공통 `BaseEntity`** — 지금은 각 엔티티가 `@PrePersist` 로 `createdAt` 을 채운다. 감사 컬럼(`created_at`/`updated_at`/`created_by`)이 도메인 전반에 필요해지면 `@MappedSuperclass BaseEntity` + JPA Auditing 도입.
- **Flyway** — 수기 DDL 관리가 한계에 달하면 마이그레이션 도구 도입(추가적 변경 원칙은 `principles.md §3` 유지).

> 위 항목은 **추측성 선도입 금지**. 실제 필요가 생긴 PR 에서 근거와 함께 추가한다.

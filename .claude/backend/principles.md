# 백엔드 핵심 원칙 (Iron Rule)

> 어기면 리뷰 반려. "왜" 를 함께 적었다. 상세 예시·템플릿은 `conventions.md` 참조.

---

## 1. 보안 — 기본은 인증, 공개는 명시

- **시크릿·자격증명을 하드코딩하지 않는다.** JWT secret, MySQL/Redis 비밀번호, PG·중계사 키, CORS origin 등은 `application-*.yml` 의 `${ENV:default}` 로 주입하고 운영 값은 배포 파이프라인의 환경변수/Secrets 로 공급한다.
- `SecurityConfig` 의 `permitAll` 목록은 **명시된 경로만 공개**다. 현재 공개 경로는 `/api/v1/auth/**`, `/actuator/**`, `/mcp/**` 뿐. 새 공개 경로는 이 목록을 직접 수정하고 **PR 본문에 공개 사유**를 남긴다. 기본은 항상 인증 요구.
- **입력은 검증(`@Valid` + Bean Validation), 출력은 인코딩(Jackson 기본 이스케이프).** 사용자 문자열을 JPQL/네이티브 SQL 에 concat 하지 않는다 — 바인딩 파라미터를 쓴다.
- **소유권은 서비스에서 재검증한다.** URL 의 `{id}` 를 신뢰하지 않는다. `ApiKeyService.revoke()` 처럼 `apiKey.getMember().getId().equals(memberId)` 를 반드시 확인한다.
- **민감 정보 로깅 금지.** 토큰·비밀번호·API Key 원문·OTP·CI/DI 는 평문으로 남기지 않는다. API Key 는 **해시(SHA-256)로만 저장**하고 원문은 발급 응답 1회만 노출한다(`ApiKeyService.create`).
- 인증/인가 경로(`SecurityConfig`, `JwtAuthenticationFilter`, `ApiKeyAuthFilter`, `TokenBlacklistService`)를 수정하면 **테스트를 반드시 추가**하고, 보안 영향이 크므로 **독립 커밋**으로 분리한다.
- `/mcp/**` 는 `permitAll` 이지만 `ApiKeyAuthFilter` 가 진입 직후 API Key 를 검증한다. **공개 경로는 인증 필터가 책임진다** — 공격자가 임의 payload 를 보낸다는 전제로 작성한다.
- OWASP 중 **SQL Injection / Broken Access Control / Sensitive Data Exposure / Security Misconfiguration** 4종을 PR 체크리스트로 항상 확인한다.

> 왜: 메시징 서비스는 발신번호·결제·발송 권한이 곧 돈과 사칭 리스크다. 인증 경계가 무너지면 KPI(K3 운영 신뢰성)와 직결된다.

---

## 2. 에러 처리 — 경계에서 빠르게, 부수효과는 안전하게

- 비즈니스 오류는 **예외를 던지고 `GlobalExceptionHandler` 가 변환**한다. 컨트롤러/서비스에서 `try/catch` 로 HTTP 응답을 직접 만들지 않는다.
- 새 오류 케이스는 **전용 예외 클래스**(`exception/` 패키지)를 추가하고 `GlobalExceptionHandler` 에 핸들러를 등록한다. 현재: `EntityNotFoundException`(404), `DuplicateEmailException`(409), 검증 실패(400).
- **`throw new RuntimeException("...")` 를 비즈니스 오류로 쓰지 않는다.** (현재 `ApiKeyService` 일부에 남아 있는 패턴은 개선 대상 — 권한 오류는 `AccessDeniedException` 류, 상태 충돌은 전용 예외로 승격한다.)
- **에러를 조용히 삼키지 않는다.** `catch (Exception e) {}` 금지. 의도적으로 무시하면 **사유 한 줄 주석 + `log.warn`** 을 남긴다.
- 외부 송출·결제·파일 같은 **부수효과는 트랜잭션 커밋 이후**에 수행한다. 롤백 시 외부 호출이 되돌려지지 않으므로 `@TransactionalEventListener(AFTER_COMMIT)` 또는 멱등 재시도 가능한 형태로 분리한다.
- **변경된 비즈니스 로직에는 실패 경로 테스트를 최소 하나** 포함한다. happy path 만 있는 PR 은 반려.

> 왜: 발송·충전은 부분 실패가 잦다(잔액 부족 분기, PG 타임아웃). 실패를 1급 시민으로 다뤄야 운영 클레임이 줄어든다.

---

## 3. 데이터베이스 — MySQL, 추가적(additive) 변경

- 운영 DB 는 **MySQL 8.0 / utf8mb4**. `VARCHAR(N)` 의 N 은 **문자 수**(한글·이모지 1자 = 1). 자릿수는 `05_DATA_MODEL §표준 자릿수 가이드` 를 따른다.
- ID 는 **`@GeneratedValue(strategy = GenerationType.IDENTITY)`** (auto-increment). Oracle 시퀀스 패턴(`@SequenceGenerator`)을 가져오지 않는다.
- enum 컬럼은 `@Enumerated(EnumType.STRING)` + `length = 20`(또는 10). `ORDINAL` 금지(순서 바뀌면 데이터 깨짐).
- **스키마 변경은 추가적으로.** 컬럼 추가는 `NULL` 허용/기본값. 삭제·이름변경·타입변경은 (1) 새 컬럼 추가+양쪽 호환 → (2) 다음 릴리스에서 구 컬럼 제거 의 2단계로 분리한다.
- **스키마 마이그레이션과 기능 리팩터링을 한 커밋에 섞지 않는다.** DDL 은 `chore:` 별도 커밋으로 뺀다.
- 같은 조건을 `@Query` / 메서드 쿼리 / 네이티브 SQL 여러 곳에 중복 작성하지 않는다. 복잡 동적 쿼리가 필요해지면 `XxxRepositoryCustom` + `Impl` 로 분리한다(QueryDSL 도입은 `stack.md §확장` 참조).
- **핵심 영속성 경로는 `@DataJpaTest` (H2) 로 저장·조회·제약 위반을 검증**한다. MySQL 전용 동작이 의심되면 통합 테스트로 보강한다.

> 왜: 운영 중 무중단 배포를 하려면 스키마가 항상 구·신 코드와 동시에 호환돼야 한다.

---

## 4. 완전성 — 증분 비용이 낮으면 끝까지

- 엔티티를 추가하면 **repository·service·controller·DTO·테스트**까지 한 사이클에 함께 쌓는다.
- "happy path 만 머지하고 edge 는 나중에" 를 미완성으로 취급한다. 본 도메인의 전형적 엣지(잔액 부족, 발신번호 미승인, API Key 폐기/만료, 중복 이메일, 권한 없음)는 작성 시점에 함께 고려한다.
- **호수 vs 바다 구분.** 한정된 작업(예: "API Key 폐기 가드 추가")은 끝까지 구현한다. 무한한 재작성(예: "전 도메인 MapStruct 전환")은 단계별 체크리스트로 쪼개고 매 단계 독립 머지 가능하게 한다.
- `TODO`/`FIXME` 를 남기며 테스트·문서 중 하나를 건너뛴 코드는 반려. 끌 거면 복구 계획을 커밋/PR 에 명시한다.

### 완료 선언 전 체크리스트
```
[ ] controller·service·repository·domain·dto 가 정합하게 연결되는가?
[ ] DDL/엔티티 변경 시 enum 길이·NULL 허용·인덱스를 확인했는가?
[ ] SecurityConfig permitAll 에 경로 추가/제거가 필요한가?
[ ] ApiResponse<T> 로 응답을 감쌌는가? 엔티티 직접 노출이 없는가?
[ ] 성공 + 실패 경로 테스트가 있는가?
[ ] 로그에 민감 정보(토큰/키/비밀번호)가 없는가?
[ ] application-{local,develop,product}.yml 에 환경별 값이 모두 있는가?
```

---

## 5. 만들기 전에 검색 — 기존 자산 재사용

새 인프라/유틸을 만들기 전에 이미 있는지 확인한다. 중복 구현 금지.

| 필요 | 이미 있는 것 |
|------|--------------|
| 표준 응답 | `dto/ApiResponse<T>` (`success`/`error`) |
| 예외 변환 | `exception/GlobalExceptionHandler` + 전용 예외 |
| 현재 로그인 식별 | `MemberService.getCurrentMemberId()`, `common/security/*Principal` |
| JWT 발급/검증 | `config/JwtProvider`, `JwtAuthenticationFilter` |
| 토큰 블랙리스트 | `service/TokenBlacklistService` (Redis) |
| API Key 인증·레이트리밋 | `config/ApiKeyAuthFilter` |
| MCP 도구 등록 | `mcp/` + `config/McpConfig` (Spring AI) |
| 비밀번호 해시 | `SecurityConfig.passwordEncoder()` (BCrypt) |
| Redis 접근 | `config/RedisConfig` + `StringRedisTemplate` |

- 외부 라이브러리 추가 판단: ① Spring Boot 스타터에 있나? → 쓴다. ② 기존 의존성으로 15줄 안에 되나? → 추가 안 한다. ③ 추가한다면 Lombok annotation processor 와 충돌하지 않는지 확인하고 PR 에 근거를 남긴다.

> 왜: 응답·예외·인증을 제각각 만들면 프론트가 일관된 계약을 못 받고, MCP/CLI 동등성(K2)도 깨진다.

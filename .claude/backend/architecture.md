# 백엔드 아키텍처 — 패키지·레이어 지도

> "이 코드는 어디에 두지?" 의 단일 참조. 실제 `backend/src/main/java/com/wisecan/unified` 기준.

---

## 1. 구조: Package-by-Layer

본 프로젝트는 **레이어 단위 패키징**(package-by-layer)을 쓴다. (web-s9 의 도메인 단위 패키징과 다르다.)

```
com.wisecan.unified/
├── Application.java              # Spring Boot 진입점
│
├── config/                       # 설정·필터·빈
│   ├── SecurityConfig            #   Stateless JWT + CORS + 필터 체인
│   ├── JwtAuthenticationFilter   #   JWT 인증 (/api/v1/** 보호 경로)
│   ├── JwtProvider               #   토큰 발급/파싱
│   ├── ApiKeyAuthFilter          #   /mcp/** API Key 인증 + 레이트리밋
│   ├── RedisConfig               #   StringRedisTemplate / 캐시
│   └── McpConfig                 #   Spring AI MCP 서버 도구 등록
│
├── controller/                   # HTTP 어댑터 (@RestController)
│   ├── AuthController            #   /api/v1/auth/**
│   ├── ApiKeyController          #   /api/v1/api-keys/**
│   └── UsageController           #   /api/v1/usage/**
│
├── service/                      # 비즈니스 로직 + 트랜잭션 경계
│   ├── AuthService / MemberService
│   ├── ApiKeyService / UsageService
│   └── TokenBlacklistService     #   Redis 기반
│
├── repository/                   # Spring Data JPA
│   ├── MemberRepository
│   ├── ApiKeyRepository
│   └── ApiUsageRepository
│
├── domain/                       # JPA 엔티티 + 도메인 enum
│   ├── Member / MemberRole / MemberStatus
│   ├── ApiKey / ApiKeyStatus
│   └── ApiUsage / UsageStatus
│
├── dto/                          # 요청·응답 (record + ApiResponse 래퍼)
│   ├── ApiResponse<T>            #   공통 응답 envelope
│   ├── AuthDto / ApiKeyDto / UsageDto
│
├── exception/                    # 예외 + 전역 핸들러
│   ├── GlobalExceptionHandler
│   ├── EntityNotFoundException
│   └── DuplicateEmailException
│
├── common/                       # 공통 모델
│   └── security/                 #   UserPrincipal / ApiKeyPrincipal / CallerPrincipal
│
└── mcp/                          # Spring AI MCP 도구
    └── PingTool                  #   @Tool 메서드 (발송·이력 등 확장 지점)
```

> 도메인이 늘어나면(발신번호·발송·결제·체험모드) 같은 레이어 폴더 안에 클래스를 추가한다. `controller/SendController`, `service/SendService`, `domain/SendMessage` 식. **레이어 폴더를 도메인별로 다시 쪼개지 않는다**(현 관행 유지). 단, 클래스 수가 폭증하면 `controller/send/` 같은 하위 묶음을 도입할 수 있고 그때는 PR 에서 합의한다.

---

## 2. 의존성 방향 (핵심 규칙)

```
controller → service → repository → domain
      ↘          ↘                    ↗
        dto   (record from() 로 엔티티→DTO 변환)
config / common / exception 은 전 레이어가 참조 가능
```

| 레이어 | import 가능 | 금지 |
|--------|------------|------|
| `controller` | `service`, `dto`, `common`, `exception` | `repository`/`domain` 직접 조작, **엔티티 반환** |
| `service` | `repository`, `domain`, `dto`, `common`, `config`(빈), 타 `service` | `controller` |
| `repository` | `domain` | `service`, `controller`, `dto` |
| `domain` | (자기 완결) JPA·enum | `service`, `controller`, `repository`, `dto` |
| `dto` | `domain` 의 enum / 엔티티(정적 `from()` 입력) | `service`, `repository`, `controller` |

- **엔티티는 컨트롤러 경계를 넘지 않는다.** 응답은 DTO record 로 변환 후 `ApiResponse` 로 감싼다.
- 단방향 의존. 순환 금지. 크로스 도메인 부수효과가 생기면 `ApplicationEventPublisher` 로 분리한다.

---

## 3. 레이어별 역할

### `controller/` — HTTP 어댑터
- `@RestController` + `@RequestMapping("/api/v1/...")` + `@RequiredArgsConstructor`.
- 요청 DTO 에 `@Valid`. 응답은 `ResponseEntity<ApiResponse<T>>`.
- **비즈니스 로직 금지** — 서비스 호출 + 응답 래핑만. 메서드 3~5줄.
- 현재 로그인 회원은 `memberService.getCurrentMemberId()` 로 얻는다.

### `service/` — 유즈케이스 / 트랜잭션
- `@Service` + `@RequiredArgsConstructor` + (쓰기 도메인은) `@Transactional`.
- 읽기 전용 메서드는 `@Transactional(readOnly = true)`.
- 검증 실패·권한 오류·상태 충돌은 **예외로 던진다**(`GlobalExceptionHandler` 가 변환).
- 엔티티→DTO 변환은 DTO record 의 `from()` 정적 팩토리로(예: `ApiKeyDto.Response::from`).

### `repository/` — 데이터 접근
- `extends JpaRepository<T, ID>`.
- 단순 조건은 메서드명 쿼리(`findByMemberIdOrderByCreatedAtDescIdDesc`), 복잡하면 `@Query`(JPQL).
- 비즈니스 로직 금지.

### `domain/` — 엔티티
- `@Entity` + `@Table(name = "...")` + `@Getter` + `@NoArgsConstructor` + `@Builder`(생성자에).
- ID 는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- enum 은 `@Enumerated(EnumType.STRING)`.
- 생성 시각은 `@PrePersist` 로 `createdAt` 채움(공통 `BaseEntity` 는 아직 없음 — `stack.md §확장` 참조).
- 상태 변경은 **도메인 메서드로 캡슐화**(예: `apiKey.revoke()`). 서비스에서 setter 남발 금지.

### `dto/` — 전송 객체
- **record** 사용. 한 도메인당 `XxxDto` 클래스 안에 `CreateRequest / Response / ...` 중첩 record.
- 검증 어노테이션은 record 파라미터에 직접.
- 엔티티→DTO 는 `Response.from(entity)` 정적 팩토리. (MapStruct 미도입.)
- 모든 응답은 최종적으로 `ApiResponse.success(dto)` 로 감싼다.

### `mcp/` — Spring AI MCP 도구
- `@Tool` 어노테이션 메서드로 MCP 도구를 노출(`PingTool` 참고).
- **보안 민감 액션(키 발급·발신번호 등록·결제)은 MCP 로 노출하지 않는다**(`01_PRD §페르소나`, `RQ-MCP-014` 비노출 정책). 자연어 추론에 의한 우발 호출 차단.
- 발송·이력 조회 같은 안전 액션만 도구화하고, CLI 동등성을 유지한다.

---

## 4. 횡단 관심사

### 보안 (`config/SecurityConfig`)
- **Stateless JWT.** 세션 미사용. permitAll: `/api/v1/auth/**`, `/actuator/**`, `/mcp/**`.
- 필터 순서: `JwtAuthenticationFilter` (UsernamePasswordAuthenticationFilter 앞) → `ApiKeyAuthFilter` (JWT 필터 뒤, `/mcp/**` 전용).
- CORS origin·레이트리밋은 `application-*.yml` 의 `wisecan.cors.*` / `wisecan.rate-limit.*` 로 주입.
- 현재 로그인 식별은 `*Principal` + `MemberService` 경유. `SecurityContextHolder` 직접 파싱 지양.

### 인증 2종 (이 프로젝트 특성)
- **JWT** — 웹 콘솔 사용자(`/api/v1/**`).
- **API Key** — 발송 자동화/SDK/CLI/MCP(`/mcp/**`). SHA-256 해시 저장, prefix 8자 표시용, 레이트리밋(Redis) 적용.

### MCP + CLI 동등성 (KPI K2)
- MCP 도구를 추가하면 동일 기능의 CLI 명령·SDK 메서드도 함께 설계한다. 동등성 매트릭스를 깨지 않는다(`04_PROJECT_PLAN W-303`).

---

## 5. 새 도메인 추가 체크리스트 (예: 발송 `send`)

1. `domain/` 에 엔티티 + enum (`SendMessage`, `SendChannel`, `SendStatus`). `IDENTITY` ID, `@PrePersist` 시각.
2. `repository/` 에 `SendRepository extends JpaRepository`.
3. `dto/SendDto` 에 `CreateRequest`(@Valid) / `Response`(`from()`).
4. `service/SendService` — `@Transactional`, 검증·예외, 부수효과는 이벤트로.
5. `controller/SendController` — `/api/v1/send`, `ApiResponse` 래핑.
6. 새 오류는 `exception/` 에 전용 예외 + `GlobalExceptionHandler` 핸들러.
7. 공개·자동화 경로면 `SecurityConfig`/MCP 노출 정책 확인.
8. 성공+실패 테스트.

# 백엔드 코드 컨벤션 — 상세 + 실제 코드 예시

> 규칙의 "왜"·우선순위는 `principles.md`, 패키지 배치는 `architecture.md`.
> 아래 예시는 모두 실제 코드(`com.wisecan.unified`) 패턴이다.

---

## 1. 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스/인터페이스/enum/record | PascalCase | `ApiKeyService`, `MemberStatus` |
| 구현체(필요 시) | 인터페이스명 + `Impl` | `XxxRepositoryImpl` |
| 메서드·필드·파라미터 | camelCase | `getCurrentMemberId`, `keyHash` |
| 상수 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| enum 값 | SCREAMING_SNAKE_CASE | `ACTIVE`, `REVOKED`, `COMPANY_MASTER` |
| 패키지 | 소문자 | `com.wisecan.unified.service` |
| 테이블·컬럼 | snake_case | `member`, `created_at`, `key_hash` |
| URL | 소문자 + `/api/v1/` prefix | `/api/v1/api-keys` |

---

## 2. API 설계

### URL
```
GET    /api/v1/{resource}          목록
GET    /api/v1/{resource}/{id}     단건
POST   /api/v1/{resource}          생성
PUT    /api/v1/{resource}/{id}     전체 수정
PATCH  /api/v1/{resource}/{id}     부분 수정 / 상태 전환
DELETE /api/v1/{resource}/{id}     삭제
```
- **`/api/v1/` prefix 필수**(현 관행). MCP 경로만 예외(`/mcp/**`).
- 상태 전환은 `PATCH /{id}/revoke` 처럼 하위 동작으로(실제 `ApiKeyController` 패턴). 동사형 최상위 경로(`/do-xxx`) 금지.
- 공개 경로는 `SecurityConfig.permitAll` 에 명시.

### 응답: `ApiResponse<T>` envelope 필수
```java
// 성공
return ResponseEntity.ok(ApiResponse.success(dto));
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
// 데이터 없는 성공
return ResponseEntity.ok(ApiResponse.success(null));
// 실패 → 던지면 GlobalExceptionHandler 가 ApiResponse.error(message) 로 변환
```
`{ success, data, message, timestamp }` 형태로 통일된다. **컨트롤러에서 직접 `ResponseEntity.badRequest().body(...)` 를 만들지 않는다.**

### 컨트롤러 — 실제 패턴
```java
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyDto.CreateResponse>> create(@RequestBody @Valid ApiKeyDto.CreateRequest request) {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(apiKeyService.create(memberId, request)));
    }

    @PatchMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable Long id) {
        Long memberId = memberService.getCurrentMemberId();
        apiKeyService.revoke(memberId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```
- 반환 타입은 `ResponseEntity<ApiResponse<T>>` 로 **제네릭을 좁혀서** 쓴다(현 관행). `ResponseEntity<?>` 남용 금지.
- 비즈니스 로직 없음. `@Valid` 필수. 소유권은 서비스(`memberId` 전달)가 재검증.

---

## 3. DTO (record)

```
규칙 1. dto/request, dto/response 하위 폴더 금지.
규칙 2. dto/{Domain}Dto.java 한 파일에 중첩 record 로 모은다.
        → ApiKeyDto.{CreateRequest, CreateResponse, Response}
규칙 3. 컨트롤러/서비스에 record 직접 선언 금지.
규칙 4. 검증은 record 파라미터에 직접.
규칙 5. Entity → DTO 는 record 내부 static from() 팩토리.
규칙 6. record 는 불변. Setter/@Builder 금지(빌더가 필요하면 정적 팩토리에서 정리).
```
```java
public class ApiKeyDto {
    public record CreateRequest(@NotBlank @Size(max = 100) String keyName) {}

    public record Response(Long id, String keyName, String keyPrefix, String status, LocalDateTime createdAt) {
        public static Response from(ApiKey e) {
            return new Response(e.getId(), e.getKeyName(), e.getKeyPrefix(), e.getStatus().name(), e.getCreatedAt());
        }
    }
}
```
- 서비스에서 `.map(ApiKeyDto.Response::from).toList()` 로 변환(실제 `ApiKeyService.getMyKeys`).
- **원문 시크릿(API Key 등)은 생성 응답에 1회만** 담고 이후 조회 응답엔 prefix 만.

---

## 4. Service

```java
@Service
@RequiredArgsConstructor
@Transactional                       // 클래스 레벨 쓰기 트랜잭션 (현 관행)
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final MemberRepository memberRepository;

    public ApiKeyDto.CreateResponse create(Long memberId, ApiKeyDto.CreateRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("Member", memberId));
        // ... 도메인 생성 + 저장
    }

    @Transactional(readOnly = true)  // 읽기 전용은 명시
    public List<ApiKeyDto.Response> getMyKeys(Long memberId) { ... }
}
```
- `@RequiredArgsConstructor` + `private final` 주입. **`@Autowired` 필드 주입 금지**(테스트 슬라이스 제외).
- 클래스 레벨 `@Transactional` + 읽기 메서드 `@Transactional(readOnly = true)`.
- 리소스 없음은 `EntityNotFoundException(엔티티명, id)`.
- **권한/상태 충돌은 전용 예외로**(현재 `RuntimeException` 사용처는 승격 대상, `principles.md §2`).

---

## 5. Entity

```java
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @Builder
    public Member(String email, /* ... */) { /* 필드 대입 */ }
}
```
- `@GeneratedValue(IDENTITY)` — MySQL auto-increment.
- enum 은 `EnumType.STRING`. `length` 는 `05_DATA_MODEL` 자릿수 가이드(enum 20, 짧은 enum 10).
- `@Builder` 는 **생성자에** 부착(전체 클래스 아님) — 불변 필드만 받게.
- 상태 변경 메서드를 엔티티에 둔다(예: `revoke()`). `@Data`/엔티티 전체 `@Setter` 금지.
- 자릿수·NULL·unique 제약을 컬럼에 명시.

---

## 6. 예외 처리

```java
// 리소스 없음
throw new EntityNotFoundException("ApiKey", apiKeyId);   // → 404
// 중복
throw new DuplicateEmailException(email);                // → 409
// 검증 실패 → @Valid 가 자동, GlobalExceptionHandler 가 400
```
- 새 오류 = `exception/` 에 전용 예외 클래스 + `GlobalExceptionHandler` 핸들러 추가.
- **컨트롤러에서 try/catch 금지.** 전역 핸들러가 `ApiResponse.error(message)` 로 변환.
- `RuntimeException` 을 비즈니스 오류로 던지지 않는다(권한 없음/상태 충돌은 전용 예외로).

---

## 7. 트랜잭션·이벤트

- `@Transactional` 은 **서비스에서만**. 컨트롤러·레포지토리 금지.
- 롤백 대상 체크 예외가 있으면 `rollbackFor = Exception.class`.
- **DB 커밋 후에만 해야 할 부수효과**(외부 발송·결제 확정·알림)는 `ApplicationEventPublisher.publishEvent(...)` → `@TransactionalEventListener(phase = AFTER_COMMIT)` 리스너로 분리. 트랜잭션 롤백 시 외부 호출이 따라 롤백되지 않는 문제를 차단.

---

## 8. 로깅·스타일

- 로거는 Lombok `@Slf4j` 만. 민감 정보(토큰·키·비밀번호·OTP) 로깅 금지.
- import 와일드카드(`.*`) 금지. 들여쓰기 4 spaces.
- 메서드 시그니처 개행 금지(120자 초과 등 사유 없으면 한 줄).
- `Optional` 은 **반환 타입에만**. 필드·파라미터 금지.
- `var` 는 타입이 자명한 지역변수에 한해 허용(과용 금지). 컬렉션·DTO 등은 명시적 타입 권장.
- Java 21 — record, switch 패턴 매칭, `HexFormat` 등 표준 API 적극 활용.

---

## 9. 커밋 (루트 `.claude/rules/git-workflow.md` 준수)

- 타입 필수, **scope 없음**: `feat:`, `fix:`, `refactor:`, `perf:`, `docs:`, `test:`, `chore:`, `style:`, `ci:`, `build:`.
- 한글 subject 허용, 마침표로 끝내지 않음. 본문은 "왜".
- 요구사항 ID 연결: `feat: API Key 폐기 가드 추가 (REQ-KEY-031)`.
- 보안 변경(SecurityConfig·필터)·DDL 은 **독립 커밋**으로 분리.

# /back-refactor 커맨드

기존 백엔드 코드를 레이어드 아키텍처와 Spring 컨벤션에 맞게 리팩토링합니다.

## 참조
- 컨벤션: `.claude/rules/backend-conventions.md`
- 코드 패턴: `.claude/rules/backend-java-patterns.md`
- 디자인 패턴: `.claude/rules/backend-design-patterns.md`

## 입력
$ARGUMENTS: 리팩토링 대상 또는 범위 (예: "service/product", "전체 레이어 검증", "DTO 정리")

**사용법:**
- `/back-refactor service/product` — 특정 패키지 리팩토링
- `/back-refactor 전체 레이어 검증` — 프로젝트 전체 구조 점검
- `/back-refactor DTO 정리` — DTO inner record 전환
- `/back-refactor 예외 처리` — 예외 체계 통합

---

## 실행 단계

### 1단계: 현재 상태 분석

```bash
# 프로젝트 구조 확인
find src/main/java -type f -name "*.java" | head -50

# 컴파일 에러 확인
./gradlew compileJava 2>&1 | tail -30

# 테스트 상태 확인
./gradlew test 2>&1 | tail -20
```

### 2단계: 문제 식별

다음 항목들을 순서대로 점검합니다:

#### 레이어 위반
- [ ] Controller에 비즈니스 로직 포함
- [ ] Service에서 DB 직접 접근 (Repository 우회)
- [ ] Repository에 비즈니스 로직 포함
- [ ] Entity에 요청/응답 관련 어노테이션 (@RequestBody 등)
- [ ] 순환 의존 (Service A → Service B → Service A)

#### 멀티 모듈 위반 (multi-module 프로젝트인 경우)
- [ ] 모듈 의존 방향 역전 (`common` → `domain`, `domain` → `api` 금지)
- [ ] `common` 모듈에서 `domain` 또는 `api` 클래스 import
- [ ] `domain` 모듈에서 `api` 클래스 import
- [ ] Entity/Repository가 `api` 모듈에 위치 (`domain` 모듈로 이동 필요)
- [ ] 공통 예외/DTO가 `api` 모듈에 위치 (`common` 모듈로 이동 필요)
- [ ] 모듈 간 순환 의존

#### DTO / Entity 구조
- [ ] Controller/Service에 직접 record/class 선언 (dto/ 패키지 외부)
- [ ] Entity를 API 응답으로 직접 반환
- [ ] DTO가 inner record가 아닌 개별 파일로 분산
- [ ] Builder 패턴을 DTO record에 사용 (Entity에서만 허용)

#### 코드 품질
- [ ] Raw Type 사용 (`List` → `List<Product>`)
- [ ] 하드코딩된 매직 넘버/스트링
- [ ] 중복 코드 (3회 이상 반복)
- [ ] 과도한 클래스 크기 (300줄 초과)
- [ ] 불필요한 `@Autowired` (생성자 주입으로 전환)
- [ ] `Optional` 오용 (필드, 파라미터에 사용)

#### Spring 컨벤션
- [ ] `@Transactional` 누락 또는 부적절한 범위
- [ ] 읽기 전용 메서드에 `@Transactional(readOnly = true)` 미적용
- [ ] Service 인터페이스 없이 구현체만 존재 (DIP 미준수)
- [ ] 예외 처리가 GlobalExceptionHandler 대신 각 Controller에 산재

#### 디자인 패턴 적용 점검

> 패턴별 적용 시점, 판단 기준: `.claude/rules/backend-design-patterns.md` 참조

### 3단계: 리팩토링 계획 수립

발견된 문제를 우선순위별로 정리합니다:

```markdown
## 리팩토링 계획

### 🔴 즉시 수정 (레이어 위반, 컴파일 에러)
1. [파일:라인] 문제 설명 → 해결 방안

### 🟡 권장 수정 (코드 품질, Spring 컨벤션)
1. [파일:라인] 문제 설명 → 해결 방안

### 🟢 개선 제안 (디자인 패턴, 구조 최적화)
1. [파일:라인] 문제 설명 → 해결 방안
```

**사용자에게 계획을 보여주고 승인을 받은 후 진행합니다.**

### 4단계: 단계별 리팩토링 실행

리팩토링 순서 (의존 방향: 하위 → 상위):

```
1. domain/      → Entity, Enum, VO 정리
2. dto/         → inner record 전환, Entity 직접 반환 제거
3. exception/   → 커스텀 예외 + GlobalExceptionHandler 통합
4. repository/  → 쿼리 정리, Specification 적용
5. service/     → 인터페이스 분리, 트랜잭션 범위 조정
6. controller/  → 비즈니스 로직 제거, 응답 래핑 통일
7. config/      → 설정 클래스 정리
8. event/       → (kafka 사용 시) 이벤트 구조 정리
```

각 단계마다:
1. 변경 전 테스트 실행
2. 코드 수정
3. 컴파일 확인 (`./gradlew compileJava`)
4. 변경 후 테스트 실행
5. 커밋

### 5단계: 검증

```bash
# 컴파일
./gradlew compileJava

# 전체 테스트
./gradlew test

# 빌드
./gradlew build

# (선택) SpotBugs / Checkstyle
./gradlew check

# 멀티 모듈인 경우 — 모듈별 개별 검증
# ./gradlew :common:build
# ./gradlew :domain:build
# ./gradlew :api:build
```

### 6단계: 결과 보고

```markdown
## 리팩토링 결과

### 변경 요약
- 수정 파일: N개
- 레이어 위반 수정: N건
- 코드 품질 개선: N건
- 디자인 패턴 적용: N건

### 변경 내역
| 파일 | 변경 내용 | 이유 |
|------|-----------|------|
| ... | ... | ... |

### 검증 결과
- [ ] 컴파일 성공
- [ ] 전체 테스트 통과
- [ ] 빌드 성공
- [ ] 레이어 의존성 규칙 준수
```

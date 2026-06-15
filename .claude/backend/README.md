# 백엔드 개발자 참조 — WiseCan 통합 메시징 서비스

> Spring Boot 백엔드(`backend/`) 개발자가 코드를 작성·리뷰하기 전에 읽는 진입점이다.
> 본 폴더(`.claude/backend/`)는 **실제 코드베이스(`com.wisecan.unified`)의 관행**을 정본으로 한다.
> 기존 `.claude/rules/backend-*.md` 는 신규 생성 템플릿(스킬용)이고, 충돌 시 **실제 코드 > 본 폴더 > rules 템플릿** 순으로 우선한다.

---

## 1. 무엇부터 읽나 (우선순위)

1. `principles.md` — 절대 어기면 안 되는 핵심 원칙(보안·에러·DB·완전성·검색우선). **Iron Rule.**
2. `architecture.md` — 패키지·레이어 지도. "이 코드 어디에 두지?" 의 답.
3. `conventions.md` — 네이밍·DTO·예외·트랜잭션·API·로깅 상세 규칙 + 실제 코드 예시.
4. `stack.md` — 라이브러리 스택·빌드·실행 커맨드.
5. `../../pm/05_DATA_MODEL.md` — 도메인 ERD 정본(엔티티 설계 시).
6. `../../pm/02_FEATURE_SPEC.md` — 액션 카드(기능 구현 시).

---

## 2. 스택 한 줄 요약

Java 21 · Spring Boot 3.4 · MySQL 8.0(utf8mb4) · Redis 7 · Spring Security + JJWT · **Spring AI MCP Server(WebMVC)** · Micrometer Prometheus · Lombok · Gradle.

상세는 `stack.md`.

---

## 3. 이 프로젝트가 web-s9(OAM)와 다른 점 (혼동 주의)

과거 사내 OAM 백엔드(`web-s9-oam-back`)의 컨벤션을 참조했더라도, **WiseCan 은 아래가 다르다.** OAM 습관을 그대로 가져오지 말 것.

| 항목 | web-s9 (OAM) | **WiseCan (본 프로젝트)** |
|------|--------------|--------------------------|
| DB | Oracle (시퀀스 ID) | **MySQL 8.0 (`IDENTITY` auto-increment)** |
| 패키지 구조 | package-by-feature (`alarm/manage/...`) | **package-by-layer (`controller/`, `service/`, `domain/`...)** |
| 응답 형태 | envelope 없음, `ResponseEntity.ok(dto)` | **`ApiResponse<T>` envelope (`success`/`error`)** |
| 예외 | 단일 `WiseException` + `ErrorCode` | **개별 예외 클래스 + `GlobalExceptionHandler`** |
| URL | prefix 없음 (`/alarm/manage`) | **`/api/v1/...` prefix** |
| 매핑 | MapStruct | **record 내부 `from()` 정적 팩토리 (MapStruct 미도입)** |
| Java | 17 | **21** |

본 프로젝트만의 추가 자산: **Spring AI MCP Server**(`/mcp/**`) + **API Key 인증 필터**(발송 자동화용).

---

## 4. 작업 전 체크 (요약)

- [ ] 변경 도메인의 ERD(`05_DATA_MODEL`)·액션(`02_FEATURE_SPEC`)을 확인했다.
- [ ] 새 코드의 레이어 위치가 `architecture.md` 와 정합한다.
- [ ] 응답은 `ApiResponse<T>` 로 감쌌다. 엔티티를 직접 반환하지 않는다.
- [ ] 비즈니스 오류는 예외 클래스 + `GlobalExceptionHandler` 로 처리했다(컨트롤러 try/catch 금지).
- [ ] 공개 경로를 추가했다면 `SecurityConfig` 의 `permitAll` 에 명시하고 PR 에 사유를 남겼다.
- [ ] 성공 + 실패 경로 테스트를 함께 작성했다.
- [ ] `./gradlew test` 그린.

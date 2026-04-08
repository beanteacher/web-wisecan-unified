# /refactor 커맨드

기존 코드를 FSD 아키텍처와 컨벤션에 맞게 리팩토링합니다.

## 참조
- 아키텍처: `.claude/rules/frontend-architecture.md`
- 컨벤션: `.claude/rules/frontend-conventions.md`

## 입력
$ARGUMENTS: 리팩토링 대상 또는 범위 (예: "features/auth", "shared/api", "전체 FSD 검증")

---

## 실행 단계

### 1단계: 현재 상태 분석

```bash
# FSD 레이어 의존성 검증
bash scripts/validate-fsd.sh ./src

# 변경 대상 파일 확인
find src/ -name "*.ts" -o -name "*.tsx" | head -50

# TypeScript 에러 확인
npx tsc --noEmit 2>&1 | head -30
```

### 2단계: 문제 식별

다음 항목들을 순서대로 점검합니다:

#### FSD 위반 사항
- [ ] 레이어 간 잘못된 import (하위 → 상위)
- [ ] 같은 레이어 간 cross-import
- [ ] Public API(index.ts) 우회 직접 접근
- [ ] 잘못된 레이어에 배치된 코드

#### 코드 품질
- [ ] `any` 타입 사용
- [ ] 하드코딩된 매직 넘버/스트링
- [ ] 중복 코드 (3회 이상 반복)
- [ ] 과도한 컴포넌트 크기 (200줄 초과)
- [ ] 불필요한 `useState`/`useEffect`

#### 구조 개선
- [ ] Server/Client Component 경계 최적화
- [ ] 컴포넌트 분리가 필요한 큰 컴포넌트
- [ ] 공통 로직을 shared로 추출 가능한 코드

### 3단계: 리팩토링 계획 수립

발견된 문제를 우선순위별로 정리합니다:

```markdown
## 리팩토링 계획

### 🔴 즉시 수정 (FSD 위반, 타입 에러)
1. [파일:라인] 문제 설명 → 해결 방안

### 🟡 권장 수정 (코드 품질)
1. [파일:라인] 문제 설명 → 해결 방안

### 🟢 개선 제안 (구조 최적화)
1. [파일:라인] 문제 설명 → 해결 방안
```

**사용자에게 계획을 보여주고 승인을 받은 후 진행합니다.**

### 4단계: 단계별 리팩토링 실행

리팩토링 순서 (의존성 방향: 하위 → 상위):

```
1. shared/    → 공통 유틸리티, API 클라이언트
2. entities/  → 타입 정의, 엔티티 API/UI
3. features/  → 상태 관리, 인터랙션 UI
4. widgets/   → 조합 컴포넌트
5. app/       → 페이지, 레이아웃
```

각 단계마다:
- 변경 전 테스트 실행
- 코드 수정
- TypeScript 에러 확인
- 변경 후 테스트 실행
- 커밋

### 5단계: 검증

```bash
# FSD 검증
bash scripts/validate-fsd.sh ./src

# TypeScript
npx tsc --noEmit

# 린트
npm run lint

# 빌드
npm run build

# 테스트
npm run test
```

### 6단계: 결과 보고

```markdown
## 리팩토링 결과

### 변경 요약
- 수정 파일: N개
- FSD 위반 수정: N건
- 코드 품질 개선: N건

### 변경 내역
| 파일 | 변경 내용 | 이유 |
|------|-----------|------|
| ... | ... | ... |

### 검증 결과
- [ ] FSD 검증 통과
- [ ] TypeScript 에러 없음
- [ ] 린트 통과
- [ ] 빌드 성공
- [ ] 테스트 통과
```
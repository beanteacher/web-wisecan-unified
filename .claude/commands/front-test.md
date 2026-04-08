# /test 커맨드

특정 feature/entity에 대한 테스트를 생성하거나 기존 테스트를 실행합니다.

## 참조
- 라이브러리: `.claude/rules/frontend-library-stack.md` (Vitest + Testing Library)
- 아키텍처: `.claude/rules/frontend-architecture.md`

## 입력
$ARGUMENTS: 테스트 대상 (예: "features/auth", "entities/user", "shared/lib/cn", "run")

**사용법:**
- `/test features/auth` — features/auth 하위 테스트 생성
- `/test entities/user` — entities/user 하위 테스트 생성
- `/test run` — 전체 테스트 실행
- `/test run features/auth` — 특정 경로 테스트 실행

---

## 실행 단계

### 분기: "run"이면 테스트 실행

```bash
# 전체 실행
npx vitest run

# 특정 경로 실행
npx vitest run src/{path}
```

### 분기: 경로 지정이면 테스트 생성

### 1단계: 대상 파일 분석

```bash
# 테스트 대상 파일 목록
find src/$ARGUMENTS -name "*.ts" -o -name "*.tsx" | grep -v ".test." | grep -v "index.ts"
```

각 파일의 export를 분석하여 테스트 대상을 파악합니다.

### 2단계: 테스트 파일 생성

FSD 규칙에 따라 테스트 파일은 대상 파일 옆에 배치합니다:

```
features/auth/
├── api/
│   ├── authApi.ts
│   └── authApi.test.ts      ← 생성
├── model/
│   ├── useAuth.ts
│   └── useAuth.test.ts      ← 생성
└── ui/
    ├── LoginForm.tsx
    └── LoginForm.test.tsx    ← 생성
```

### 3단계: 테스트 유형별 템플릿

#### API 함수 테스트
```tsx
// {name}Api.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get{Entity}, create{Entity} } from './{name}Api';

// API 클라이언트 모킹
vi.mock('@/shared/api/client', () => ({
  api: {
    get: vi.fn(() => ({ json: vi.fn() })),
    post: vi.fn(() => ({ json: vi.fn() })),
  },
}));

describe('{name}Api', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('get{Entity} — 정상 호출', async () => {
    // given / when / then
  });
});
```

#### React Query 훅 테스트
```tsx
// use{Entity}.test.ts
import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { use{Entity} } from './use{Entity}';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('use{Entity}', () => {
  it('데이터를 정상적으로 반환한다', async () => {
    const { result } = renderHook(() => use{Entity}('1'), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
```

#### UI 컴포넌트 테스트
```tsx
// {Component}.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { {Component} } from './{Component}';

describe('{Component}', () => {
  it('렌더링된다', () => {
    render(<{Component} />);
    expect(screen.getByRole('...')).toBeInTheDocument();
  });

  it('사용자 인터랙션이 동작한다', async () => {
    const user = userEvent.setup();
    render(<{Component} />);
    await user.click(screen.getByRole('button'));
    // assertion
  });
});
```

#### Zustand 스토어 테스트
```tsx
// {name}Store.test.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { use{Name}Store } from './{name}Store';

describe('{name}Store', () => {
  beforeEach(() => {
    use{Name}Store.setState(use{Name}Store.getInitialState());
  });

  it('초기 상태가 올바르다', () => {
    const state = use{Name}Store.getState();
    expect(state).toEqual(/* 초기 상태 */);
  });

  it('액션이 상태를 올바르게 변경한다', () => {
    use{Name}Store.getState().someAction(/* args */);
    expect(use{Name}Store.getState().someField).toBe(/* 기대값 */);
  });
});
```

### 4단계: 테스트 실행 및 검증

```bash
# 생성된 테스트 실행
npx vitest run src/$ARGUMENTS

# 커버리지 확인
npx vitest run src/$ARGUMENTS --coverage
```

### 5단계: 결과 보고

```markdown
## 테스트 생성 결과

### 생성된 파일
| 파일 | 테스트 유형 | 테스트 수 |
|------|------------|----------|
| ...test.ts | API | N개 |
| ...test.tsx | UI | N개 |

### 실행 결과
- 전체: N개
- 성공: N개
- 실패: N개
- 커버리지: N%
```
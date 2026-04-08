# UI/UX Figma 플러그인 개발 규칙

> Figma 플러그인 code.js 작성 시 반드시 준수할 규칙.
> 과거 피드백에서 반복 발생한 실수를 방지하기 위한 체크리스트.

---

## 1. Figma Plugin 기본

- manifest 폴더에는 반드시 **3개 파일** 생성:
  - `manifest.json` — 플러그인 메타 (Figma가 읽는 진입점)
  - `manifest.import-data.json` — 디자인 스펙 데이터 (프레임, 토큰, 모션)
  - `code.js` — Figma Plugin API 코드 (캔버스에 실제 요소 생성)
- manifest.import-data.json만 만들고 code.js를 안 만들면 Figma에서 실행 불가
- `manifest.json` 필수 필드: `name`, `id`, `api`, `main`, `editorType`
  - `main`은 `"code.js"` (누락 금지)
  - `editorType`은 배열: `["figma"]`
- `const` 변수는 반드시 **선언 후 참조** (호이스팅 안 됨)
  - 데이터 배열(tableRows 등)을 먼저 선언하고, 그 length를 사용하는 코드는 아래에 배치

## 2. 텍스트 중앙 정렬 (가장 빈번한 실수)

- `addText` 헬퍼에 반드시 `textAlignHorizontal` 적용 코드 포함:
  ```js
  if (opts.align) t.textAlignHorizontal = opts.align;
  ```
- **원칙: 컨테이너 내부 텍스트는 기본적으로 중앙 정렬**
  - 버튼 텍스트: `w: 버튼너비, align: 'CENTER'`
  - 테이블 셀: `w: 컬럼너비, align: 'CENTER'`
  - 뱃지 텍스트: `w: 뱃지너비, align: 'CENTER'`
  - 원형 아이콘 안 텍스트 (✓, !, i): `x: 원의x, w: 원의너비, align: 'CENTER'`
- 좌측 정렬이 적절한 경우: 섹션 제목, 설명 본문, 네비게이션 메뉴 항목

## 3. 컴포넌트 스타일 규칙

### 버튼
- Loading 상태: "로그인 중..." 텍스트 금지 → 원래 텍스트 유지 + 옆에 스피너
- 스피너: 원형 도넛 링 + 밝은 점 (radius = 크기/2, 네모 금지)

### 에러 배너
- 버튼과 구분되어야 함: 좌측 강조 바(4px) + 아이콘 + 메시지
- 버튼보다 높이 크게 (52px+), radius 작게 (6px)

### 테이블
- zebra striping (줄마다 색 변경) 금지 → 헤더만 #F8FAFC, 데이터 행은 모두 #FFFFFF
- 열 구분선 필수 (세로 라인, #E2E8F0 이상 진한 색)
- 뱃지/버튼은 셀 내 중앙 배치: `colX + (colWidth - elementWidth) / 2`

### 사이드바
- 화면 끝에 붙는 면은 직각(0), 반대면만 라운드(16):
  ```js
  frame.topLeftRadius = 0;
  frame.topRightRadius = 16;
  frame.bottomRightRadius = 16;
  frame.bottomLeftRadius = 0;
  ```
- 전체 UI가 둥근 모서리 톤이면 사이드바도 라운드 플로팅 적용

### 원형 아이콘
- radius는 반드시 크기의 절반 (20x20 → radius: 10)
- 내부 텍스트는 w + align CENTER로 중앙 정렬

### 색상 가시성
- 구분선 최소 #E2E8F0 (slate-200) 이상 — #F1F5F9는 안 보임

## 4. 상태 표현 규칙

- 폼은 **정상 상태 + 에러 상태** 두 가지 모두 표현
- 에러 상태만 있고 정상 상태 없으면 안 됨

## 5. 모션 전달 규칙

- Figma는 정적 → 모션을 전달하려면:
  - Before/After 프레임으로 상태 변화 시각화
  - 프레젠테이션 스텝은 **섹션 단위** (글자 하나하나 X)
  - 레이어 이름에 `[motion:...]` 라벨
  - manifest.import-data.json에 motions 배열

## 6. 문구/디자인 방향 규칙

- 내부 기술명(MCP 등)을 고객 대면 문구에 노출 금지
- 벤치마킹은 **구조/문구 방향** 참고, **비주얼(색상/레이아웃) 차별화** 필수
- 히어로 섹션에서 서비스의 핵심 기능 목록이 바로 보여야 함

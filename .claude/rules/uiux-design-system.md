---
globs: ["*.figma", "*.sketch", "*design-system*", "*design-token*", "*theme*"]
description: "UI/UX 디자인 시스템 (Figma 구조, 컬러/타이포/스페이싱, 와이어프레임, 컴포넌트 라이브러리) 참조"
---

# UI/UX 디자인 시스템 & 산출물 참고 자료 (참고 자료 전용)

> **이 파일은 디자인 시스템·유저 플로우·컴포넌트 라이브러리·manifest 스키마 참고 자료만 포함합니다. 규칙·컨벤션은 `uiux_designer/AGENTS.md`를 참고하세요.**
> `/uiux:design-system` 스킬로 호출 가능합니다.

---

## 프로젝트 컨텍스트

> 프로젝트명, 레퍼런스 사이트, 디자인 방향은 루트 AGENTS.md를 참조한다.

| 항목 | 내용 |
|------|------|
| **지원 디바이스** | PC (Desktop 1280px~) / Mobile (375px~) |
| **디자인 툴** | Figma |
| **폰트** | 프로젝트 레퍼런스 분석 후 제안 (미정) |

---

## Figma 프로젝트 구조 (참고)

```
{프로젝트명}
├── 01. Design System  ← 컬러/타이포/컴포넌트
├── 02. UX Design      ← User Flow / Wireframe
└── 03. UI Design      ← Hi-fi 시안 (PC + Mobile)
```

---

## 디자인 시스템 설계 방향

### 컬러 팔레트 (프로젝트 레퍼런스 기반)

| 용도 | 색상 | HEX |
|------|------|-----|
| Primary Background | 블랙 | `#000000` |
| Secondary Background | 다크 그레이 | `#111111` |
| Surface / Card | 그레이 | `#1A1A1A` |
| Border | 연한 그레이 | `#2C2C2C` |
| Primary Text | 화이트 | `#FFFFFF` |
| Secondary Text | 연한 그레이 | `#999999` |
| Accent / Point | 화이트 | `#FFFFFF` |
| CTA Button | 화이트 | `#FFFFFF` (텍스트 블랙) |
| Error | 레드 | `#FF3B30` |
| Success | 그린 | `#34C759` |

> 위 컬러는 다크 모드 기반 초안이며 프로젝트에 따라 조정한다. 디자이너 Sub Agent가 최종 확정한다.

### 폰트 추천 후보

| 후보 | 특징 | 적합도 |
|------|------|--------|
| **Pretendard** | 한글/영문 균형, 고딕 계열, 한글 웹 폰트 표준 | ★★★★★ |
| **Noto Sans KR** | 구글 무료, 안정적 | ★★★★ |
| **Apple SD Gothic Neo** | iOS 기본체, 자연스러움 | ★★★ |

> 권장: `Pretendard` (라이선스 무료, 한글/영문 균형)

### 타이포그래피 스케일

| 이름 | 크기 | 굵기 | 용도 |
|------|------|------|------|
| Display | 32px | 700 | 히어로 배너 제목 |
| H1 | 24px | 700 | 페이지 제목 |
| H2 | 20px | 600 | 섹션 제목 |
| H3 | 18px | 600 | 카드 제목 |
| Body1 | 16px | 400 | 본문 |
| Body2 | 14px | 400 | 보조 텍스트 |
| Caption | 12px | 400 | 라벨, 태그 |

### 스페이싱 시스템 (8px Grid)

```
4px  → 극소 간격
8px  → 소 간격
16px → 기본 간격
24px → 중 간격
32px → 대 간격
48px → 섹션 간격
64px → 페이지 섹션 간격
```

---

## 사용자 동선 (User Flow)

### 일반 사용자 동선
```
진입 (메인) → 탐색/검색 → 목록 → 상세
  → [프로젝트 핵심 기능] → 전환(구매/신청 등) → 완료
```

### 관리자 동선
```
관리자 로그인 → 관리자 대시보드 → 콘텐츠 등록/관리 → 운영 관리
```

### 비회원 동선
```
메인 → 콘텐츠 탐색 → 상세 → 로그인/회원가입 유도 → [일부 기능은 로그인 필요]
```

---

## 페이지별 와이어프레임 목록

### Phase 1 - MVP

| 페이지 | 설명 | 우선순위 |
|--------|------|----------|
| 메인(홈) | 배너, 추천 콘텐츠, 주요 섹션 | P0 |
| 카테고리/목록 | 필터, 정렬, 그리드 레이아웃 | P0 |
| 상세 | 정보, 옵션, CTA | P0 |
| 로그인 / 회원가입 | 소셜 로그인 포함 | P0 |
| 장바구니/선택함 (커머스 프로젝트 시) | 수량, 금액, 쿠폰 | P1 |
| 결제/전환 (커머스 프로젝트 시) | 주소, 결제수단, 최종확인 | P1 |
| 완료/내역 | 상태 트래킹 | P1 |
| 마이페이지 | 프로필, 활동내역 | P1 |
| 파트너/조직 페이지 (해당 시) | 소개, 콘텐츠 목록 | P1 |

### Phase 2 - 프로젝트 핵심 기능 A (프로젝트별 정의)

| 페이지 | 설명 |
|--------|------|
| 핵심 기능 A | 프로젝트 시작 시 정의 |

### Phase 3 - 프로젝트 핵심 기능 B (프로젝트별 정의)

| 페이지 | 설명 |
|--------|------|
| 핵심 기능 B | 프로젝트 시작 시 정의 |

---

## 핵심 기능 인터페이스 설계 지침

### 프로젝트 핵심 기능 A UI (프로젝트별 정의)
- 핵심 기능 진입점(CTA 버튼)을 상세 페이지에 배치
- 모달 또는 전체화면 전환 고려
- 로딩 상태 표시 필수

### 프로젝트 핵심 기능 B UI (프로젝트별 정의)
- 핵심 기능 B 진입점(CTA 버튼)을 적절한 페이지에 배치
- 비로그인 시 로그인 유도 모달 (로그인 필요 기능인 경우)
- 단계별 플로우가 있는 경우 온보딩 플로우 설계
- 결과 화면 레이아웃 및 상태 표시 설계

---

## Figma 컴포넌트 라이브러리 구성

```
Components/
├── Foundation
│   ├── Colors        ← 컬러 스와치
│   ├── Typography    ← 폰트 스타일
│   └── Spacing       ← 간격 기준
├── Elements
│   ├── Button        ← Primary, Secondary, Ghost
│   ├── Input         ← Text, Select, Checkbox, Radio
│   ├── Badge / Tag   ← 카테고리, 할인율 등
│   └── Icon          ← 아이콘 세트
├── Components
│   ├── Navbar        ← PC / Mobile 네비게이션
│   ├── ProductCard   ← 상품 카드 (썸네일, 가격, 브랜드)
│   ├── Modal         ← 기본 모달, 3D 뷰어 모달
│   ├── Carousel      ← 배너, 상품 슬라이드
│   └── Pagination    ← 페이지 네비게이션
└── Patterns
    ├── ProductGrid   ← 상품 목록 그리드
    ├── FilterPanel   ← 필터 사이드바
    └── CheckoutFlow  ← 결제 플로우
```

---

## 반응형 브레이크포인트

| 구분 | 범위 | 레이아웃 |
|------|------|----------|
| Mobile | 375px ~ 767px | 1컬럼, 하단 탭바 |
| Desktop | 1280px ~ | 사이드바 + 컨텐츠 영역 |

---

## 레퍼런스 분석 포인트

디자인 작업 전 아래 항목을 프로젝트 레퍼런스에서 직접 확인하고 벤치마킹할 것:

- [ ] 네비게이션 구조 (카테고리 뎁스, 검색 UI)
- [ ] 콘텐츠 카드 레이아웃 (이미지 비율, 정보 계층)
- [ ] 상세 페이지 구성 (정보, 옵션 선택, CTA 위치)
- [ ] 필터/정렬 UI 패턴
- [ ] 메인 배너 및 기획전 레이아웃
- [ ] 전환(장바구니/결제/신청 등) 플로우
- [ ] 모바일 앱 UX 패턴 (하단 탭바 구조)

---

## Manifest 파일 스키마

### manifest.json — 최소 구조

```json
{
  "name": "{프로젝트명} {산출물명}",
  "api": "1.0.0",
  "id": "{project-slug}-{산출물명}",
  "editorType": ["figma"]
}
```

- `id` 네이밍: `{project-slug}-{산출물명}` (예: `my-shop-product-detail-hifi`)
- `api`는 항상 `"1.0.0"` 고정

### manifest.import-data.json — 프레임 스펙 구조

```json
{
  "createdAt": "YYYY-MM-DD",
  "project": "project-slug",
  "sprint": N,
  "targetFile": "03. UI Design — ...",
  "frames": [
    {
      "name": "Hero Section - PC (1440px)",
      "width": 1440,
      "height": 900,
      "category": "hero",
      "layout": "2컬럼, 좌측 텍스트 우측 이미지",
      "components": ["Button", "Badge", "Image"],
      "spacing": "gap-6, px-8, py-16",
      "typography": "h1 제목, body1 설명",
      "responsive": "모바일 1컬럼 스택, 이미지 상단"
    }
  ],
  "designTokens": {
    "colors": {},
    "typography": {},
    "spacing": {},
    "borderRadius": {}
  }
}
```

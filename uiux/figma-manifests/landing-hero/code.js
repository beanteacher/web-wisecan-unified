(async () => {
  // ── Helpers ──────────────────────────────────────────────────────────────
  function hexToRgb(hex) {
    return {
      r: parseInt(hex.slice(1, 3), 16) / 255,
      g: parseInt(hex.slice(3, 5), 16) / 255,
      b: parseInt(hex.slice(5, 7), 16) / 255,
    };
  }
  function solid(hex) { return [{ type: 'SOLID', color: hexToRgb(hex) }]; }

  await Promise.all([
    figma.loadFontAsync({ family: 'Inter', style: 'Regular' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Medium' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Extra Bold' }),
  ]);

  const page = figma.currentPage;
  page.name = 'Landing - Hero';

  // ── Shared builders ──────────────────────────────────────────────────────
  function addRect(parent, { name, x, y, w, h, fill, radius, strokeColor, strokeWeight }) {
    const r = figma.createRectangle();
    r.name = name || 'rect';
    r.x = x; r.y = y;
    r.resize(w, h);
    r.fills = solid(fill);
    if (radius) r.cornerRadius = radius;
    if (strokeColor) {
      r.strokes = solid(strokeColor);
      r.strokeWeight = strokeWeight || 1;
    }
    parent.appendChild(r);
    return r;
  }

  function addText(parent, { x, y, text, size, style, color, w, align }) {
    const t = figma.createText();
    t.x = x; t.y = y;
    t.fontName = { family: 'Inter', style: style || 'Regular' };
    t.fontSize = size || 16;
    t.fills = solid(color || '#0F172A');
    t.characters = text;
    if (w) { t.resize(w, t.height); t.textAutoResize = 'HEIGHT'; }
    if (align) t.textAlignHorizontal = align;
    parent.appendChild(t);
    return t;
  }

  function makeFrame(name, w, h, x, y, bg) {
    const f = figma.createFrame();
    f.name = name;
    f.resize(w, h);
    f.x = x; f.y = y;
    f.fills = solid(bg);
    page.appendChild(f);
    return f;
  }

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 1: Landing Hero - PC (1440 x 1080)
  // ════════════════════════════════════════════════════════════════════════
  const hero = makeFrame('Landing Hero - PC (1440px)', 1440, 1080, 0, 0, '#FFFFFF');

  // ── Top Navigation Bar ─────────────────────────────────────────────────
  addRect(hero, { name: 'nav-bg', x: 0, y: 0, w: 1440, h: 64, fill: '#FFFFFF', strokeColor: '#F1F5F9', strokeWeight: 1 });
  addText(hero, { x: 120, y: 20, text: 'Wisecan', size: 20, style: 'Bold', color: '#0F172A' });
  // Nav links
  addText(hero, { x: 560, y: 22, text: '기능', size: 14, style: 'Medium', color: '#64748B' });
  addText(hero, { x: 620, y: 22, text: '요금', size: 14, style: 'Medium', color: '#64748B' });
  addText(hero, { x: 680, y: 22, text: 'API 문서', size: 14, style: 'Medium', color: '#64748B' });
  addText(hero, { x: 760, y: 22, text: '고객사례', size: 14, style: 'Medium', color: '#64748B' });
  // Nav buttons
  addRect(hero, { name: 'btn-login', x: 1180, y: 16, w: 72, h: 32, fill: '#FFFFFF', radius: 6, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(hero, { x: 1195, y: 23, text: '로그인', size: 13, style: 'Medium', color: '#0F172A' });
  addRect(hero, { name: 'btn-start', x: 1264, y: 16, w: 96, h: 32, fill: '#2563EB', radius: 6 });
  addText(hero, { x: 1278, y: 23, text: '시작하기', size: 13, style: 'Medium', color: '#FFFFFF' });

  // ── Hero Section ───────────────────────────────────────────────────────
  const CX = 720; // center x

  // Badge pill [motion:fadeInDown, 0s]
  const badgeW = 180;
  addRect(hero, { name: '[motion:fadeInDown,0s] badge-bg', x: CX - badgeW / 2, y: 140, w: badgeW, h: 32, fill: '#EFF6FF', radius: 16, strokeColor: '#BFDBFE', strokeWeight: 1 });
  addText(hero, { x: CX - 65, y: 147, text: '통합 메시징 플랫폼', size: 13, style: 'Medium', color: '#2563EB' });

  // Main headline [motion:fadeInUp, 순차]
  addText(hero, { x: CX - 360, y: 200, text: '문자, 알림톡, RCS.', size: 48, style: 'Extra Bold', color: '#0F172A', w: 720, align: 'CENTER' });
  const h1 = hero.children[hero.children.length - 1]; h1.name = '[motion:fadeInUp,0.15s] headline-line1';

  addText(hero, { x: CX - 360, y: 264, text: '하나의 API로 모두 보내세요.', size: 48, style: 'Extra Bold', color: '#2563EB', w: 720, align: 'CENTER' });
  const h2 = hero.children[hero.children.length - 1]; h2.name = '[motion:fadeInUp,0.3s] headline-line2';

  // Sub copy [motion:fadeIn, 0.5s]
  addText(hero, { x: CX - 320, y: 340, text: 'SMS, MMS, 카카오 알림톡, RCS까지 — 통합 발송과 실시간 통계를 한 곳에서', size: 18, style: 'Regular', color: '#64748B', w: 640, align: 'CENTER' });
  const sub = hero.children[hero.children.length - 1]; sub.name = '[motion:fadeIn,0.5s] sub-copy';

  // CTA buttons [motion:fadeInUp, 0.65s] [motion:hover scale(1.03)]
  const ctaY = 410;
  // Primary CTA
  addRect(hero, { name: '[motion:fadeInUp,0.65s][hover:scale1.03+shadow] cta-primary', x: CX - 170, y: ctaY, w: 160, h: 48, fill: '#2563EB', radius: 8 });
  addText(hero, { x: CX - 135, y: ctaY + 15, text: '무료로 시작하기', size: 15, style: 'Semi Bold', color: '#FFFFFF' });
  // Secondary CTA
  addRect(hero, { name: '[motion:fadeInUp,0.65s][hover:scale1.03] cta-secondary', x: CX + 10, y: ctaY, w: 160, h: 48, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(hero, { x: CX + 42, y: ctaY + 15, text: 'API 문서 보기', size: 15, style: 'Semi Bold', color: '#0F172A' });

  // Trust indicators [motion:staggerFadeIn, 0.85s, 0.08s간격]
  const trustY = 486;
  const trustItems = ['4채널 통합 발송', '실시간 발송 통계', '5분 만에 API 연동', '발송 결과 자동 분석'];
  let trustX = CX - 340;
  for (let ti = 0; ti < trustItems.length; ti++) {
    const item = trustItems[ti];
    addRect(hero, { name: `[motion:staggerFadeIn,${0.85 + ti * 0.08}s] check-bg-${ti}`, x: trustX, y: trustY, w: 20, h: 20, fill: '#F0FDF4', radius: 10 });
    addText(hero, { x: trustX + 5, y: trustY + 3, text: '✓', size: 12, style: 'Bold', color: '#22C55E' });
    addText(hero, { x: trustX + 26, y: trustY + 2, text: item, size: 13, style: 'Regular', color: '#64748B' });
    trustX += 170;
  }

  // ── Divider ────────────────────────────────────────────────────────────
  addRect(hero, { name: 'divider', x: 120, y: 540, w: 1200, h: 1, fill: '#F1F5F9' });

  // ── Channel Icons Row ──────────────────────────────────────────────────
  const chY = 572;
  addText(hero, { x: CX - 80, y: chY, text: '지원 채널', size: 13, style: 'Semi Bold', color: '#94A3B8', w: 160, align: 'CENTER' });

  const channels = [
    { name: 'SMS', desc: '단문 문자', color: '#2563EB', bgColor: '#EFF6FF' },
    { name: 'MMS', desc: '장문/이미지', color: '#7C3AED', bgColor: '#F5F3FF' },
    { name: '카카오 알림톡', desc: '카카오톡 알림', color: '#F59E0B', bgColor: '#FFFBEB' },
    { name: 'RCS', desc: '리치 메시지', color: '#059669', bgColor: '#ECFDF5' },
  ];

  const chCardW = 240;
  const chGap = 24;
  const chTotalW = channels.length * chCardW + (channels.length - 1) * chGap;
  let chX = CX - chTotalW / 2;
  const chCardY = chY + 32;

  for (const ch of channels) {
    // Card background [motion:onScroll,staggerSlideUp] [hover:liftY-4+shadow]
    addRect(hero, { name: `[motion:onScroll,staggerSlideUp,0.1s][hover:liftY-4] ch-${ch.name}`, x: chX, y: chCardY, w: chCardW, h: 80, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
    // Icon circle
    addRect(hero, { name: `ch-icon-${ch.name}`, x: chX + 16, y: chCardY + 20, w: 40, h: 40, fill: ch.bgColor, radius: 20 });
    addText(hero, { x: chX + 24, y: chCardY + 28, text: ch.name.slice(0, 2), size: 16, style: 'Bold', color: ch.color });
    // Text
    addText(hero, { x: chX + 68, y: chCardY + 22, text: ch.name, size: 15, style: 'Semi Bold', color: '#0F172A' });
    addText(hero, { x: chX + 68, y: chCardY + 44, text: ch.desc, size: 13, style: 'Regular', color: '#64748B' });
    chX += chCardW + chGap;
  }

  // ── Feature Cards (4개) ────────────────────────────────────────────────
  const featY = 720;
  addText(hero, { x: CX - 80, y: featY, text: '핵심 기능', size: 13, style: 'Semi Bold', color: '#94A3B8', w: 160, align: 'CENTER' });

  const features = [
    { icon: '📨', title: '통합 발송', desc: 'SMS, MMS, 카카오 알림톡,\nRCS를 하나의 API로 발송', color: '#2563EB' },
    { icon: '📊', title: '실시간 통계', desc: '발송 현황, 성공률, 채널별\n통계를 대시보드에서 확인', color: '#7C3AED' },
    { icon: '🔑', title: 'API Key 관리', desc: 'API Key 발급·관리·사용량\n추적을 한 곳에서', color: '#059669' },
    { icon: '📄', title: '파일 변환', desc: 'Markdown을 DOCX, PDF로\n간편하게 변환', color: '#F59E0B' },
  ];

  const fCardW = 264;
  const fGap = 24;
  const fTotalW = features.length * fCardW + (features.length - 1) * fGap;
  let fX = CX - fTotalW / 2;
  const fCardY = featY + 32;

  for (const feat of features) {
    // Card [motion:onScroll,staggerSlideUp] [hover:liftY-4+borderBlue]
    addRect(hero, { name: `[motion:onScroll,staggerSlideUp,0.1s][hover:liftY-4+border] feat-${feat.title}`, x: fX, y: fCardY, w: fCardW, h: 200, fill: '#FFFFFF', radius: 16, strokeColor: '#E2E8F0', strokeWeight: 1 });
    // Icon circle
    addRect(hero, { name: `feat-icon-${feat.title}`, x: fX + 24, y: fCardY + 24, w: 48, h: 48, fill: '#F8FAFC', radius: 12 });
    addText(hero, { x: fX + 36, y: fCardY + 36, text: feat.icon, size: 22, style: 'Regular', color: '#0F172A' });
    // Title
    addText(hero, { x: fX + 24, y: fCardY + 92, text: feat.title, size: 18, style: 'Bold', color: '#0F172A' });
    // Top accent bar
    addRect(hero, { name: `feat-bar-${feat.title}`, x: fX + 24, y: fCardY + 84, w: 32, h: 3, fill: feat.color, radius: 2 });
    // Description
    addText(hero, { x: fX + 24, y: fCardY + 120, text: feat.desc, size: 14, style: 'Regular', color: '#64748B', w: fCardW - 48 });
    fX += fCardW + fGap;
  }

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 2: Landing Hero - Mobile (375 x 1200)
  // ════════════════════════════════════════════════════════════════════════
  const heroM = makeFrame('Landing Hero - Mobile (375px)', 375, 1400, 1640, 0, '#FFFFFF');

  // ── Mobile Nav ─────────────────────────────────────────────────────────
  addRect(heroM, { name: 'nav-bg-m', x: 0, y: 0, w: 375, h: 56, fill: '#FFFFFF', strokeColor: '#F1F5F9', strokeWeight: 1 });
  addText(heroM, { x: 20, y: 18, text: 'Wisecan', size: 18, style: 'Bold', color: '#0F172A' });
  // Hamburger icon (3 lines)
  addRect(heroM, { name: 'hamburger-1', x: 335, y: 22, w: 20, h: 2, fill: '#64748B', radius: 1 });
  addRect(heroM, { name: 'hamburger-2', x: 335, y: 28, w: 20, h: 2, fill: '#64748B', radius: 1 });
  addRect(heroM, { name: 'hamburger-3', x: 335, y: 34, w: 20, h: 2, fill: '#64748B', radius: 1 });

  // ── Mobile Hero ────────────────────────────────────────────────────────
  const mCX = 187; // mobile center

  // Badge
  const mBadgeW = 160;
  addRect(heroM, { name: 'badge-m', x: mCX - mBadgeW / 2, y: 88, w: mBadgeW, h: 28, fill: '#EFF6FF', radius: 14, strokeColor: '#BFDBFE', strokeWeight: 1 });
  addText(heroM, { x: mCX - 55, y: 94, text: '통합 메시징 플랫폼', size: 12, style: 'Medium', color: '#2563EB' });

  // Headline
  addText(heroM, { x: 20, y: 136, text: '문자, 알림톡, RCS.', size: 28, style: 'Extra Bold', color: '#0F172A', w: 335, align: 'CENTER' });
  addText(heroM, { x: 20, y: 176, text: '하나의 API로\n모두 보내세요.', size: 28, style: 'Extra Bold', color: '#2563EB', w: 335, align: 'CENTER' });

  // Sub copy
  addText(heroM, { x: 20, y: 260, text: 'SMS, MMS, 카카오 알림톡, RCS까지\n통합 발송과 실시간 통계를 한 곳에서', size: 15, style: 'Regular', color: '#64748B', w: 335, align: 'CENTER' });

  // CTAs stacked
  const mCtaY = 330;
  addRect(heroM, { name: 'cta-primary-m', x: 20, y: mCtaY, w: 335, h: 48, fill: '#2563EB', radius: 8 });
  addText(heroM, { x: mCX - 45, y: mCtaY + 15, text: '무료로 시작하기', size: 15, style: 'Semi Bold', color: '#FFFFFF' });
  addRect(heroM, { name: 'cta-secondary-m', x: 20, y: mCtaY + 60, w: 335, h: 48, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(heroM, { x: mCX - 40, y: mCtaY + 75, text: 'API 문서 보기', size: 15, style: 'Semi Bold', color: '#0F172A' });

  // Trust indicators - 2x2 grid
  const mTrustY = mCtaY + 132;
  const mTrustItems = ['4채널 통합 발송', '실시간 발송 통계', '5분 만에 API 연동', '발송 결과 자동 분석'];
  for (let i = 0; i < mTrustItems.length; i++) {
    const tx = 20 + (i % 2) * 170;
    const ty = mTrustY + Math.floor(i / 2) * 28;
    addRect(heroM, { name: 'check-m', x: tx, y: ty, w: 16, h: 16, fill: '#F0FDF4', radius: 8 });
    addText(heroM, { x: tx + 4, y: ty + 2, text: '✓', size: 10, style: 'Bold', color: '#22C55E' });
    addText(heroM, { x: tx + 22, y: ty + 1, text: mTrustItems[i], size: 12, style: 'Regular', color: '#64748B' });
  }

  // ── Mobile Divider ─────────────────────────────────────────────────────
  addRect(heroM, { name: 'divider-m', x: 20, y: mTrustY + 72, w: 335, h: 1, fill: '#F1F5F9' });

  // ── Mobile Channel Cards (2x2 grid) ────────────────────────────────────
  const mChY = mTrustY + 96;
  addText(heroM, { x: 20, y: mChY, text: '지원 채널', size: 12, style: 'Semi Bold', color: '#94A3B8' });

  const mChCardW = 160;
  const mChGap = 15;
  const mChCardY = mChY + 28;

  for (let i = 0; i < channels.length; i++) {
    const ch = channels[i];
    const cx = 20 + (i % 2) * (mChCardW + mChGap);
    const cy = mChCardY + Math.floor(i / 2) * (72 + 12);

    addRect(heroM, { name: `mch-${ch.name}`, x: cx, y: cy, w: mChCardW, h: 72, fill: '#FFFFFF', radius: 10, strokeColor: '#E2E8F0', strokeWeight: 1 });
    addRect(heroM, { name: `mch-icon-${ch.name}`, x: cx + 12, y: cy + 16, w: 36, h: 36, fill: ch.bgColor, radius: 18 });
    addText(heroM, { x: cx + 19, y: cy + 23, text: ch.name.slice(0, 2), size: 14, style: 'Bold', color: ch.color });
    addText(heroM, { x: cx + 56, y: cy + 18, text: ch.name, size: 13, style: 'Semi Bold', color: '#0F172A' });
    addText(heroM, { x: cx + 56, y: cy + 38, text: ch.desc, size: 11, style: 'Regular', color: '#64748B' });
  }

  // ── Mobile Feature Cards (1 column stack) ──────────────────────────────
  const mFeatY = mChCardY + 2 * 84 + 24;
  addText(heroM, { x: 20, y: mFeatY, text: '핵심 기능', size: 12, style: 'Semi Bold', color: '#94A3B8' });

  let mfY = mFeatY + 28;
  for (const feat of features) {
    addRect(heroM, { name: `mfeat-${feat.title}`, x: 20, y: mfY, w: 335, h: 96, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
    // Icon
    addRect(heroM, { name: `mfeat-icon-${feat.title}`, x: 36, y: mfY + 24, w: 44, h: 44, fill: '#F8FAFC', radius: 10 });
    addText(heroM, { x: 47, y: mfY + 34, text: feat.icon, size: 20, style: 'Regular', color: '#0F172A' });
    // Accent bar
    addRect(heroM, { name: `mfeat-bar-${feat.title}`, x: 96, y: mfY + 24, w: 28, h: 3, fill: feat.color, radius: 2 });
    // Title + desc
    addText(heroM, { x: 96, y: mfY + 32, text: feat.title, size: 15, style: 'Bold', color: '#0F172A' });
    addText(heroM, { x: 96, y: mfY + 56, text: feat.desc.replace('\n', ' '), size: 12, style: 'Regular', color: '#64748B', w: 240 });
    mfY += 108;
  }

  // ════════════════════════════════════════════════════════════════════════
  // PRESENTATION STEPS — Present 모드에서 클릭하며 애니메이션 단계 확인
  // 각 스텝은 이전 스텝 + 새 요소가 등장한 상태
  // ════════════════════════════════════════════════════════════════════════

  const STEP_W = 1440;
  const STEP_H = 1080;
  const STEP_Y = 1300; // 메인 프레임 아래에 배치

  // 스텝별 가시성 정의
  const steps = [
    { name: 'Step 0 — 초기 상태', label: '페이지 로드 직후. 모든 요소 숨김, Nav만 표시', show: [] },
    { name: 'Step 1 — 뱃지 등장', label: '0.0s | fadeInDown | 위에서 아래로 내려오며 나타남', show: ['badge'] },
    { name: 'Step 2 — 헤드라인 1줄', label: '0.15s | fadeInUp | 아래에서 위로 올라오며 나타남', show: ['badge', 'h1'] },
    { name: 'Step 3 — 헤드라인 2줄 (Blue)', label: '0.3s | fadeInUp | 아래에서 위로, 블루 강조색', show: ['badge', 'h1', 'h2'] },
    { name: 'Step 4 — 서브 카피', label: '0.5s | fadeIn | 투명도만 0→1', show: ['badge', 'h1', 'h2', 'sub'] },
    { name: 'Step 5 — CTA 버튼', label: '0.65s | fadeInUp | 아래에서 위로 + hover: scale(1.03)', show: ['badge', 'h1', 'h2', 'sub', 'cta'] },
    { name: 'Step 6 — 신뢰 지표', label: '0.85s | stagger fadeIn | 0.08s 간격으로 순차 등장', show: ['badge', 'h1', 'h2', 'sub', 'cta', 'trust'] },
    { name: 'Step 7 — 채널 카드 (스크롤)', label: 'onScroll | stagger slideUp | 뷰포트 30% 진입 시 + hover: liftY(-4px)', show: ['badge', 'h1', 'h2', 'sub', 'cta', 'trust', 'channels'] },
    { name: 'Step 8 — 기능 카드 (스크롤)', label: 'onScroll | stagger slideUp | 뷰포트 30% 진입 시 + hover: liftY(-4px) + border blue', show: ['badge', 'h1', 'h2', 'sub', 'cta', 'trust', 'channels', 'features'] },
  ];

  for (let si = 0; si < steps.length; si++) {
    const step = steps[si];
    const sx = si * (STEP_W + 100);
    const f = makeFrame(step.name, STEP_W, STEP_H, sx, STEP_Y, '#FFFFFF');
    const vis = new Set(step.show);

    // ── 스텝 라벨 바 (상단) ──────────────────────────────────────────
    addRect(f, { name: 'step-label-bg', x: 0, y: 0, w: STEP_W, h: 44, fill: '#0F172A' });
    addText(f, { x: 16, y: 12, text: `${si}/8`, size: 14, style: 'Bold', color: '#FFFFFF' });
    addText(f, { x: 56, y: 12, text: step.label, size: 14, style: 'Regular', color: '#94A3B8' });
    // 우측 안내
    if (si < steps.length - 1) {
      addText(f, { x: STEP_W - 160, y: 12, text: '클릭하여 다음 →', size: 13, style: 'Medium', color: '#3B82F6' });
    } else {
      addText(f, { x: STEP_W - 100, y: 12, text: '최종 상태', size: 13, style: 'Bold', color: '#22C55E' });
    }

    const oY = 44; // offset for label bar

    // ── Nav (항상 표시) ──────────────────────────────────────────────
    addRect(f, { name: 'nav', x: 0, y: oY, w: 1440, h: 64, fill: '#FFFFFF', strokeColor: '#F1F5F9', strokeWeight: 1 });
    addText(f, { x: 120, y: oY + 20, text: 'Wisecan', size: 20, style: 'Bold', color: '#0F172A' });
    addText(f, { x: 560, y: oY + 22, text: '기능', size: 14, style: 'Medium', color: '#64748B' });
    addText(f, { x: 620, y: oY + 22, text: '요금', size: 14, style: 'Medium', color: '#64748B' });
    addText(f, { x: 680, y: oY + 22, text: 'API 문서', size: 14, style: 'Medium', color: '#64748B' });
    addRect(f, { name: 'btn-login', x: 1180, y: oY + 16, w: 72, h: 32, fill: '#FFFFFF', radius: 6, strokeColor: '#E2E8F0', strokeWeight: 1 });
    addText(f, { x: 1195, y: oY + 23, text: '로그인', size: 13, style: 'Medium', color: '#0F172A' });
    addRect(f, { name: 'btn-start', x: 1264, y: oY + 16, w: 96, h: 32, fill: '#2563EB', radius: 6 });
    addText(f, { x: 1278, y: oY + 23, text: '시작하기', size: 13, style: 'Medium', color: '#FFFFFF' });

    // ── Badge ────────────────────────────────────────────────────────
    const badgeOp = vis.has('badge') ? 1 : 0.08;
    const badgeOffY = vis.has('badge') ? 0 : -16;
    const bRect = addRect(f, { name: 'badge', x: CX - 90, y: oY + 96 + badgeOffY, w: 180, h: 32, fill: '#EFF6FF', radius: 16, strokeColor: '#BFDBFE', strokeWeight: 1 });
    bRect.opacity = badgeOp;
    const bTxt = addText(f, { x: CX - 65, y: oY + 103 + badgeOffY, text: '통합 메시징 플랫폼', size: 13, style: 'Medium', color: '#2563EB' });
    bTxt.opacity = badgeOp;

    // ── Headline 1 ───────────────────────────────────────────────────
    const h1Op = vis.has('h1') ? 1 : 0.08;
    const h1Off = vis.has('h1') ? 0 : 24;
    const h1T = addText(f, { x: CX - 360, y: oY + 156 + h1Off, text: '문자, 알림톡, RCS.', size: 48, style: 'Extra Bold', color: '#0F172A', w: 720, align: 'CENTER' });
    h1T.opacity = h1Op;

    // ── Headline 2 ───────────────────────────────────────────────────
    const h2Op = vis.has('h2') ? 1 : 0.08;
    const h2Off = vis.has('h2') ? 0 : 24;
    const h2T = addText(f, { x: CX - 360, y: oY + 220 + h2Off, text: '하나의 API로 모두 보내세요.', size: 48, style: 'Extra Bold', color: '#2563EB', w: 720, align: 'CENTER' });
    h2T.opacity = h2Op;

    // ── Sub copy ─────────────────────────────────────────────────────
    const subOp = vis.has('sub') ? 1 : 0.08;
    const subT = addText(f, { x: CX - 320, y: oY + 296, text: 'SMS, MMS, 카카오 알림톡, RCS까지 — 통합 발송과 실시간 통계를 한 곳에서', size: 18, style: 'Regular', color: '#64748B', w: 640, align: 'CENTER' });
    subT.opacity = subOp;

    // ── CTA ──────────────────────────────────────────────────────────
    const ctOp = vis.has('cta') ? 1 : 0.08;
    const ctOff = vis.has('cta') ? 0 : 16;
    const sCtaY = oY + 366 + ctOff;
    const c1 = addRect(f, { name: 'cta1', x: CX - 170, y: sCtaY, w: 160, h: 48, fill: '#2563EB', radius: 8 });
    c1.opacity = ctOp;
    const c1t = addText(f, { x: CX - 135, y: sCtaY + 15, text: '무료로 시작하기', size: 15, style: 'Semi Bold', color: '#FFFFFF' });
    c1t.opacity = ctOp;
    const c2 = addRect(f, { name: 'cta2', x: CX + 10, y: sCtaY, w: 160, h: 48, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
    c2.opacity = ctOp;
    const c2t = addText(f, { x: CX + 42, y: sCtaY + 15, text: 'API 문서 보기', size: 15, style: 'Semi Bold', color: '#0F172A' });
    c2t.opacity = ctOp;

    // ── Trust indicators ─────────────────────────────────────────────
    const trOp = vis.has('trust') ? 1 : 0.08;
    let sTrustX = CX - 340;
    for (const item of trustItems) {
      const tr1 = addRect(f, { name: 'trust-check', x: sTrustX, y: oY + 442, w: 20, h: 20, fill: '#F0FDF4', radius: 10 });
      tr1.opacity = trOp;
      const tr1t = addText(f, { x: sTrustX + 5, y: oY + 445, text: '✓', size: 12, style: 'Bold', color: '#22C55E' });
      tr1t.opacity = trOp;
      const tr2 = addText(f, { x: sTrustX + 26, y: oY + 444, text: item, size: 13, style: 'Regular', color: '#64748B' });
      tr2.opacity = trOp;
      sTrustX += 170;
    }

    // ── Divider ──────────────────────────────────────────────────────
    addRect(f, { name: 'div', x: 120, y: oY + 496, w: 1200, h: 1, fill: '#F1F5F9' });

    // ── Channel cards ────────────────────────────────────────────────
    const chOp = vis.has('channels') ? 1 : 0.08;
    const chOff = vis.has('channels') ? 0 : 32;
    addText(f, { x: CX - 80, y: oY + 528, text: '지원 채널', size: 13, style: 'Semi Bold', color: '#94A3B8', w: 160, align: 'CENTER' });
    let sChX = CX - chTotalW / 2;
    for (const ch of channels) {
      const card = addRect(f, { name: `ch-${ch.name}`, x: sChX, y: oY + 560 + chOff, w: chCardW, h: 80, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
      card.opacity = chOp;
      const icon = addRect(f, { name: `ch-icon`, x: sChX + 16, y: oY + 580 + chOff, w: 40, h: 40, fill: ch.bgColor, radius: 20 });
      icon.opacity = chOp;
      const ic = addText(f, { x: sChX + 24, y: oY + 588 + chOff, text: ch.name.slice(0, 2), size: 16, style: 'Bold', color: ch.color });
      ic.opacity = chOp;
      const nm = addText(f, { x: sChX + 68, y: oY + 582 + chOff, text: ch.name, size: 15, style: 'Semi Bold', color: '#0F172A' });
      nm.opacity = chOp;
      const ds = addText(f, { x: sChX + 68, y: oY + 604 + chOff, text: ch.desc, size: 13, style: 'Regular', color: '#64748B' });
      ds.opacity = chOp;
      sChX += chCardW + chGap;
    }

    // ── Feature cards ────────────────────────────────────────────────
    const ftOp = vis.has('features') ? 1 : 0.08;
    const ftOff = vis.has('features') ? 0 : 32;
    addText(f, { x: CX - 80, y: oY + 676, text: '핵심 기능', size: 13, style: 'Semi Bold', color: '#94A3B8', w: 160, align: 'CENTER' });
    let sFX = CX - fTotalW / 2;
    for (const feat of features) {
      const fc = addRect(f, { name: `feat-${feat.title}`, x: sFX, y: oY + 708 + ftOff, w: fCardW, h: 200, fill: '#FFFFFF', radius: 16, strokeColor: '#E2E8F0', strokeWeight: 1 });
      fc.opacity = ftOp;
      const fi = addRect(f, { name: 'feat-icon', x: sFX + 24, y: oY + 732 + ftOff, w: 48, h: 48, fill: '#F8FAFC', radius: 12 });
      fi.opacity = ftOp;
      const fie = addText(f, { x: sFX + 36, y: oY + 744 + ftOff, text: feat.icon, size: 22, style: 'Regular', color: '#0F172A' });
      fie.opacity = ftOp;
      const fbar = addRect(f, { name: 'feat-bar', x: sFX + 24, y: oY + 792 + ftOff, w: 32, h: 3, fill: feat.color, radius: 2 });
      fbar.opacity = ftOp;
      const ftt = addText(f, { x: sFX + 24, y: oY + 800 + ftOff, text: feat.title, size: 18, style: 'Bold', color: '#0F172A' });
      ftt.opacity = ftOp;
      const ftd = addText(f, { x: sFX + 24, y: oY + 828 + ftOff, text: feat.desc, size: 14, style: 'Regular', color: '#64748B', w: fCardW - 48 });
      ftd.opacity = ftOp;
      sFX += fCardW + fGap;
    }

    // ── 새로 등장하는 요소 하이라이트 (현재 스텝에서 추가된 것) ────────
    if (si > 0) {
      const prev = new Set(steps[si - 1].show);
      const newItems = step.show.filter(s => !prev.has(s));
      if (newItems.length > 0) {
        // 우하단에 "NEW" 뱃지
        addRect(f, { name: 'new-badge', x: STEP_W - 200, y: STEP_H - 40, w: 184, h: 28, fill: '#DCFCE7', radius: 14 });
        addText(f, { x: STEP_W - 190, y: STEP_H - 34, text: 'NEW: ' + newItems.join(', '), size: 12, style: 'Semi Bold', color: '#16A34A' });
      }
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  figma.viewport.scrollAndZoomIntoView(page.children);
  figma.closePlugin('Landing Hero — PC + Mobile + 프레젠테이션 9스텝 생성 완료');
})();


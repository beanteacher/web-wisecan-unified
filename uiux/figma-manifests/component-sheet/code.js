(async () => {
  // ── Helpers ───────────────────────────────────────────────────────────────
  function hexToRgb(hex) {
    const r = parseInt(hex.slice(1, 3), 16) / 255;
    const g = parseInt(hex.slice(3, 5), 16) / 255;
    const b = parseInt(hex.slice(5, 7), 16) / 255;
    return { r, g, b };
  }

  function solid(hex) {
    return [{ type: 'SOLID', color: hexToRgb(hex) }];
  }

  function solidAlpha(hex, opacity) {
    return [{ type: 'SOLID', color: hexToRgb(hex), opacity }];
  }

  function addRect(parent, opts) {
    const r = figma.createRectangle();
    r.name = opts.name || 'rect';
    r.x = opts.x;
    r.y = opts.y;
    r.resize(opts.w, opts.h);
    r.fills = opts.fill ? solid(opts.fill) : [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    if (opts.radius !== undefined) r.cornerRadius = opts.radius;
    if (opts.strokeColor) {
      r.strokes = [{ type: 'SOLID', color: hexToRgb(opts.strokeColor) }];
      r.strokeWeight = opts.strokeWeight || 1;
      r.strokeAlign = 'INSIDE';
    }
    if (opts.opacity !== undefined) r.opacity = opts.opacity;
    parent.appendChild(r);
    return r;
  }

  function addText(parent, opts) {
    const t = figma.createText();
    t.x = opts.x;
    t.y = opts.y;
    t.characters = opts.text;
    t.fontSize = opts.size || 14;
    t.fontName = { family: 'Inter', style: opts.style || 'Regular' };
    t.fills = solid(opts.color || '#0F172A');
    if (opts.w) { t.textAutoResize = 'HEIGHT'; t.resize(opts.w, t.height); }
    if (opts.align) t.textAlignHorizontal = opts.align;
    parent.appendChild(t);
    return t;
  }

  function makeFrame(parent, opts) {
    const f = figma.createFrame();
    f.name = opts.name || 'frame';
    f.x = opts.x || 0;
    f.y = opts.y || 0;
    f.resize(opts.w, opts.h);
    f.fills = opts.fill ? solid(opts.fill) : [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    if (opts.radius !== undefined) f.cornerRadius = opts.radius;
    if (opts.strokeColor) {
      f.strokes = [{ type: 'SOLID', color: hexToRgb(opts.strokeColor) }];
      f.strokeWeight = opts.strokeWeight || 1;
      f.strokeAlign = 'INSIDE';
    }
    if (opts.clipsContent !== undefined) f.clipsContent = opts.clipsContent;
    parent.appendChild(f);
    return f;
  }

  function addDivider(parent, x, y, w) {
    addRect(parent, { name: 'divider', x, y, w, h: 2, fill: '#E2E8F0', radius: 0 });
  }

  function sectionHeader(parent, x, y, label, frameW) {
    addText(parent, { x, y, text: label, size: 30, style: 'Bold', color: '#0F172A' });
    addDivider(parent, x, y + 44, frameW - x * 2);
    return y + 64;
  }

  // ── Load fonts ─────────────────────────────────────────────────────────────
  await Promise.all([
    figma.loadFontAsync({ family: 'Inter', style: 'Regular' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Medium' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Extra Bold' }),
  ]);

  // ── Main frame ─────────────────────────────────────────────────────────────
  const FRAME_W = 2400;
  const FRAME_H = 6000;
  const PAD_X = 80;
  const SEC_GAP = 80;

  const page = figma.currentPage;
  page.name = 'Component Sheet';

  const main = figma.createFrame();
  main.name = 'Component Sheet - PC (2400 x 6000)';
  main.resize(FRAME_W, FRAME_H);
  main.fills = solid('#FFFFFF');
  main.x = 0;
  main.y = 0;
  page.appendChild(main);

  let Y = 64;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 1 — Buttons  (y: ~0)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Buttons', FRAME_W);

  // Row 1: Variants
  addText(main, { x: PAD_X, y: Y, text: 'VARIANTS', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const btnVariants = [
    { label: 'API Key 발급', bg: '#2563EB', tc: '#FFFFFF', stroke: null,      name: 'Primary'     },
    { label: '내보내기',     bg: '#FFFFFF',  tc: '#0F172A', stroke: '#E2E8F0', name: 'Secondary'   },
    { label: '취소',         bg: '#FFFFFF',  tc: '#64748B', stroke: '#E2E8F0', name: 'Ghost'       },
    { label: '키 비활성화',  bg: '#EF4444',  tc: '#FFFFFF', stroke: null,      name: 'Destructive' },
  ];

  let bx = PAD_X;
  for (const b of btnVariants) {
    const bw = b.label.length * 9 + 32;
    addRect(main, { name: b.name, x: bx, y: Y, w: bw, h: 40, fill: b.bg, radius: 8, strokeColor: b.stroke });
    // 텍스트 중앙 정렬
    addText(main, { x: bx, y: Y + 12, text: b.label, size: 14, style: 'Medium', color: b.tc, w: bw, align: 'CENTER' });
    addText(main, { x: bx, y: Y + 48, text: b.name, size: 11, style: 'Regular', color: '#94A3B8' });
    bx += bw + 32;
  }
  Y += 40 + 36;

  // Row 2: Sizes
  addText(main, { x: PAD_X, y: Y, text: 'SIZES', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const btnSizes = [
    { label: 'Small',  h: 32, px: 12, fs: 13 },
    { label: 'Medium', h: 40, px: 16, fs: 14 },
    { label: 'Large',  h: 48, px: 24, fs: 16 },
  ];
  bx = PAD_X;
  for (const s of btnSizes) {
    const bw = s.label.length * s.fs * 0.65 + s.px * 2;
    addRect(main, { name: `btn-${s.label}`, x: bx, y: Y, w: bw, h: s.h, fill: '#2563EB', radius: 8 });
    // 텍스트 중앙 정렬
    addText(main, { x: bx, y: Y + (s.h - s.fs) / 2, text: s.label, size: s.fs, style: 'Medium', color: '#FFFFFF', w: bw, align: 'CENTER' });
    addText(main, { x: bx, y: Y + s.h + 8, text: `h:${s.h}px`, size: 11, style: 'Regular', color: '#94A3B8' });
    bx += bw + 32;
  }
  Y += 48 + 36;

  // Row 3: States
  addText(main, { x: PAD_X, y: Y, text: 'STATES', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const btnStates = [
    { label: '로그인', bg: '#2563EB', tc: '#FFFFFF', name: 'Default',  opacity: 1   },
    { label: '로그인', bg: '#1D4ED8', tc: '#FFFFFF', name: 'Hover',    opacity: 1   },
    { label: '로그인', bg: '#2563EB', tc: '#FFFFFF', name: 'Loading',  opacity: 1, spinner: true },
    { label: '로그인', bg: '#2563EB', tc: '#FFFFFF', name: 'Disabled', opacity: 0.5 },
  ];
  bx = PAD_X;
  for (const s of btnStates) {
    const bw = 120;
    const grp = makeFrame(main, { name: `state-${s.name}`, x: bx, y: Y, w: bw, h: 40, fill: s.bg, radius: 8 });
    grp.opacity = s.opacity;
    if (s.spinner) {
      // "로그인" 텍스트 + 우측 스피너
      addText(grp, { x: 0, y: 12, text: s.label, size: 14, style: 'Medium', color: s.tc, w: bw - 24, align: 'CENTER' });
      // 스피너 (도넛 링 + 밝은 점으로 회전 힌트)
      addRect(grp, { name: 'spinner-track', x: bw - 32, y: 10, w: 20, h: 20, fill: '#93C5FD', radius: 10 });
      addRect(grp, { name: 'spinner-inner', x: bw - 28, y: 14, w: 12, h: 12, fill: s.bg, radius: 6 });
      addRect(grp, { name: 'spinner-dot', x: bw - 24, y: 10, w: 5, h: 5, fill: '#FFFFFF', radius: 3 });
    } else {
      // 텍스트 중앙 정렬
      addText(grp, { x: 0, y: 12, text: s.label, size: 14, style: 'Medium', color: s.tc, w: bw, align: 'CENTER' });
    }
    addText(main, { x: bx, y: Y + 48, text: s.name, size: 11, style: 'Regular', color: '#94A3B8' });
    bx += bw + 32;
  }
  Y += 40 + 36;

  // Row 4: Icon Buttons
  addText(main, { x: PAD_X, y: Y, text: 'ICON BUTTONS', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  // Icon + text (아이콘 왼쪽, 텍스트는 아이콘 이후 중앙)
  const iconBtnW = 160;
  const iconBtnF = makeFrame(main, { name: 'btn-icon-text', x: PAD_X, y: Y, w: iconBtnW, h: 40, fill: '#2563EB', radius: 8 });
  addRect(iconBtnF, { name: 'icon-plus', x: 14, y: 10, w: 20, h: 20, fill: '#FFFFFF', radius: 4 });
  addText(iconBtnF, { x: 38, y: 12, text: '새 키 발급', size: 14, style: 'Medium', color: '#FFFFFF', w: iconBtnW - 52, align: 'CENTER' });

  // Text only
  const textOnlyF = makeFrame(main, { name: 'btn-text-only', x: PAD_X + iconBtnW + 32, y: Y, w: 120, h: 40, fill: '#2563EB', radius: 8 });
  addText(textOnlyF, { x: 0, y: 12, text: '시작하기', size: 14, style: 'Medium', color: '#FFFFFF', w: 120, align: 'CENTER' });

  // Icon only (circle)
  addRect(main, { name: 'btn-icon-only', x: PAD_X + iconBtnW + 32 + 120 + 32, y: Y, w: 40, h: 40, fill: '#2563EB', radius: 20 });
  addRect(main, { name: 'icon-inner', x: PAD_X + iconBtnW + 32 + 120 + 32 + 10, y: Y + 10, w: 20, h: 20, fill: '#FFFFFF', radius: 4 });

  Y += 40 + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 2 — Input Fields  (y: ~500)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Input Fields', FRAME_W);

  const INPUT_W = 400;
  const INPUT_H = 40;
  const INPUT_COL_GAP = 48;

  // Row 1: States (vertical stack, 3 columns)
  const inputStates = [
    {
      label: '이메일 *', placeholder: '예) user@wisecan.co.kr',
      value: null, border: '#E2E8F0', borderW: 1, ring: false,
      errorMsg: null, hint: null, disabled: false, state: 'Default',
    },
    {
      label: '이메일 *', placeholder: null,
      value: 'kim@wisecan.co.kr', border: '#E2E8F0', borderW: 1, ring: false,
      errorMsg: null, hint: null, disabled: false, state: 'Filled',
    },
    {
      label: '이메일 *', placeholder: '예) user@wisecan.co.kr',
      value: null, border: '#2563EB', borderW: 2, ring: true,
      errorMsg: null, hint: null, disabled: false, state: 'Focus',
    },
    {
      label: '이메일 *', placeholder: 'invalid-email',
      value: null, border: '#EF4444', borderW: 1, ring: false,
      errorMsg: '올바른 이메일 형식이 아닙니다', hint: null, disabled: false, state: 'Error',
    },
    {
      label: '이메일 *', placeholder: '예) user@wisecan.co.kr',
      value: null, border: '#E2E8F0', borderW: 1, ring: false,
      errorMsg: null, hint: null, disabled: true, state: 'Disabled',
    },
    {
      label: '비밀번호 *', placeholder: '8자 이상 입력',
      value: null, border: '#E2E8F0', borderW: 1, ring: false,
      errorMsg: null, hint: '영문, 숫자를 포함하여 8자 이상', disabled: false, state: 'With Hint',
    },
  ];

  addText(main, { x: PAD_X, y: Y, text: 'STATES', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const statesStartY = Y;
  for (let i = 0; i < inputStates.length; i++) {
    const inp = inputStates[i];
    const col = Math.floor(i / 2);
    const row = i % 2;
    const ix = PAD_X + col * (INPUT_W + INPUT_COL_GAP);
    const iy = statesStartY + row * 100;
    const bg = inp.disabled ? '#F8FAFC' : '#FFFFFF';
    const opacity = inp.disabled ? 0.5 : 1;

    addText(main, { x: ix, y: iy, text: inp.label, size: 14, style: 'Medium', color: '#0F172A' });

    const inputBox = makeFrame(main, { name: `input-${inp.state}`, x: ix, y: iy + 22, w: INPUT_W, h: INPUT_H, fill: bg, radius: 8, strokeColor: inp.border });
    inputBox.strokes = [{ type: 'SOLID', color: hexToRgb(inp.border) }];
    inputBox.strokeWeight = inp.borderW;
    inputBox.strokeAlign = 'INSIDE';
    inputBox.opacity = opacity;

    const displayText = inp.value || inp.placeholder;
    const textColor = inp.value ? '#0F172A' : '#94A3B8';
    addText(inputBox, { x: 12, y: 11, text: displayText || '', size: 14, style: 'Regular', color: textColor });

    if (inp.errorMsg) {
      addText(main, { x: ix, y: iy + 66, text: inp.errorMsg, size: 13, style: 'Regular', color: '#EF4444' });
    }
    if (inp.hint) {
      addText(main, { x: ix, y: iy + 66, text: inp.hint, size: 12, style: 'Regular', color: '#64748B' });
    }
    addText(main, { x: ix + INPUT_W + 8, y: iy + 32, text: inp.state, size: 11, style: 'Regular', color: '#94A3B8' });
  }
  Y = statesStartY + 2 * 100 + 20;

  // Row 2: Input Types
  addText(main, { x: PAD_X, y: Y, text: 'INPUT TYPES', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const inputTypes = [
    { label: 'API Key 별칭', placeholder: '예) 프로덕션 서버 키',     type: 'Text',     icon: null    },
    { label: '비밀번호 *',   placeholder: '●●●●●●●●',               type: 'Password', icon: '👁'    },
    { label: '메시지 검색',  placeholder: '메시지 내용으로 검색...',   type: 'Search',   icon: '🔍'   },
  ];
  let itX = PAD_X;
  for (const it of inputTypes) {
    addText(main, { x: itX, y: Y, text: it.label, size: 14, style: 'Medium', color: '#0F172A' });
    const itBox = makeFrame(main, { name: `input-${it.type}`, x: itX, y: Y + 22, w: INPUT_W, h: INPUT_H, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0' });
    itBox.strokes = [{ type: 'SOLID', color: hexToRgb('#E2E8F0') }];
    itBox.strokeWeight = 1;
    itBox.strokeAlign = 'INSIDE';
    const textOffX = it.type === 'Search' ? 36 : 12;
    addText(itBox, { x: textOffX, y: 11, text: it.placeholder, size: 14, style: 'Regular', color: '#94A3B8' });
    if (it.type === 'Search') {
      addRect(itBox, { name: 'search-icon', x: 10, y: 10, w: 20, h: 20, fill: '#94A3B8', radius: 10 });
    }
    if (it.type === 'Password' || it.type === 'Search') {
      addRect(itBox, { name: 'right-icon', x: INPUT_W - 32, y: 10, w: 20, h: 20, fill: '#CBD5E1', radius: 4 });
    }
    itX += INPUT_W + INPUT_COL_GAP;
  }
  Y += INPUT_H + 40;

  // Row 3: Textarea
  addText(main, { x: PAD_X, y: Y, text: 'TEXTAREA', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;
  addText(main, { x: PAD_X, y: Y, text: '메시지 내용 *', size: 14, style: 'Medium', color: '#0F172A' });
  const taFrame = makeFrame(main, { name: 'textarea', x: PAD_X, y: Y + 22, w: INPUT_W, h: 120, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0' });
  taFrame.strokes = [{ type: 'SOLID', color: hexToRgb('#E2E8F0') }];
  taFrame.strokeWeight = 1;
  taFrame.strokeAlign = 'INSIDE';
  addText(taFrame, { x: 12, y: 12, text: '전송할 메시지를 입력하세요.', size: 14, style: 'Regular', color: '#94A3B8' });
  addText(taFrame, { x: INPUT_W - 52, y: 96, text: '0/500', size: 12, style: 'Regular', color: '#94A3B8' });
  Y += 120 + 22 + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 3 — Cards  (y: ~1100)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Cards', FRAME_W);

  // Row 1: Stat Cards (4개)
  addText(main, { x: PAD_X, y: Y, text: 'STAT CARDS', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const statCards = [
    { title: '오늘 API 호출',   value: '1,234건',   delta: '+12.5% 전일 대비', deltaColor: '#22C55E' },
    { title: '성공률',          value: '98.5%',     delta: '-0.3% 전일 대비',  deltaColor: '#EF4444' },
    { title: '활성 API Key',    value: '3개',       delta: '변화 없음',         deltaColor: '#64748B' },
    { title: '이번 달 총 호출', value: '45,678건',  delta: '+8.2% 전월 대비',  deltaColor: '#22C55E' },
  ];

  let scX = PAD_X;
  for (const sc of statCards) {
    const scF = makeFrame(main, { name: `stat-${sc.title}`, x: scX, y: Y, w: 280, h: 120, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0' });
    addText(scF, { x: 20, y: 20, text: sc.title, size: 12, style: 'Medium', color: '#64748B' });
    addText(scF, { x: 20, y: 42, text: sc.value, size: 30, style: 'Bold', color: '#0F172A' });
    addText(scF, { x: 20, y: 84, text: sc.delta, size: 12, style: 'Regular', color: sc.deltaColor });
    addRect(scF, { name: 'trend-icon', x: 236, y: 16, w: 28, h: 28, fill: '#F1F5F9', radius: 6 });
    scX += 280 + 24;
  }
  Y += 120 + 32;

  // Row 2: Default Card
  addText(main, { x: PAD_X, y: Y, text: 'DEFAULT CARD (bordered)', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const defCard = makeFrame(main, { name: 'default-card', x: PAD_X, y: Y, w: 560, h: 200, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0' });
  addText(defCard, { x: 24, y: 24, text: '최근 호출 이력', size: 18, style: 'Bold', color: '#0F172A' });
  addText(defCard, { x: 490, y: 28, text: '전체 보기', size: 14, style: 'Medium', color: '#2563EB' });
  addRect(defCard, { name: 'content-placeholder', x: 24, y: 68, w: 512, h: 100, fill: '#F8FAFC', radius: 8 });
  addText(defCard, { x: 220, y: 110, text: '호출 데이터 로딩 중...', size: 13, style: 'Regular', color: '#CBD5E1' });

  // Row 3: Elevated Card
  const elevCard = makeFrame(main, { name: 'elevated-card', x: PAD_X + 560 + 32, y: Y, w: 560, h: 200, fill: '#FFFFFF', radius: 12 });
  addText(elevCard, { x: 24, y: 24, text: 'API 사용 현황', size: 18, style: 'Bold', color: '#0F172A' });
  addText(elevCard, { x: 490, y: 28, text: '상세 보기', size: 14, style: 'Medium', color: '#2563EB' });
  addRect(elevCard, { name: 'content-placeholder', x: 24, y: 68, w: 512, h: 100, fill: '#F8FAFC', radius: 8 });
  addText(elevCard, { x: 180, y: 110, text: 'Elevated Card (shadow-md — CSS로 구현)', size: 13, style: 'Regular', color: '#CBD5E1' });

  Y += 200 + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 4 — Badges  (y: ~1800)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Badges & Status', FRAME_W);

  // Row 1: Badge Variants
  addText(main, { x: PAD_X, y: Y, text: 'BADGE VARIANTS', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const badges = [
    { label: 'Active',   bg: '#F0FDF4', tc: '#16A34A' },
    { label: 'Revoked',  bg: '#FEF2F2', tc: '#DC2626' },
    { label: 'Pending',  bg: '#FFFBEB', tc: '#D97706' },
    { label: '베타',     bg: '#EFF6FF', tc: '#1D4ED8' },
    { label: '신규',     bg: '#F1F5F9', tc: '#475569' },
  ];
  let bdgX = PAD_X;
  for (const b of badges) {
    const bw = b.label.length * 9 + 20;
    addRect(main, { name: `badge-${b.label}`, x: bdgX, y: Y, w: bw, h: 24, fill: b.bg, radius: 12 });
    addText(main, { x: bdgX + 10, y: Y + 6, text: b.label, size: 12, style: 'Medium', color: b.tc });
    bdgX += bw + 12;
  }
  Y += 24 + 32;

  // Row 2: Status Dot + Text
  addText(main, { x: PAD_X, y: Y, text: 'STATUS INDICATORS', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const statusDots = [
    { label: '성공',  color: '#22C55E' },
    { label: '실패',  color: '#EF4444' },
    { label: '대기중', color: '#F59E0B' },
    { label: '취소됨', color: '#94A3B8' },
  ];
  let sdX = PAD_X;
  for (const sd of statusDots) {
    addRect(main, { name: `dot-${sd.label}`, x: sdX, y: Y + 4, w: 8, h: 8, fill: sd.color, radius: 4 });
    addText(main, { x: sdX + 14, y: Y, text: sd.label, size: 14, style: 'Regular', color: '#0F172A' });
    sdX += 80;
  }
  Y += 24 + 24;

  // Row 3: 인기 Badge
  addText(main, { x: PAD_X, y: Y, text: 'HIGHLIGHT BADGE', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;
  addRect(main, { name: 'badge-popular', x: PAD_X, y: Y, w: 56, h: 24, fill: '#2563EB', radius: 12 });
  addText(main, { x: PAD_X + 12, y: Y + 6, text: '인기', size: 12, style: 'Medium', color: '#FFFFFF' });

  Y += 24 + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 5 — Table  (y: ~2100)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Table', FRAME_W);

  const TABLE_W = 1400;
  const ROW_H = 56;
  const HEADER_H = 44;

  // API 호출 이력 테이블
  addText(main, { x: PAD_X, y: Y, text: 'API 호출 이력 테이블', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const colWidths = [140, 200, 260, 100, 120, 100];
  const colHeaders = ['시간', 'API Key', '도구명', '상태', '응답시간', '결과코드'];
  const colX = colWidths.map((_, i) => PAD_X + colWidths.slice(0, i).reduce((a, b) => a + b, 0));

  // Header row
  addRect(main, { name: 'table-header-bg', x: PAD_X, y: Y, w: TABLE_W, h: HEADER_H, fill: '#F8FAFC', radius: 0 });
  for (let i = 0; i < colHeaders.length; i++) {
    addText(main, { x: colX[i], y: Y + 14, text: colHeaders[i].toUpperCase(), size: 11, style: 'Semi Bold', color: '#64748B', w: colWidths[i], align: 'CENTER' });
  }
  Y += HEADER_H;

  const tableRows = [
    { time: '14:23:05', key: 'wc_a1b2c3d4', tool: 'message_send',       status: '성공', statusBg: '#F0FDF4', statusTc: '#16A34A', latency: '234ms',   code: '0000' },
    { time: '14:22:51', key: 'wc_a1b2c3d4', tool: 'message_get_result', status: '성공', statusBg: '#F0FDF4', statusTc: '#16A34A', latency: '89ms',    code: '0000' },
    { time: '14:21:33', key: 'wc_e5f6g7h8', tool: 'message_send',       status: '실패', statusBg: '#FEF2F2', statusTc: '#DC2626', latency: '1,203ms', code: 'E301' },
    { time: '14:20:15', key: 'wc_a1b2c3d4', tool: 'message_search',     status: '성공', statusBg: '#F0FDF4', statusTc: '#16A34A', latency: '156ms',   code: '0000' },
    { time: '14:19:02', key: 'wc_a1b2c3d4', tool: 'file_md_to_pdf',     status: '성공', statusBg: '#F0FDF4', statusTc: '#16A34A', latency: '2,340ms', code: '0000' },
  ];

  // 열 구분선 (헤더+데이터 영역 전체)
  const totalTableH = HEADER_H + tableRows.length * ROW_H;
  for (let i = 1; i < colX.length; i++) {
    addRect(main, { name: `col-line-${i}`, x: colX[i], y: Y - HEADER_H, w: 1, h: totalTableH, fill: '#E2E8F0' });
  }

  for (let ri = 0; ri < tableRows.length; ri++) {
    const row = tableRows[ri];
    const rowY = Y + ri * ROW_H;
    addRect(main, { name: `row-${ri}-bg`, x: PAD_X, y: rowY, w: TABLE_W, h: ROW_H, fill: '#FFFFFF' });
    addRect(main, { name: `row-${ri}-border`, x: PAD_X, y: rowY + ROW_H - 1, w: TABLE_W, h: 1, fill: '#F1F5F9' });

    const cells = [row.time, row.key, row.tool, null, row.latency, row.code];
    for (let ci = 0; ci < cells.length; ci++) {
      if (ci === 3) {
        // 뱃지 셀 중앙 정렬
        const badgeW = row.status.length * 10 + 20;
        const badgeX = colX[ci] + (colWidths[ci] - badgeW) / 2;
        addRect(main, { name: `badge-status`, x: badgeX, y: rowY + 16, w: badgeW, h: 24, fill: row.statusBg, radius: 12 });
        addText(main, { x: badgeX, y: rowY + 22, text: row.status, size: 12, style: 'Medium', color: row.statusTc, w: badgeW, align: 'CENTER' });
      } else {
        const textColor = ci === 1 ? '#2563EB' : '#0F172A';
        addText(main, { x: colX[ci], y: rowY + 19, text: cells[ci], size: 14, style: ci === 1 ? 'Medium' : 'Regular', color: textColor, w: colWidths[ci], align: 'CENTER' });
      }
    }
  }

  Y += tableRows.length * ROW_H;

  // 페이지네이션
  const pageNums = ['<', '1', '2', '3', '...', '10', '>'];
  let pgX = PAD_X + TABLE_W / 2 - pageNums.length * 20;
  addText(main, { x: PAD_X, y: Y + 14, text: '총 128건', size: 13, style: 'Regular', color: '#64748B' });
  for (const pg of pageNums) {
    const isActive = pg === '1';
    addRect(main, { name: `pg-${pg}`, x: pgX, y: Y + 8, w: 32, h: 32, fill: isActive ? '#2563EB' : '#FFFFFF', radius: 6, strokeColor: isActive ? undefined : '#E2E8F0' });
    addText(main, { x: pgX + (pg.length === 1 ? 10 : 6), y: Y + 16, text: pg, size: 14, style: isActive ? 'Bold' : 'Regular', color: isActive ? '#FFFFFF' : '#64748B' });
    pgX += 40;
  }
  Y += 48 + 48;

  // API Key 테이블
  addText(main, { x: PAD_X, y: Y, text: 'API KEY 관리 테이블', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const keyColW = [220, 200, 120, 180, 180, 160];
  const keyColH = ['별칭', '접두사', '상태', '마지막 사용', '생성일', '액션'];
  const keyColX = keyColW.map((_, i) => PAD_X + keyColW.slice(0, i).reduce((a, b) => a + b, 0));
  const keyTableW = keyColW.reduce((a, b) => a + b, 0);

  addRect(main, { name: 'key-table-header-bg', x: PAD_X, y: Y, w: keyTableW, h: HEADER_H, fill: '#F8FAFC', radius: 0 });
  for (let i = 0; i < keyColH.length; i++) {
    addText(main, { x: keyColX[i], y: Y + 14, text: keyColH[i].toUpperCase(), size: 11, style: 'Semi Bold', color: '#64748B', w: keyColW[i], align: 'CENTER' });
  }
  Y += HEADER_H;

  const keyRows = [
    { alias: '프로덕션 키', prefix: 'wc_a1b2c3d4', status: 'Active', statusBg: '#F0FDF4', statusTc: '#16A34A', lastUsed: '2분 전',   created: '2024-03-15', revoked: false },
    { alias: '테스트 키',   prefix: 'wc_e5f6g7h8', status: 'Active', statusBg: '#F0FDF4', statusTc: '#16A34A', lastUsed: '1시간 전', created: '2024-03-10', revoked: false },
    { alias: '레거시 키',   prefix: 'wc_i9j0k1l2', status: 'Revoked', statusBg: '#FEF2F2', statusTc: '#DC2626', lastUsed: '-',        created: '2024-02-01', revoked: true  },
  ];

  // 열 구분선 (keyRows 선언 후)
  const keyTableTotalH = HEADER_H + keyRows.length * ROW_H;
  for (let i = 1; i < keyColX.length; i++) {
    addRect(main, { name: `kcol-line-${i}`, x: keyColX[i], y: Y - HEADER_H, w: 1, h: keyTableTotalH, fill: '#E2E8F0' });
  }

  for (let ri = 0; ri < keyRows.length; ri++) {
    const row = keyRows[ri];
    const rowY = Y + ri * ROW_H;
    addRect(main, { name: `krow-${ri}-bg`, x: PAD_X, y: rowY, w: keyTableW, h: ROW_H, fill: '#FFFFFF' });
    addRect(main, { name: `krow-${ri}-border`, x: PAD_X, y: rowY + ROW_H - 1, w: keyTableW, h: 1, fill: '#F1F5F9' });

    addText(main, { x: keyColX[0], y: rowY + 19, text: row.alias, size: 14, style: 'Medium', color: '#0F172A', w: keyColW[0], align: 'CENTER' });
    addText(main, { x: keyColX[1], y: rowY + 19, text: row.prefix, size: 13, style: 'Regular', color: '#64748B', w: keyColW[1], align: 'CENTER' });

    const bw2 = row.status.length * 9 + 20;
    const kBadgeX = keyColX[2] + (keyColW[2] - bw2) / 2;
    addRect(main, { name: `kbadge`, x: kBadgeX, y: rowY + 16, w: bw2, h: 24, fill: row.statusBg, radius: 12 });
    addText(main, { x: kBadgeX, y: rowY + 22, text: row.status, size: 12, style: 'Medium', color: row.statusTc, w: bw2, align: 'CENTER' });

    addText(main, { x: keyColX[3], y: rowY + 19, text: row.lastUsed, size: 14, style: 'Regular', color: '#64748B', w: keyColW[3], align: 'CENTER' });
    addText(main, { x: keyColX[4], y: rowY + 19, text: row.created, size: 14, style: 'Regular', color: '#64748B', w: keyColW[4], align: 'CENTER' });

    if (!row.revoked) {
      const btnW = 88;
      const btnX = keyColX[5] + (keyColW[5] - btnW) / 2;
      addRect(main, { name: 'btn-deactivate', x: btnX, y: rowY + 14, w: btnW, h: 28, fill: '#FFFFFF', radius: 6, strokeColor: '#EF4444' });
      addText(main, { x: btnX, y: rowY + 20, text: '비활성화', size: 13, style: 'Medium', color: '#EF4444', w: btnW, align: 'CENTER' });
    } else {
      addText(main, { x: keyColX[5], y: rowY + 19, text: '-', size: 14, style: 'Regular', color: '#CBD5E1', w: keyColW[5], align: 'CENTER' });
    }
  }

  Y += keyRows.length * ROW_H + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 6 — Modal / Dialog  (y: ~2800)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Modal / Dialog', FRAME_W);

  // PC Modal — 기본
  addText(main, { x: PAD_X, y: Y, text: 'PC MODAL — 새 API Key 발급', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const overlayW = 800;
  const overlayH = 500;
  const modalW = 480;
  const modalH = 360;

  const overlayF1 = makeFrame(main, { name: 'modal-overlay-1', x: PAD_X, y: Y, w: overlayW, h: overlayH, fill: '#0F172A', radius: 0, clipsContent: true });
  overlayF1.opacity = 0.5;

  const modalF1 = makeFrame(main, { name: 'modal-create-key', x: PAD_X + (overlayW - modalW) / 2, y: Y + (overlayH - modalH) / 2, w: modalW, h: modalH, fill: '#FFFFFF', radius: 16 });
  // Header
  addText(modalF1, { x: 24, y: 24, text: '새 API Key 발급', size: 20, style: 'Bold', color: '#0F172A' });
  addRect(modalF1, { name: 'close-btn', x: modalW - 44, y: 20, w: 24, h: 24, fill: '#F1F5F9', radius: 12 });
  addRect(modalF1, { name: 'modal-divider', x: 0, y: 64, w: modalW, h: 1, fill: '#E2E8F0' });
  // Body
  addText(modalF1, { x: 24, y: 80, text: 'API Key 별칭 *', size: 14, style: 'Medium', color: '#0F172A' });
  const mInputF = makeFrame(modalF1, { name: 'modal-input', x: 24, y: 104, w: modalW - 48, h: 40, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0' });
  mInputF.strokes = [{ type: 'SOLID', color: hexToRgb('#E2E8F0') }];
  mInputF.strokeWeight = 1;
  mInputF.strokeAlign = 'INSIDE';
  addText(mInputF, { x: 12, y: 11, text: '예) 프로덕션 서버 키', size: 14, style: 'Regular', color: '#94A3B8' });
  // Footer
  addRect(modalF1, { name: 'modal-footer-divider', x: 0, y: modalH - 72, w: modalW, h: 1, fill: '#E2E8F0' });
  addRect(modalF1, { name: 'btn-cancel', x: 24, y: modalH - 52, w: 100, h: 40, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0' });
  addText(modalF1, { x: 52, y: modalH - 40, text: '취소', size: 14, style: 'Medium', color: '#0F172A' });
  addRect(modalF1, { name: 'btn-create', x: modalW - 140, y: modalH - 52, w: 116, h: 40, fill: '#2563EB', radius: 8 });
  addText(modalF1, { x: modalW - 116, y: modalH - 40, text: '발급하기', size: 14, style: 'Medium', color: '#FFFFFF' });

  // PC Modal — 성공
  const modal2X = PAD_X + overlayW + 48;
  addText(main, { x: modal2X, y: Y, text: 'PC MODAL — 발급 완료', size: 11, style: 'Semi Bold', color: '#94A3B8' });

  const overlayF2 = makeFrame(main, { name: 'modal-overlay-2', x: modal2X, y: Y + 20, w: overlayW, h: overlayH, fill: '#0F172A', radius: 0, clipsContent: true });
  overlayF2.opacity = 0.5;

  const modal2H = 400;
  const modalF2 = makeFrame(main, { name: 'modal-success', x: modal2X + (overlayW - modalW) / 2, y: Y + 20 + (overlayH - modal2H) / 2, w: modalW, h: modal2H, fill: '#FFFFFF', radius: 16 });
  // Header
  addRect(modalF2, { name: 'check-icon-bg', x: 24, y: 24, w: 32, h: 32, fill: '#F0FDF4', radius: 16 });
  addRect(modalF2, { name: 'check-icon', x: 32, y: 32, w: 16, h: 16, fill: '#22C55E', radius: 8 });
  addText(modalF2, { x: 68, y: 32, text: 'API Key 발급 완료', size: 20, style: 'Bold', color: '#0F172A' });
  addRect(modalF2, { name: 'divider', x: 0, y: 72, w: modalW, h: 1, fill: '#E2E8F0' });
  // Body
  addText(modalF2, { x: 24, y: 88, text: '발급된 API Key', size: 14, style: 'Medium', color: '#64748B' });
  const keyDisplayF = makeFrame(modalF2, { name: 'key-display', x: 24, y: 112, w: modalW - 48, h: 48, fill: '#F8FAFC', radius: 8, strokeColor: '#E2E8F0' });
  keyDisplayF.strokes = [{ type: 'SOLID', color: hexToRgb('#E2E8F0') }];
  keyDisplayF.strokeWeight = 1;
  keyDisplayF.strokeAlign = 'INSIDE';
  addText(keyDisplayF, { x: 12, y: 14, text: 'wc_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5', size: 13, style: 'Regular', color: '#0F172A' });
  addRect(keyDisplayF, { name: 'copy-btn', x: keyDisplayF.width - 68, y: 8, w: 56, h: 32, fill: '#2563EB', radius: 6 });
  addText(keyDisplayF, { x: keyDisplayF.width - 52, y: 16, text: '복사', size: 13, style: 'Medium', color: '#FFFFFF' });
  // Warning banner
  const warnF = makeFrame(modalF2, { name: 'warning-banner', x: 24, y: 176, w: modalW - 48, h: 56, fill: '#FEF2F2', radius: 8 });
  addRect(warnF, { name: 'warn-bar', x: 0, y: 0, w: 4, h: 56, fill: '#EF4444', radius: 2 });
  addText(warnF, { x: 16, y: 10, text: '이 키는 다시 확인할 수 없습니다.', size: 13, style: 'Semi Bold', color: '#991B1B' });
  addText(warnF, { x: 16, y: 30, text: '안전한 곳에 즉시 보관하세요.', size: 12, style: 'Regular', color: '#B91C1C' });
  // Footer
  addRect(modalF2, { name: 'divider-footer', x: 0, y: modal2H - 72, w: modalW, h: 1, fill: '#E2E8F0' });
  addRect(modalF2, { name: 'btn-confirm', x: modalW - 116, y: modal2H - 52, w: 92, h: 40, fill: '#2563EB', radius: 8 });
  addText(modalF2, { x: modalW - 96, y: modal2H - 40, text: '확인', size: 14, style: 'Medium', color: '#FFFFFF' });

  // Mobile Bottom Sheet
  const bsX = PAD_X + overlayW + 48 + overlayW + 48;
  addText(main, { x: bsX, y: Y, text: 'MOBILE BOTTOM SHEET', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  const bsW = 375;
  const bsH = 400;
  const bsF = makeFrame(main, { name: 'mobile-bottom-sheet', x: bsX, y: Y + 20, w: bsW, h: bsH, fill: '#FFFFFF', radius: 0 });
  // 상단 핸들
  addRect(bsF, { name: 'handle', x: (bsW - 40) / 2, y: 12, w: 40, h: 4, fill: '#CBD5E1', radius: 2 });
  addRect(bsF, { name: 'bs-divider', x: 0, y: 36, w: bsW, h: 1, fill: '#E2E8F0' });
  addText(bsF, { x: 20, y: 52, text: '새 API Key 발급', size: 18, style: 'Bold', color: '#0F172A' });
  addText(bsF, { x: 20, y: 88, text: 'API Key 별칭 *', size: 14, style: 'Medium', color: '#0F172A' });
  const bsInputF = makeFrame(bsF, { name: 'bs-input', x: 20, y: 112, w: bsW - 40, h: 40, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0' });
  bsInputF.strokes = [{ type: 'SOLID', color: hexToRgb('#E2E8F0') }];
  bsInputF.strokeWeight = 1;
  bsInputF.strokeAlign = 'INSIDE';
  addText(bsInputF, { x: 12, y: 11, text: '예) 프로덕션 서버 키', size: 14, style: 'Regular', color: '#94A3B8' });
  addRect(bsF, { name: 'bs-btn-cancel', x: 20, y: bsH - 100, w: (bsW - 52) / 2, h: 44, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0' });
  addText(bsF, { x: 60, y: bsH - 86, text: '취소', size: 15, style: 'Medium', color: '#0F172A' });
  addRect(bsF, { name: 'bs-btn-create', x: 20 + (bsW - 52) / 2 + 12, y: bsH - 100, w: (bsW - 52) / 2, h: 44, fill: '#2563EB', radius: 8 });
  addText(bsF, { x: 20 + (bsW - 52) / 2 + 36, y: bsH - 86, text: '발급하기', size: 15, style: 'Medium', color: '#FFFFFF' });

  Y += overlayH + 20 + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 7 — Sidebar Navigation  (y: ~3600)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Sidebar Navigation', FRAME_W);

  // PC Sidebar — Expanded
  addText(main, { x: PAD_X, y: Y, text: 'PC SIDEBAR — EXPANDED', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const sbW = 240;
  const sbH = 700;
  // 라운드 플로팅: 왼쪽 직각 + 오른쪽만 라운드 (좌 0, 우 16)
  const sbF = makeFrame(main, { name: 'sidebar-expanded', x: PAD_X, y: Y, w: sbW, h: sbH, fill: '#FFFFFF' });
  sbF.topLeftRadius = 0;
  sbF.topRightRadius = 16;
  sbF.bottomRightRadius = 16;
  sbF.bottomLeftRadius = 0;
  sbF.strokes = solid('#E2E8F0');
  sbF.strokeWeight = 1;
  sbF.strokeAlign = 'INSIDE';

  // Logo
  addText(sbF, { x: 16, y: 20, text: 'Wisecan', size: 20, style: 'Bold', color: '#0F172A' });
  addText(sbF, { x: 16, y: 46, text: 'MCP 플랫폼', size: 12, style: 'Regular', color: '#64748B' });
  addRect(sbF, { name: 'logo-divider', x: 0, y: 72, w: sbW, h: 1, fill: '#E2E8F0' });

  // Nav items
  const navItems = [
    { label: '대시보드',    active: true,  section: null          },
    { label: 'API Keys',   active: false, section: null          },
    { label: '메시지 발송', active: false, section: '도구'        },
    { label: '메시지 통계', active: false, section: null          },
    { label: '파일 변환',  active: false, section: null          },
    { label: '설정',       active: false, section: 'divider'     },
  ];
  let navY = 84;
  for (const item of navItems) {
    if (item.section === 'divider') {
      addRect(sbF, { name: 'nav-divider', x: 0, y: navY, w: sbW, h: 1, fill: '#E2E8F0' });
      navY += 12;
    }
    if (item.section && item.section !== 'divider') {
      addRect(sbF, { name: `sec-divider`, x: 0, y: navY, w: sbW, h: 1, fill: '#E2E8F0' });
      navY += 8;
      addText(sbF, { x: 16, y: navY, text: item.section.toUpperCase(), size: 11, style: 'Semi Bold', color: '#94A3B8' });
      navY += 20;
    }
    const itemBg = item.active ? '#EFF6FF' : 'transparent';
    const itemTc = item.active ? '#2563EB' : '#64748B';
    if (item.active) {
      addRect(sbF, { name: `nav-active-bg`, x: 8, y: navY, w: sbW - 16, h: 40, fill: '#EFF6FF', radius: 8 });
    }
    addRect(sbF, { name: `nav-icon-${item.label}`, x: 16, y: navY + 10, w: 20, h: 20, fill: item.active ? '#2563EB' : '#CBD5E1', radius: 4 });
    addText(sbF, { x: 44, y: navY + 12, text: item.label, size: 14, style: item.active ? 'Semi Bold' : 'Regular', color: itemTc });
    navY += 44;
  }

  // User profile
  addRect(sbF, { name: 'profile-divider', x: 0, y: sbH - 72, w: sbW, h: 1, fill: '#E2E8F0' });
  addRect(sbF, { name: 'avatar', x: 16, y: sbH - 52, w: 32, h: 32, fill: '#EFF6FF', radius: 16 });
  addText(sbF, { x: 26, y: sbH - 41, text: '김', size: 14, style: 'Bold', color: '#2563EB' });
  addText(sbF, { x: 56, y: sbH - 50, text: '김위즈캔', size: 14, style: 'Medium', color: '#0F172A' });
  addText(sbF, { x: 56, y: sbH - 32, text: 'kim@wisecan.co.kr', size: 11, style: 'Regular', color: '#64748B' });

  // PC Sidebar — Collapsed
  addText(main, { x: PAD_X + sbW + 48, y: Y, text: 'PC SIDEBAR — COLLAPSED', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const sbcX = PAD_X + sbW + 48;
  const sbcF = makeFrame(main, { name: 'sidebar-collapsed', x: sbcX, y: Y, w: 64, h: sbH, fill: '#FFFFFF' });
  sbcF.topLeftRadius = 0;
  sbcF.topRightRadius = 16;
  sbcF.bottomRightRadius = 16;
  sbcF.bottomLeftRadius = 0;
  sbcF.strokes = solid('#E2E8F0');
  sbcF.strokeWeight = 1;
  sbcF.strokeAlign = 'INSIDE';
  addRect(sbcF, { name: 'logo-icon', x: 12, y: 20, w: 40, h: 40, fill: '#EFF6FF', radius: 8 });
  addRect(sbcF, { name: 'logo-letter', x: 22, y: 30, w: 20, h: 20, fill: '#2563EB', radius: 4 });
  addRect(sbcF, { name: 'col-divider', x: 0, y: 72, w: 64, h: 1, fill: '#E2E8F0' });
  const colIcons = [true, false, false, false, false, false];
  let ciY = 84;
  for (let ci = 0; ci < colIcons.length; ci++) {
    const isAct = colIcons[ci];
    if (isAct) addRect(sbcF, { name: 'icon-active-bg', x: 8, y: ciY, w: 48, h: 40, fill: '#EFF6FF', radius: 8 });
    addRect(sbcF, { name: `col-icon-${ci}`, x: 22, y: ciY + 10, w: 20, h: 20, fill: isAct ? '#2563EB' : '#CBD5E1', radius: 4 });
    ciY += 44;
  }

  // Mobile Drawer
  const drawerX = sbcX + 64 + 48;
  addText(main, { x: drawerX, y: Y, text: 'MOBILE DRAWER', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const drawerOverlayW = 500;
  const drawerW = 300;
  const drawerOF = makeFrame(main, { name: 'drawer-overlay', x: drawerX, y: Y, w: drawerOverlayW, h: sbH, fill: '#0F172A', radius: 0, clipsContent: true });
  drawerOF.opacity = 0.5;

  const drawerF = makeFrame(main, { name: 'mobile-drawer', x: drawerX, y: Y, w: drawerW, h: sbH, fill: '#FFFFFF' });
  drawerF.topLeftRadius = 0;
  drawerF.topRightRadius = 16;
  drawerF.bottomRightRadius = 16;
  drawerF.bottomLeftRadius = 0;
  addText(drawerF, { x: 16, y: 20, text: 'Wisecan', size: 18, style: 'Bold', color: '#0F172A' });
  addRect(drawerF, { name: 'drawer-close', x: drawerW - 44, y: 16, w: 28, h: 28, fill: '#F1F5F9', radius: 14 });
  addRect(drawerF, { name: 'drawer-divider', x: 0, y: 56, w: drawerW, h: 1, fill: '#E2E8F0' });

  let dnavY = 68;
  for (const item of navItems) {
    if (item.section === 'divider') { addRect(drawerF, { name: 'dnav-div', x: 0, y: dnavY, w: drawerW, h: 1, fill: '#E2E8F0' }); dnavY += 12; }
    if (item.section && item.section !== 'divider') {
      addRect(drawerF, { name: `dsec-div`, x: 0, y: dnavY, w: drawerW, h: 1, fill: '#E2E8F0' }); dnavY += 8;
      addText(drawerF, { x: 16, y: dnavY, text: item.section.toUpperCase(), size: 11, style: 'Semi Bold', color: '#94A3B8' }); dnavY += 20;
    }
    if (item.active) addRect(drawerF, { name: `dnav-active-bg`, x: 8, y: dnavY, w: drawerW - 16, h: 40, fill: '#EFF6FF', radius: 8 });
    addRect(drawerF, { name: `dnav-icon`, x: 16, y: dnavY + 10, w: 20, h: 20, fill: item.active ? '#2563EB' : '#CBD5E1', radius: 4 });
    addText(drawerF, { x: 44, y: dnavY + 12, text: item.label, size: 14, style: item.active ? 'Semi Bold' : 'Regular', color: item.active ? '#2563EB' : '#64748B' });
    dnavY += 44;
  }

  Y += sbH + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 8 — Toast & Alert  (y: ~4400)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Toast & Alert', FRAME_W);

  addText(main, { x: PAD_X, y: Y, text: 'TOAST VARIANTS', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const toasts = [
    { msg: 'API Key가 성공적으로 발급되었습니다', barColor: '#22C55E', iconColor: '#22C55E', iconLabel: '✓' },
    { msg: '메시지 발송에 실패했습니다',           barColor: '#EF4444', iconColor: '#EF4444', iconLabel: '!' },
    { msg: '새로운 기능이 추가되었습니다',          barColor: '#2563EB', iconColor: '#2563EB', iconLabel: 'i' },
  ];
  let toastX = PAD_X;
  for (const t of toasts) {
    const toastW = 400;
    const toastF = makeFrame(main, { name: `toast-${t.iconLabel}`, x: toastX, y: Y, w: toastW, h: 56, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0' });
    addRect(toastF, { name: 'toast-bar', x: 0, y: 0, w: 4, h: 56, fill: t.barColor, radius: 2 });
    addRect(toastF, { name: 'toast-icon-bg', x: 14, y: 16, w: 24, h: 24, fill: t.iconColor, radius: 12 });
    addText(toastF, { x: 14, y: 20, text: t.iconLabel, size: 12, style: 'Bold', color: '#FFFFFF', w: 24, align: 'CENTER' });
    addText(toastF, { x: 50, y: 19, text: t.msg, size: 13, style: 'Regular', color: '#0F172A', w: toastW - 90 });
    addRect(toastF, { name: 'toast-close', x: toastW - 32, y: 18, w: 20, h: 20, fill: '#F1F5F9', radius: 10 });
    toastX += toastW + 24;
  }
  Y += 56 + 32;

  // Error Banner
  addText(main, { x: PAD_X, y: Y, text: 'ERROR BANNER (폼 에러용)', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const errBannerW = 480;
  const errF = makeFrame(main, { name: 'error-banner', x: PAD_X, y: Y, w: errBannerW, h: 56, fill: '#FEF2F2', radius: 8 });
  addRect(errF, { name: 'err-bar', x: 0, y: 0, w: 4, h: 56, fill: '#EF4444', radius: 2 });
  addRect(errF, { name: 'err-icon-bg', x: 14, y: 16, w: 24, h: 24, fill: '#EF4444', radius: 12 });
  addText(errF, { x: 14, y: 20, text: '!', size: 13, style: 'Bold', color: '#FFFFFF', w: 24, align: 'CENTER' });
  addText(errF, { x: 50, y: 19, text: '이메일 또는 비밀번호가 일치하지 않습니다', size: 13, style: 'Regular', color: '#991B1B', w: errBannerW - 80 });

  Y += 56 + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 9 — Empty State  (y: ~4800)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Empty State', FRAME_W);

  const emptyStates = [
    {
      title: 'API Key가 없습니다',
      sub:   '아직 발급된 API Key가 없습니다.\nAPI Key를 발급하고 메시지 발송을 시작하세요.',
      btn:   'API Key 발급',
    },
    {
      title: '호출 이력이 없습니다',
      sub:   '아직 API 호출 이력이 없습니다.\nAPI Key를 사용하여 첫 호출을 시작하세요.',
      btn:   'API 문서 보기',
    },
    {
      title: '검색 결과가 없습니다',
      sub:   '입력하신 조건과 일치하는 결과가 없습니다.\n다른 조건으로 다시 검색해보세요.',
      btn:   null,
    },
  ];

  let esX = PAD_X;
  for (const es of emptyStates) {
    const esW = 400;
    const esH = 240;
    const esF = makeFrame(main, { name: `empty-${es.title}`, x: esX, y: Y, w: esW, h: esH, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0' });

    // Icon circle
    addRect(esF, { name: 'empty-icon-bg', x: (esW - 64) / 2, y: 24, w: 64, h: 64, fill: '#F1F5F9', radius: 32 });
    addRect(esF, { name: 'empty-icon', x: (esW - 32) / 2, y: 40, w: 32, h: 32, fill: '#CBD5E1', radius: 8 });

    addText(esF, { x: 24, y: 104, text: es.title, size: 16, style: 'Semi Bold', color: '#0F172A', w: esW - 48 });
    addText(esF, { x: 24, y: 128, text: es.sub, size: 14, style: 'Regular', color: '#64748B', w: esW - 48 });

    if (es.btn) {
      const btnW = es.btn.length * 9 + 32;
      addRect(esF, { name: 'empty-btn', x: (esW - btnW) / 2, y: esH - 52, w: btnW, h: 36, fill: '#2563EB', radius: 8 });
      addText(esF, { x: (esW - btnW) / 2 + 16, y: esH - 42, text: es.btn, size: 14, style: 'Medium', color: '#FFFFFF' });
    }

    esX += esW + 32;
  }
  Y += 240 + SEC_GAP;

  // ══════════════════════════════════════════════════════════════════════════
  // SECTION 10 — Form Validation & Page Error Banner  (y: ~5200)
  // ══════════════════════════════════════════════════════════════════════════
  Y = sectionHeader(main, PAD_X, Y, 'Error States', FRAME_W);

  // Form validation errors
  addText(main, { x: PAD_X, y: Y, text: 'FORM VALIDATION ERRORS', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const formErrFields = [
    { label: '이메일 *', placeholder: 'invalid-email', error: '올바른 이메일 형식이 아닙니다' },
    { label: '비밀번호 *', placeholder: '●●●●●', error: '비밀번호는 8자 이상이어야 합니다' },
    { label: '회사명 *', placeholder: '', error: '회사명을 입력해주세요' },
  ];
  let fvX = PAD_X;
  for (const fv of formErrFields) {
    addText(main, { x: fvX, y: Y, text: fv.label, size: 14, style: 'Medium', color: '#0F172A' });
    const fvBox = makeFrame(main, { name: `fv-input`, x: fvX, y: Y + 22, w: 360, h: 40, fill: '#FFFFFF', radius: 8, strokeColor: '#EF4444' });
    fvBox.strokes = [{ type: 'SOLID', color: hexToRgb('#EF4444') }];
    fvBox.strokeWeight = 1;
    fvBox.strokeAlign = 'INSIDE';
    addText(fvBox, { x: 12, y: 11, text: fv.placeholder || '입력하세요', size: 14, style: 'Regular', color: fv.placeholder ? '#0F172A' : '#94A3B8' });
    addText(main, { x: fvX, y: Y + 66, text: fv.error, size: 13, style: 'Regular', color: '#EF4444' });
    fvX += 360 + 40;
  }
  Y += 40 + 36 + 24;

  // Page-level error banner
  addText(main, { x: PAD_X, y: Y, text: 'PAGE-LEVEL ERROR BANNER', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  Y += 20;

  const pgBannerW = 1200;
  const pgBannerF = makeFrame(main, { name: 'page-error-banner', x: PAD_X, y: Y, w: pgBannerW, h: 56, fill: '#FEF2F2', radius: 8 });
  addRect(pgBannerF, { name: 'pg-err-bar', x: 0, y: 0, w: 6, h: 56, fill: '#EF4444', radius: 2 });
  addRect(pgBannerF, { name: 'pg-err-icon-bg', x: 18, y: 16, w: 24, h: 24, fill: '#EF4444', radius: 12 });
  addText(pgBannerF, { x: 18, y: 20, text: '!', size: 13, style: 'Bold', color: '#FFFFFF', w: 24, align: 'CENTER' });
  addText(pgBannerF, { x: 56, y: 19, text: 'API 서버와의 연결에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.', size: 14, style: 'Regular', color: '#991B1B', w: pgBannerW - 120 });
  addRect(pgBannerF, { name: 'pg-err-close', x: pgBannerW - 40, y: 18, w: 20, h: 20, fill: '#FCA5A5', radius: 10 });

  // ── Done ──────────────────────────────────────────────────────────────────
  figma.viewport.scrollAndZoomIntoView([main]);
  figma.closePlugin('Component Sheet 생성 완료 — 10개 섹션');
})();

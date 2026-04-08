(async () => {
  // ── Helper ────────────────────────────────────────────────────────────────
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

  // ── Load fonts ────────────────────────────────────────────────────────────
  await Promise.all([
    figma.loadFontAsync({ family: 'Inter', style: 'Regular' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Medium' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Extra Bold' }),
  ]);

  // ── Create / select page ──────────────────────────────────────────────────
  const page = figma.currentPage;
  page.name = 'Design System Overview';

  // ── Main frame ────────────────────────────────────────────────────────────
  const FRAME_W = 1440;
  const FRAME_H = 4200;
  const PADDING_X = 80;
  const SECTION_GAP = 64;

  const mainFrame = figma.createFrame();
  mainFrame.name = 'Design System Overview - PC (1440px)';
  mainFrame.resize(FRAME_W, FRAME_H);
  mainFrame.fills = solid('#FFFFFF');
  mainFrame.x = 0;
  mainFrame.y = 0;
  page.appendChild(mainFrame);

  // ── Utility: add text node ────────────────────────────────────────────────
  function addText(parent, opts) {
    const t = figma.createText();
    t.x = opts.x;
    t.y = opts.y;
    t.characters = opts.text;
    t.fontSize = opts.size || 14;
    t.fontName = { family: 'Inter', style: opts.style || 'Regular' };
    t.fills = solid(opts.color || '#0F172A');
    if (opts.w) { t.textAutoResize = 'HEIGHT'; t.resize(opts.w, t.height); }
    parent.appendChild(t);
    return t;
  }

  // ── Utility: add rect ────────────────────────────────────────────────────
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
    }
    parent.appendChild(r);
    return r;
  }

  // ── Section label ─────────────────────────────────────────────────────────
  function sectionLabel(parent, x, y, label) {
    addText(parent, { x, y, text: label, size: 11, style: 'Semi Bold', color: '#94A3B8', w: 300 });
  }

  let cursorY = 64;

  // ─────────────────────────────────────────────────────────────────────────
  // SECTION 1 — Header
  // ─────────────────────────────────────────────────────────────────────────
  addText(mainFrame, {
    x: PADDING_X, y: cursorY,
    text: 'Wisecan Design System',
    size: 36, style: 'Extra Bold', color: '#0F172A', w: FRAME_W - PADDING_X * 2,
  });
  addText(mainFrame, {
    x: PADDING_X, y: cursorY + 50,
    text: 'Sprint 1 — 디자인 토큰 정의 v1.0.0',
    size: 16, style: 'Regular', color: '#64748B', w: FRAME_W - PADDING_X * 2,
  });

  cursorY += 120 + SECTION_GAP;

  // ─────────────────────────────────────────────────────────────────────────
  // SECTION 2 — Color Palette
  // ─────────────────────────────────────────────────────────────────────────
  addText(mainFrame, { x: PADDING_X, y: cursorY, text: 'Color Palette', size: 24, style: 'Semi Bold', color: '#0F172A' });
  cursorY += 40;

  const colorGroups = [
    {
      label: 'Primary',
      swatches: [
        { name: 'Primary', hex: '#2563EB', tw: 'blue-600' },
        { name: 'Primary Dark', hex: '#1D4ED8', tw: 'blue-700' },
        { name: 'Primary Light', hex: '#3B82F6', tw: 'blue-500' },
        { name: 'Extra Light', hex: '#EFF6FF', tw: 'blue-50' },
      ],
    },
    {
      label: 'Neutral',
      swatches: [
        { name: 'White', hex: '#FFFFFF', tw: 'white' },
        { name: 'Surface', hex: '#F8FAFC', tw: 'slate-50' },
        { name: 'Surface Hover', hex: '#F1F5F9', tw: 'slate-100' },
        { name: 'Border', hex: '#E2E8F0', tw: 'slate-200' },
        { name: 'Border Strong', hex: '#CBD5E1', tw: 'slate-300' },
      ],
    },
    {
      label: 'Text',
      swatches: [
        { name: 'Text Primary', hex: '#0F172A', tw: 'slate-900' },
        { name: 'Text Secondary', hex: '#64748B', tw: 'slate-500' },
        { name: 'Text Disabled', hex: '#94A3B8', tw: 'slate-400' },
      ],
    },
    {
      label: 'Semantic',
      swatches: [
        { name: 'Success', hex: '#22C55E', tw: 'green-500' },
        { name: 'Error', hex: '#EF4444', tw: 'red-500' },
        { name: 'Warning', hex: '#F59E0B', tw: 'amber-500' },
        { name: 'Info', hex: '#3B82F6', tw: 'blue-500' },
      ],
    },
  ];

  const SWATCH_SIZE = 80;
  const SWATCH_GAP = 12;
  const GROUP_GAP = 40;

  let groupX = PADDING_X;
  for (const group of colorGroups) {
    sectionLabel(mainFrame, groupX, cursorY, group.label.toUpperCase());
    let swatchX = groupX;
    for (const sw of group.swatches) {
      addRect(mainFrame, {
        name: sw.name, x: swatchX, y: cursorY + 20,
        w: SWATCH_SIZE, h: SWATCH_SIZE,
        fill: sw.hex, radius: 8,
        strokeColor: sw.hex === '#FFFFFF' || sw.hex === '#F8FAFC' || sw.hex === '#F1F5F9' ? '#E2E8F0' : undefined,
        strokeWeight: 1,
      });
      addText(mainFrame, {
        x: swatchX, y: cursorY + 20 + SWATCH_SIZE + 6,
        text: sw.hex, size: 10, style: 'Regular', color: '#64748B',
      });
      addText(mainFrame, {
        x: swatchX, y: cursorY + 20 + SWATCH_SIZE + 18,
        text: sw.tw, size: 9, style: 'Regular', color: '#94A3B8',
      });
      swatchX += SWATCH_SIZE + SWATCH_GAP;
    }
    groupX = swatchX + GROUP_GAP;
  }

  cursorY += 20 + SWATCH_SIZE + 40 + SECTION_GAP;

  // ─────────────────────────────────────────────────────────────────────────
  // SECTION 3 — Typography Scale
  // ─────────────────────────────────────────────────────────────────────────
  addText(mainFrame, { x: PADDING_X, y: cursorY, text: 'Typography Scale (Inter)', size: 24, style: 'Semi Bold', color: '#0F172A' });
  cursorY += 44;

  // Header row
  addText(mainFrame, { x: PADDING_X, y: cursorY, text: 'TOKEN', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  addText(mainFrame, { x: PADDING_X + 120, y: cursorY, text: 'SIZE / WEIGHT', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  addText(mainFrame, { x: PADDING_X + 260, y: cursorY, text: 'SAMPLE', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  addText(mainFrame, { x: PADDING_X + 900, y: cursorY, text: 'USAGE', size: 11, style: 'Semi Bold', color: '#94A3B8' });
  cursorY += 20;

  const typoRows = [
    { token: 'Display', size: 36, style: 'Extra Bold', spec: '36px / 800', sample: 'MCP 도구로 연결하는 새로운 방식', usage: 'Hero Title' },
    { token: 'H1', size: 30, style: 'Bold', spec: '30px / 700', sample: 'API 키 관리', usage: 'Page Title' },
    { token: 'H2', size: 24, style: 'Semi Bold', spec: '24px / 600', sample: '최근 호출 이력', usage: 'Section Title' },
    { token: 'H3', size: 20, style: 'Semi Bold', spec: '20px / 600', sample: '오늘 API 호출', usage: 'Card Title' },
    { token: 'Body1', size: 16, style: 'Regular', spec: '16px / 400', sample: 'Wisecan은 MCP 도구 기반 메시지 발송, 에이전트 진단, 파일 변환 서비스를 제공합니다.', usage: 'Body Text' },
    { token: 'Body2', size: 14, style: 'Regular', spec: '14px / 400', sample: 'API 키는 생성 후 다시 확인할 수 없습니다.', usage: 'Secondary Text' },
    { token: 'Label', size: 14, style: 'Medium', spec: '14px / 500', sample: '이메일 *', usage: 'Form Label' },
    { token: 'Caption', size: 12, style: 'Regular', spec: '12px / 400', sample: '예) user@wisecan.co.kr', usage: 'Hint / Timestamp' },
  ];

  for (const row of typoRows) {
    addRect(mainFrame, { name: 'divider', x: PADDING_X, y: cursorY, w: FRAME_W - PADDING_X * 2, h: 1, fill: '#F1F5F9', radius: 0 });
    cursorY += 8;
    addText(mainFrame, { x: PADDING_X, y: cursorY, text: row.token, size: 12, style: 'Medium', color: '#64748B' });
    addText(mainFrame, { x: PADDING_X + 120, y: cursorY, text: row.spec, size: 12, style: 'Regular', color: '#94A3B8' });
    addText(mainFrame, { x: PADDING_X + 260, y: cursorY, text: row.sample, size: row.size, style: row.style, color: '#0F172A', w: 620 });
    addText(mainFrame, { x: PADDING_X + 900, y: cursorY, text: row.usage, size: 12, style: 'Regular', color: '#94A3B8' });
    cursorY += row.size + 20;
  }

  cursorY += SECTION_GAP;

  // ─────────────────────────────────────────────────────────────────────────
  // SECTION 4 — Button Variants
  // ─────────────────────────────────────────────────────────────────────────
  addText(mainFrame, { x: PADDING_X, y: cursorY, text: 'Button Styles', size: 24, style: 'Semi Bold', color: '#0F172A' });
  cursorY += 44;

  const buttons = [
    { label: 'API Key 발급', bg: '#2563EB', textColor: '#FFFFFF', variant: 'Primary', w: 140 },
    { label: '내보내기', bg: '#FFFFFF', textColor: '#0F172A', variant: 'Secondary', w: 110, stroke: '#E2E8F0' },
    { label: '취소', bg: 'transparent', textColor: '#64748B', variant: 'Ghost', w: 80, stroke: '#E2E8F0' },
    { label: '키 비활성화', bg: '#EF4444', textColor: '#FFFFFF', variant: 'Destructive', w: 130 },
  ];

  let btnX = PADDING_X;
  sectionLabel(mainFrame, PADDING_X, cursorY, 'VARIANTS');
  cursorY += 20;

  for (const btn of buttons) {
    const bgColor = btn.bg === 'transparent' ? '#F8FAFC' : btn.bg;
    addRect(mainFrame, {
      name: btn.variant, x: btnX, y: cursorY,
      w: btn.w, h: 40, fill: bgColor, radius: 8,
      strokeColor: btn.stroke, strokeWeight: 1,
    });
    addText(mainFrame, {
      x: btnX + 12, y: cursorY + 12,
      text: btn.label, size: 14, style: 'Medium', color: btn.textColor,
    });
    addText(mainFrame, { x: btnX, y: cursorY + 48, text: btn.variant, size: 10, style: 'Regular', color: '#94A3B8' });
    btnX += btn.w + 16;
  }
  cursorY += 40 + 32;

  // Sizes row
  sectionLabel(mainFrame, PADDING_X, cursorY, 'SIZES');
  cursorY += 20;

  const sizes = [
    { label: 'Small', h: 32, w: 80, fontSize: 13 },
    { label: 'Medium', h: 40, w: 96, fontSize: 14 },
    { label: 'Large', h: 48, w: 112, fontSize: 16 },
  ];
  let szX = PADDING_X;
  for (const sz of sizes) {
    addRect(mainFrame, { name: sz.label, x: szX, y: cursorY, w: sz.w, h: sz.h, fill: '#2563EB', radius: 8 });
    addText(mainFrame, { x: szX + 12, y: cursorY + (sz.h - sz.fontSize) / 2, text: sz.label, size: sz.fontSize, style: 'Medium', color: '#FFFFFF' });
    addText(mainFrame, { x: szX, y: cursorY + sz.h + 8, text: `h: ${sz.h}px`, size: 10, style: 'Regular', color: '#94A3B8' });
    szX += sz.w + 16;
  }

  cursorY += 48 + 28 + SECTION_GAP;

  // ─────────────────────────────────────────────────────────────────────────
  // SECTION 5 — Input Fields
  // ─────────────────────────────────────────────────────────────────────────
  addText(mainFrame, { x: PADDING_X, y: cursorY, text: 'Input Fields', size: 24, style: 'Semi Bold', color: '#0F172A' });
  cursorY += 44;

  const inputs = [
    { label: '이메일 *', placeholder: '예) user@wisecan.co.kr', state: 'Default', border: '#E2E8F0', bg: '#FFFFFF' },
    { label: '비밀번호 *', placeholder: '8자 이상 입력', state: 'Default', border: '#E2E8F0', bg: '#FFFFFF' },
    { label: '이메일 *', placeholder: '예) user@wisecan.co.kr', state: 'Focus (ring: blue-500)', border: '#2563EB', bg: '#FFFFFF', ring: true },
    { label: '이메일 *', placeholder: 'invalid-email', state: 'Error', border: '#EF4444', bg: '#FFFFFF', errorMsg: '올바른 이메일 형식이 아닙니다' },
    { label: 'API Key 별칭', placeholder: '예) 프로덕션 서버 키', state: 'Default', border: '#E2E8F0', bg: '#FFFFFF' },
  ];

  const INPUT_W = 320;
  const INPUT_H = 40;
  const INPUT_COL_GAP = 40;
  let inputX = PADDING_X;
  let inputRowY = cursorY;

  for (let i = 0; i < inputs.length; i++) {
    const inp = inputs[i];
    const col = i % 3;
    const row = Math.floor(i / 3);
    const ix = PADDING_X + col * (INPUT_W + INPUT_COL_GAP);
    const iy = inputRowY + row * 100;

    addText(mainFrame, { x: ix, y: iy, text: inp.label, size: 13, style: 'Medium', color: '#0F172A' });
    addRect(mainFrame, {
      name: `input-${inp.state}`, x: ix, y: iy + 22,
      w: INPUT_W, h: INPUT_H, fill: inp.bg, radius: 8,
      strokeColor: inp.border, strokeWeight: inp.ring ? 2 : 1,
    });
    addText(mainFrame, { x: ix + 12, y: iy + 32, text: inp.placeholder, size: 13, style: 'Regular', color: '#94A3B8' });
    addText(mainFrame, { x: ix, y: iy + 66, text: `State: ${inp.state}`, size: 10, style: 'Regular', color: '#94A3B8' });
    if (inp.errorMsg) {
      addText(mainFrame, { x: ix, y: iy + 64, text: inp.errorMsg, size: 11, style: 'Regular', color: '#EF4444' });
    }
  }

  cursorY += Math.ceil(inputs.length / 3) * 100 + SECTION_GAP;

  // ─────────────────────────────────────────────────────────────────────────
  // SECTION 6 — Card Components
  // ─────────────────────────────────────────────────────────────────────────
  addText(mainFrame, { x: PADDING_X, y: cursorY, text: 'Card Components', size: 24, style: 'Semi Bold', color: '#0F172A' });
  cursorY += 44;

  // Stat Card
  addRect(mainFrame, { name: 'Stat Card', x: PADDING_X, y: cursorY, w: 280, h: 120, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(mainFrame, { x: PADDING_X + 20, y: cursorY + 20, text: '오늘 API 호출', size: 12, style: 'Medium', color: '#64748B' });
  addText(mainFrame, { x: PADDING_X + 20, y: cursorY + 42, text: '1,234건', size: 28, style: 'Bold', color: '#0F172A' });
  addText(mainFrame, { x: PADDING_X + 20, y: cursorY + 82, text: '+12.5% 전일 대비', size: 12, style: 'Regular', color: '#22C55E' });
  addText(mainFrame, { x: PADDING_X, y: cursorY + 128, text: 'Stat Card', size: 10, style: 'Regular', color: '#94A3B8' });

  // Default Card (bordered)
  addRect(mainFrame, { name: 'Default Card', x: PADDING_X + 312, y: cursorY, w: 320, h: 120, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(mainFrame, { x: PADDING_X + 332, y: cursorY + 20, text: '최근 호출 이력', size: 16, style: 'Semi Bold', color: '#0F172A' });
  addText(mainFrame, { x: PADDING_X + 332, y: cursorY + 48, text: '테이블 컨텐츠 영역', size: 13, style: 'Regular', color: '#94A3B8' });
  addRect(mainFrame, { name: 'table-placeholder', x: PADDING_X + 332, y: cursorY + 70, w: 280, h: 32, fill: '#F8FAFC', radius: 4 });
  addText(mainFrame, { x: PADDING_X + 312, y: cursorY + 128, text: 'Default Card (bordered)', size: 10, style: 'Regular', color: '#94A3B8' });

  // Elevated Card
  addRect(mainFrame, { name: 'Elevated Card', x: PADDING_X + 664, y: cursorY, w: 280, h: 120, fill: '#FFFFFF', radius: 12 });
  addText(mainFrame, { x: PADDING_X + 684, y: cursorY + 20, text: '활성 API Key', size: 16, style: 'Semi Bold', color: '#0F172A' });
  addText(mainFrame, { x: PADDING_X + 684, y: cursorY + 48, text: '3개', size: 28, style: 'Bold', color: '#0F172A' });
  addText(mainFrame, { x: PADDING_X + 664, y: cursorY + 128, text: 'Elevated Card (shadow-md)', size: 10, style: 'Regular', color: '#94A3B8' });

  cursorY += 120 + 28 + SECTION_GAP;

  // ─────────────────────────────────────────────────────────────────────────
  // SECTION 7 — Badge & Status Indicators
  // ─────────────────────────────────────────────────────────────────────────
  addText(mainFrame, { x: PADDING_X, y: cursorY, text: 'Badge & Status Indicators', size: 24, style: 'Semi Bold', color: '#0F172A' });
  cursorY += 44;

  const badges = [
    { label: 'Active', bg: '#F0FDF4', textColor: '#16A34A' },
    { label: 'Revoked', bg: '#FEF2F2', textColor: '#DC2626' },
    { label: 'Pending', bg: '#FFFBEB', textColor: '#D97706' },
    { label: '성공', bg: '#F0FDF4', textColor: '#16A34A' },
    { label: '실패', bg: '#FEF2F2', textColor: '#DC2626' },
    { label: '베타', bg: '#EFF6FF', textColor: '#1D4ED8' },
    { label: '신규', bg: '#F1F5F9', textColor: '#475569' },
  ];

  let badgeX = PADDING_X;
  for (const badge of badges) {
    const badgeW = badge.label.length * 8 + 20;
    addRect(mainFrame, { name: `badge-${badge.label}`, x: badgeX, y: cursorY, w: badgeW, h: 24, fill: badge.bg, radius: 9999 });
    addText(mainFrame, { x: badgeX + 8, y: cursorY + 6, text: badge.label, size: 12, style: 'Medium', color: badge.textColor });
    badgeX += badgeW + 12;
  }

  // ── Done ──────────────────────────────────────────────────────────────────
  figma.viewport.scrollAndZoomIntoView([mainFrame]);
  figma.closePlugin('Wisecan Design System 생성 완료');
})();

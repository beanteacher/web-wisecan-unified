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

  // ── Load fonts ────────────────────────────────────────────────────────────
  await Promise.all([
    figma.loadFontAsync({ family: 'Inter', style: 'Regular' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Medium' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Extra Bold' }),
  ]);

  const page = figma.currentPage;
  page.name = 'Dashboard Wireframe';

  // ── Utility helpers ───────────────────────────────────────────────────────
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

  function makeFrame(name, w, h, x, y, bg) {
    const f = figma.createFrame();
    f.name = name;
    f.resize(w, h);
    f.x = x;
    f.y = y;
    f.fills = solid(bg || '#F8FAFC');
    page.appendChild(f);
    return f;
  }

  // ── Sidebar builder ───────────────────────────────────────────────────────
  const SIDEBAR_W = 240;
  const NAV_ITEMS = [
    { label: '대시보드', active: true },
    { label: 'API Keys', active: false },
    { label: '메시지 도구', active: false },
    { label: '메시지 통계', active: false },
    { label: '파일 변환', active: false },
    { label: '설정', active: false },
  ];

  function buildSidebar(frame, h, activeItem) {
    // Sidebar bg + border
    addRect(frame, { name: 'sidebar-bg', x: 0, y: 0, w: SIDEBAR_W, h, fill: '#FFFFFF', strokeColor: '#E2E8F0', strokeWeight: 1 });

    // Logo
    addText(frame, { x: 20, y: 22, text: 'Wisecan', size: 18, style: 'Bold', color: '#0F172A' });

    // Nav items
    let navY = 72;
    for (const item of NAV_ITEMS) {
      const isActive = item.label === activeItem;
      if (isActive) {
        addRect(frame, { name: `nav-active-${item.label}`, x: 8, y: navY - 2, w: SIDEBAR_W - 16, h: 36, fill: '#EFF6FF', radius: 8 });
      }
      addText(frame, {
        x: 24, y: navY + 8,
        text: item.label, size: 14,
        style: isActive ? 'Semi Bold' : 'Regular',
        color: isActive ? '#2563EB' : '#64748B',
      });
      navY += 44;
    }

    // User profile at bottom
    addRect(frame, { name: 'user-area', x: 8, y: h - 72, w: SIDEBAR_W - 16, h: 56, fill: '#F8FAFC', radius: 8 });
    addRect(frame, { name: 'avatar', x: 20, y: h - 62, w: 36, h: 36, fill: '#EFF6FF', radius: 18 });
    addText(frame, { x: 29, y: h - 53, text: 'KW', size: 12, style: 'Semi Bold', color: '#2563EB' });
    addText(frame, { x: 64, y: h - 60, text: '김위즈캔', size: 13, style: 'Semi Bold', color: '#0F172A' });
    addText(frame, { x: 64, y: h - 44, text: 'kim@wisecan.co.kr', size: 11, style: 'Regular', color: '#94A3B8' });
  }

  // ── Stat card builder ─────────────────────────────────────────────────────
  function buildStatCard(parent, x, y, w, h, label, value, change, changePositive) {
    addRect(parent, { name: `stat-${label}`, x, y, w, h, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
    addText(parent, { x: x + 20, y: y + 20, text: label, size: 12, style: 'Medium', color: '#64748B' });
    addText(parent, { x: x + 20, y: y + 42, text: value, size: 22, style: 'Bold', color: '#0F172A' });
    addText(parent, { x: x + 20, y: y + h - 28, text: change, size: 12, style: 'Regular', color: changePositive ? '#22C55E' : '#64748B' });
  }

  // ── Table header row ──────────────────────────────────────────────────────
  function buildTableHeader(parent, x, y, w, cols) {
    addRect(parent, { name: 'table-header-bg', x, y, w, h: 40, fill: '#F8FAFC' });
    let colX = x + 20;
    for (const col of cols) {
      addText(parent, { x: colX, y: y + 13, text: col.label.toUpperCase(), size: 11, style: 'Semi Bold', color: '#64748B' });
      colX += col.w;
    }
  }

  function buildTableRow(parent, x, y, w, cells, cols, isEven) {
    if (isEven) addRect(parent, { name: 'row-bg', x, y, w, h: 48, fill: '#FAFAFA' });
    addRect(parent, { name: 'row-border', x, y: y + 47, w, h: 1, fill: '#F1F5F9' });
    let colX = x + 20;
    for (let i = 0; i < cells.length; i++) {
      const cell = cells[i];
      if (cell.badge) {
        const bg = cell.badge === 'success' ? '#F0FDF4' : '#FEF2F2';
        const tc = cell.badge === 'success' ? '#16A34A' : '#DC2626';
        addRect(parent, { name: 'badge', x: colX, y: y + 14, w: 48, h: 20, fill: bg, radius: 9999 });
        addText(parent, { x: colX + 8, y: y + 17, text: cell.text, size: 11, style: 'Medium', color: tc });
      } else {
        addText(parent, { x: colX, y: y + 16, text: cell.text, size: 13, style: 'Regular', color: '#0F172A' });
      }
      colX += cols[i].w;
    }
  }

  // ── Badge helper ──────────────────────────────────────────────────────────
  function addBadge(parent, x, y, label, variant) {
    const bg = variant === 'success' ? '#F0FDF4' : variant === 'error' ? '#FEF2F2' : '#F1F5F9';
    const tc = variant === 'success' ? '#16A34A' : variant === 'error' ? '#DC2626' : '#475569';
    const bw = label.length * 8 + 20;
    addRect(parent, { name: `badge-${label}`, x, y, w: bw, h: 22, fill: bg, radius: 9999 });
    addText(parent, { x: x + 8, y: y + 5, text: label, size: 11, style: 'Medium', color: tc });
    return bw;
  }

  // ── Input field ───────────────────────────────────────────────────────────
  function addInput(parent, x, y, w, label, placeholder) {
    addText(parent, { x, y, text: label, size: 13, style: 'Medium', color: '#0F172A' });
    addRect(parent, { name: `input-${label}`, x, y: y + 22, w, h: 40, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
    addText(parent, { x: x + 12, y: y + 32, text: placeholder, size: 13, style: 'Regular', color: '#94A3B8' });
  }

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 1: Dashboard - PC (1440 x 900)
  // ════════════════════════════════════════════════════════════════════════
  const dbPC = makeFrame('Dashboard - PC (1440px)', 1440, 900, 0, 0, '#F8FAFC');
  buildSidebar(dbPC, 900, '대시보드');

  const CONTENT_X = SIDEBAR_W + 32;
  const CONTENT_W = 1440 - SIDEBAR_W - 64;
  let cy = 32;

  // Page header
  addText(dbPC, { x: CONTENT_X, y: cy, text: '대시보드', size: 24, style: 'Bold', color: '#0F172A' });
  addText(dbPC, { x: CONTENT_X, y: cy + 34, text: '2026년 4월 7일 기준 서비스 현황', size: 13, style: 'Regular', color: '#64748B' });
  cy += 80;

  // Stat cards (4 columns)
  const STAT_W = Math.floor((CONTENT_W - 48) / 4);
  const statCards = [
    { label: '오늘 API 호출', value: '1,234건', change: '+12.5% 전일 대비', pos: true },
    { label: '성공률', value: '98.5%', change: '+0.3%p 전일 대비', pos: true },
    { label: '활성 API Key', value: '3개', change: '변동 없음', pos: false },
    { label: '이번 달 총 호출', value: '45,678건', change: '+8.2% 전월 대비', pos: true },
  ];
  for (let i = 0; i < 4; i++) {
    buildStatCard(dbPC, CONTENT_X + i * (STAT_W + 16), cy, STAT_W, 100, statCards[i].label, statCards[i].value, statCards[i].change, statCards[i].pos);
  }
  cy += 116;

  // Chart area
  addRect(dbPC, { name: 'chart-card', x: CONTENT_X, y: cy, w: CONTENT_W, h: 280, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(dbPC, { x: CONTENT_X + 24, y: cy + 20, text: '최근 7일 API 호출 추이', size: 16, style: 'Semi Bold', color: '#0F172A' });
  addText(dbPC, { x: CONTENT_X + 24, y: cy + 44, text: '2026.03.31 ~ 2026.04.07', size: 12, style: 'Regular', color: '#64748B' });

  // Chart bars (simplified area chart representation)
  const chartData = [980, 1120, 890, 1340, 1180, 760, 1050, 1234];
  const chartLabels = ['3/31', '4/1', '4/2', '4/3', '4/4', '4/5', '4/6', '4/7'];
  const chartMaxVal = 1400;
  const chartH = 160;
  const chartTop = cy + 76;
  const chartLeft = CONTENT_X + 48;
  const barW = Math.floor((CONTENT_W - 120) / 8);

  for (let i = 0; i < 8; i++) {
    const barH = Math.floor((chartData[i] / chartMaxVal) * chartH);
    // Bar
    addRect(dbPC, {
      name: `bar-${i}`, x: chartLeft + i * (barW + 8), y: chartTop + chartH - barH,
      w: barW, h: barH, fill: '#DBEAFE', radius: 4,
    });
    // Top accent
    addRect(dbPC, {
      name: `bar-top-${i}`, x: chartLeft + i * (barW + 8), y: chartTop + chartH - barH,
      w: barW, h: 3, fill: '#2563EB', radius: 2,
    });
    // X axis label
    addText(dbPC, { x: chartLeft + i * (barW + 8), y: chartTop + chartH + 8, text: chartLabels[i], size: 11, style: 'Regular', color: '#94A3B8' });
  }
  cy += 296;

  // Recent calls table
  addRect(dbPC, { name: 'table-card', x: CONTENT_X, y: cy, w: CONTENT_W, h: 320, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(dbPC, { x: CONTENT_X + 24, y: cy + 20, text: '최근 호출 이력', size: 16, style: 'Semi Bold', color: '#0F172A' });
  addText(dbPC, { x: CONTENT_X + 24, y: cy + 44, text: '실시간 업데이트', size: 12, style: 'Regular', color: '#64748B' });

  const tableCols = [
    { label: '시간', w: 120 },
    { label: 'API Key', w: 180 },
    { label: '도구명', w: 200 },
    { label: '상태', w: 120 },
    { label: '응답시간', w: 120 },
  ];
  buildTableHeader(dbPC, CONTENT_X, cy + 68, CONTENT_W, tableCols);

  const tableRows = [
    [{ text: '14:23:05' }, { text: 'wc_a1b2c3d4' }, { text: 'message_send' }, { text: '성공', badge: 'success' }, { text: '234ms' }],
    [{ text: '14:22:51' }, { text: 'wc_e5f6g7h8' }, { text: 'agent_diagnose' }, { text: '성공', badge: 'success' }, { text: '412ms' }],
    [{ text: '14:21:38' }, { text: 'wc_a1b2c3d4' }, { text: 'file_convert' }, { text: '실패', badge: 'error' }, { text: '1,203ms' }],
    [{ text: '14:20:17' }, { text: 'wc_e5f6g7h8' }, { text: 'message_send' }, { text: '성공', badge: 'success' }, { text: '189ms' }],
    [{ text: '14:19:44' }, { text: 'wc_a1b2c3d4' }, { text: 'message_send' }, { text: '성공', badge: 'success' }, { text: '201ms' }],
  ];
  for (let i = 0; i < tableRows.length; i++) {
    buildTableRow(dbPC, CONTENT_X, cy + 108 + i * 48, CONTENT_W, tableRows[i], tableCols, i % 2 === 1);
  }

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 2: Dashboard - Mobile (375 x 812)
  // ════════════════════════════════════════════════════════════════════════
  const dbMobile = makeFrame('Dashboard - Mobile (375px)', 375, 812, 1540, 0, '#F8FAFC');

  // Top bar
  addRect(dbMobile, { name: 'topbar', x: 0, y: 0, w: 375, h: 56, fill: '#FFFFFF', strokeColor: '#E2E8F0', strokeWeight: 1 });
  addRect(dbMobile, { name: 'hamburger', x: 16, y: 16, w: 24, h: 24, fill: '#64748B', radius: 2 });
  addText(dbMobile, { x: 140, y: 18, text: 'Wisecan', size: 18, style: 'Bold', color: '#0F172A' });
  addRect(dbMobile, { name: 'avatar', x: 322, y: 12, w: 32, h: 32, fill: '#EFF6FF', radius: 16 });
  addText(dbMobile, { x: 330, y: 20, text: 'KW', size: 11, style: 'Semi Bold', color: '#2563EB' });

  let mDbY = 72;
  addText(dbMobile, { x: 16, y: mDbY, text: '대시보드', size: 20, style: 'Bold', color: '#0F172A' });
  addText(dbMobile, { x: 16, y: mDbY + 28, text: '4월 7일 기준', size: 13, style: 'Regular', color: '#64748B' });
  mDbY += 64;

  // Stat cards 2x2 grid
  const M_STAT_W = (375 - 48) / 2;
  const mStatCards = [
    { label: '오늘 API 호출', value: '1,234건', change: '+12.5%', pos: true },
    { label: '성공률', value: '98.5%', change: '+0.3%p', pos: true },
    { label: '활성 API Key', value: '3개', change: '-', pos: false },
    { label: '이번 달 총 호출', value: '45,678건', change: '+8.2%', pos: true },
  ];
  for (let i = 0; i < 4; i++) {
    const col = i % 2;
    const row = Math.floor(i / 2);
    buildStatCard(dbMobile, 16 + col * (M_STAT_W + 12), mDbY + row * 112, M_STAT_W, 100, mStatCards[i].label, mStatCards[i].value, mStatCards[i].change, mStatCards[i].pos);
  }
  mDbY += 240;

  // Chart (simplified)
  addRect(dbMobile, { name: 'chart-card', x: 16, y: mDbY, w: 343, h: 200, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(dbMobile, { x: 32, y: mDbY + 16, text: '최근 7일 API 호출 추이', size: 14, style: 'Semi Bold', color: '#0F172A' });
  const mChartH = 110;
  const mChartTop = mDbY + 52;
  const mChartLeft = 32;
  const mBarW = 30;
  for (let i = 0; i < 8; i++) {
    const bh = Math.floor((chartData[i] / chartMaxVal) * mChartH);
    addRect(dbMobile, { name: `mbar-${i}`, x: mChartLeft + i * 38, y: mChartTop + mChartH - bh, w: mBarW, h: bh, fill: '#DBEAFE', radius: 3 });
    addRect(dbMobile, { name: `mbar-top-${i}`, x: mChartLeft + i * 38, y: mChartTop + mChartH - bh, w: mBarW, h: 3, fill: '#2563EB', radius: 2 });
  }
  addText(dbMobile, { x: 32, y: mChartTop + mChartH + 8, text: '3/31', size: 10, style: 'Regular', color: '#94A3B8' });
  addText(dbMobile, { x: 155, y: mChartTop + mChartH + 8, text: '4/4', size: 10, style: 'Regular', color: '#94A3B8' });
  addText(dbMobile, { x: 305, y: mChartTop + mChartH + 8, text: '4/7', size: 10, style: 'Regular', color: '#94A3B8' });
  mDbY += 216;

  // Mobile table (simplified, 3 cols)
  addRect(dbMobile, { name: 'table-card', x: 16, y: mDbY, w: 343, h: 220, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(dbMobile, { x: 32, y: mDbY + 16, text: '최근 호출 이력', size: 14, style: 'Semi Bold', color: '#0F172A' });

  const mCols = [{ label: '시간', w: 80 }, { label: '도구명', w: 140 }, { label: '상태', w: 100 }];
  buildTableHeader(dbMobile, 16, mDbY + 48, 343, mCols);
  const mRows = [
    [{ text: '14:23:05' }, { text: 'message_send' }, { text: '성공', badge: 'success' }],
    [{ text: '14:22:51' }, { text: 'agent_diagnose' }, { text: '성공', badge: 'success' }],
    [{ text: '14:21:38' }, { text: 'file_convert' }, { text: '실패', badge: 'error' }],
  ];
  for (let i = 0; i < mRows.length; i++) {
    buildTableRow(dbMobile, 16, mDbY + 88 + i * 44, 343, mRows[i], mCols, i % 2 === 1);
  }

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 3: API Keys - PC (1440 x 900)
  // ════════════════════════════════════════════════════════════════════════
  const apiPC = makeFrame('API Keys - PC (1440px)', 1440, 900, 3080, 0, '#F8FAFC');
  buildSidebar(apiPC, 900, 'API Keys');

  let apicy = 32;
  // Page header
  addText(apiPC, { x: CONTENT_X, y: apicy, text: 'API Keys', size: 24, style: 'Bold', color: '#0F172A' });
  addText(apiPC, { x: CONTENT_X, y: apicy + 34, text: 'API 키를 생성하고 관리하세요. 키는 생성 시 한 번만 확인 가능합니다.', size: 13, style: 'Regular', color: '#64748B', w: 700 });
  // New key button
  addRect(apiPC, { name: 'btn-new-key', x: CONTENT_X + CONTENT_W - 120, y: apicy, w: 120, h: 40, fill: '#2563EB', radius: 8 });
  addText(apiPC, { x: CONTENT_X + CONTENT_W - 96, y: apicy + 13, text: '+ 새 키 발급', size: 13, style: 'Semi Bold', color: '#FFFFFF' });
  apicy += 80;

  // Table card
  addRect(apiPC, { name: 'apikeys-table-card', x: CONTENT_X, y: apicy, w: CONTENT_W, h: 300, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });

  const apiCols = [
    { label: '별칭', w: 200 },
    { label: '접두사', w: 180 },
    { label: '상태', w: 120 },
    { label: '마지막 사용', w: 160 },
    { label: '생성일', w: 140 },
    { label: '', w: 140 },
  ];
  buildTableHeader(apiPC, CONTENT_X, apicy, CONTENT_W, apiCols);

  const apiRows = [
    [{ text: '프로덕션 키' }, { text: 'wc_a1b2c3d4' }, { text: 'Active', badge: 'success' }, { text: '2분 전' }, { text: '2024-03-15' }, { text: '비활성화' }],
    [{ text: '테스트 키' }, { text: 'wc_e5f6g7h8' }, { text: 'Active', badge: 'success' }, { text: '1시간 전' }, { text: '2024-03-10' }, { text: '비활성화' }],
    [{ text: '레거시 키' }, { text: 'wc_i9j0k1l2' }, { text: 'Revoked', badge: 'error' }, { text: '-' }, { text: '2024-02-01' }, { text: '' }],
  ];
  for (let i = 0; i < apiRows.length; i++) {
    buildTableRow(apiPC, CONTENT_X, apicy + 40 + i * 56, CONTENT_W, apiRows[i], apiCols, i % 2 === 1);
  }
  apicy += 316;

  // Info banner
  addRect(apiPC, { name: 'info-banner', x: CONTENT_X, y: apicy, w: CONTENT_W, h: 44, fill: '#EFF6FF', radius: 8, strokeColor: '#BFDBFE', strokeWeight: 1 });
  addText(apiPC, { x: CONTENT_X + 16, y: apicy + 14, text: 'API 키는 생성 시 단 한 번만 전체 값을 확인할 수 있습니다. 안전한 곳에 복사해두세요.', size: 13, style: 'Regular', color: '#1D4ED8' });

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 4: API Keys - Mobile (375 x 812)
  // ════════════════════════════════════════════════════════════════════════
  const apiMobile = makeFrame('API Keys - Mobile (375px)', 375, 812, 4620, 0, '#F8FAFC');

  // Top bar
  addRect(apiMobile, { name: 'topbar', x: 0, y: 0, w: 375, h: 56, fill: '#FFFFFF', strokeColor: '#E2E8F0', strokeWeight: 1 });
  addRect(apiMobile, { name: 'hamburger', x: 16, y: 16, w: 24, h: 24, fill: '#64748B', radius: 2 });
  addText(apiMobile, { x: 148, y: 18, text: 'API Keys', size: 16, style: 'Bold', color: '#0F172A' });
  addRect(apiMobile, { name: 'btn-plus', x: 329, y: 14, w: 30, h: 30, fill: '#2563EB', radius: 6 });
  addText(apiMobile, { x: 337, y: 20, text: '+', size: 16, style: 'Bold', color: '#FFFFFF' });

  // Card list
  const mApiCards = [
    { alias: '프로덕션 키', prefix: 'wc_a1b2c3d4', status: 'Active', statusVar: 'success', lastUsed: '2분 전', hasAction: true },
    { alias: '테스트 키', prefix: 'wc_e5f6g7h8', status: 'Active', statusVar: 'success', lastUsed: '1시간 전', hasAction: true },
    { alias: '레거시 키', prefix: 'wc_i9j0k1l2', status: 'Revoked', statusVar: 'error', lastUsed: '-', hasAction: false },
  ];

  let mApiY = 72;
  for (const card of mApiCards) {
    addRect(apiMobile, { name: `api-card-${card.alias}`, x: 16, y: mApiY, w: 343, h: 100, fill: '#FFFFFF', radius: 12, strokeColor: '#E2E8F0', strokeWeight: 1 });
    addText(apiMobile, { x: 32, y: mApiY + 16, text: card.alias, size: 15, style: 'Semi Bold', color: '#0F172A' });
    addBadge(apiMobile, 200, mApiY + 16, card.status, card.statusVar);
    addText(apiMobile, { x: 32, y: mApiY + 42, text: card.prefix, size: 12, style: 'Regular', color: '#64748B' });
    addText(apiMobile, { x: 32, y: mApiY + 66, text: `마지막 사용: ${card.lastUsed}`, size: 11, style: 'Regular', color: '#94A3B8' });
    if (card.hasAction) {
      addRect(apiMobile, { name: 'btn-deactivate', x: 245, y: mApiY + 60, w: 90, h: 28, fill: '#FEF2F2', radius: 6 });
      addText(apiMobile, { x: 261, y: mApiY + 67, text: '비활성화', size: 11, style: 'Medium', color: '#DC2626' });
    }
    mApiY += 116;
  }

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 5: API Key 발급 모달 - PC (1440 x 900)
  // Shows the API Keys PC frame with an overlay + dialog
  // ════════════════════════════════════════════════════════════════════════
  const modalPC = makeFrame('API Key 발급 모달 - PC', 1440, 900, 6160, 0, '#F8FAFC');
  buildSidebar(modalPC, 900, 'API Keys');

  // Dim overlay
  addRect(modalPC, { name: 'overlay', x: 0, y: 0, w: 1440, h: 900, fill: '#0F172A', radius: 0 });
  // Reduce opacity visually by making overlay slightly transparent-looking with a lighter fill
  const overlayRect = modalPC.children[modalPC.children.length - 1];
  overlayRect.opacity = 0.5;

  // Dialog box (480 x 360 centered)
  const DIALOG_W = 480;
  const DIALOG_H = 360;
  const DIALOG_X = (1440 - DIALOG_W) / 2;
  const DIALOG_Y = (900 - DIALOG_H) / 2;

  addRect(modalPC, { name: 'dialog', x: DIALOG_X, y: DIALOG_Y, w: DIALOG_W, h: DIALOG_H, fill: '#FFFFFF', radius: 16 });

  // Dialog header icon
  addRect(modalPC, { name: 'icon-bg', x: DIALOG_X + 32, y: DIALOG_Y + 32, w: 48, h: 48, fill: '#EFF6FF', radius: 12 });
  addText(modalPC, { x: DIALOG_X + 46, y: DIALOG_Y + 44, text: 'KEY', size: 11, style: 'Bold', color: '#2563EB' });

  addText(modalPC, { x: DIALOG_X + 32, y: DIALOG_Y + 92, text: '새 API Key 발급', size: 20, style: 'Bold', color: '#0F172A' });
  addText(modalPC, { x: DIALOG_X + 32, y: DIALOG_Y + 120, text: '이 키로 Wisecan API에 접근할 수 있습니다.', size: 13, style: 'Regular', color: '#64748B' });

  // Form field
  addText(modalPC, { x: DIALOG_X + 32, y: DIALOG_Y + 156, text: 'API Key 별칭 *', size: 13, style: 'Medium', color: '#0F172A' });
  addRect(modalPC, { name: 'input-alias', x: DIALOG_X + 32, y: DIALOG_Y + 176, w: DIALOG_W - 64, h: 40, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(modalPC, { x: DIALOG_X + 44, y: DIALOG_Y + 186, text: '예) 프로덕션 서버 키', size: 13, style: 'Regular', color: '#94A3B8' });
  addText(modalPC, { x: DIALOG_X + 32, y: DIALOG_Y + 222, text: '키를 구분하기 위한 이름입니다. 나중에 변경 가능합니다.', size: 11, style: 'Regular', color: '#94A3B8' });

  // Footer buttons
  addRect(modalPC, { name: 'btn-cancel', x: DIALOG_X + 32, y: DIALOG_Y + 292, w: 100, h: 40, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(modalPC, { x: DIALOG_X + 55, y: DIALOG_Y + 305, text: '취소', size: 14, style: 'Medium', color: '#64748B' });

  addRect(modalPC, { name: 'btn-submit', x: DIALOG_X + 316, y: DIALOG_Y + 292, w: 132, h: 40, fill: '#2563EB', radius: 8 });
  addText(modalPC, { x: DIALOG_X + 350, y: DIALOG_Y + 305, text: '발급', size: 14, style: 'Semi Bold', color: '#FFFFFF' });

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 6: API Key 발급 모달 - Mobile (375 x 812) — Bottom Sheet
  // ════════════════════════════════════════════════════════════════════════
  const modalMobile = makeFrame('API Key 발급 모달 - Mobile', 375, 812, 7700, 0, '#F8FAFC');

  // Background (API Keys Mobile content, simplified)
  addRect(modalMobile, { name: 'bg-cards', x: 0, y: 56, w: 375, h: 756, fill: '#F8FAFC' });

  // Dim overlay
  const mOverlay = addRect(modalMobile, { name: 'overlay', x: 0, y: 0, w: 375, h: 812, fill: '#0F172A', radius: 0 });
  mOverlay.opacity = 0.5;

  // Bottom sheet
  const SHEET_H = 380;
  const SHEET_Y = 812 - SHEET_H;
  addRect(modalMobile, { name: 'bottom-sheet', x: 0, y: SHEET_Y, w: 375, h: SHEET_H, fill: '#FFFFFF', radius: 16 });

  // Handle bar
  addRect(modalMobile, { name: 'handle-bar', x: (375 - 40) / 2, y: SHEET_Y + 12, w: 40, h: 4, fill: '#E2E8F0', radius: 2 });

  // Sheet content
  addText(modalMobile, { x: 24, y: SHEET_Y + 32, text: '새 API Key 발급', size: 18, style: 'Bold', color: '#0F172A' });
  addText(modalMobile, { x: 24, y: SHEET_Y + 60, text: '이 키로 Wisecan API에 접근할 수 있습니다.', size: 13, style: 'Regular', color: '#64748B' });

  addText(modalMobile, { x: 24, y: SHEET_Y + 96, text: 'API Key 별칭 *', size: 13, style: 'Medium', color: '#0F172A' });
  addRect(modalMobile, { name: 'input-alias', x: 24, y: SHEET_Y + 116, w: 327, h: 44, fill: '#FFFFFF', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(modalMobile, { x: 36, y: SHEET_Y + 128, text: '예) 프로덕션 서버 키', size: 13, style: 'Regular', color: '#94A3B8' });
  addText(modalMobile, { x: 24, y: SHEET_Y + 168, text: '키를 구분하기 위한 이름입니다.', size: 11, style: 'Regular', color: '#94A3B8' });

  // Buttons stacked
  addRect(modalMobile, { name: 'btn-submit', x: 24, y: SHEET_Y + 200, w: 327, h: 48, fill: '#2563EB', radius: 8 });
  addText(modalMobile, { x: 170, y: SHEET_Y + 218, text: '발급', size: 15, style: 'Semi Bold', color: '#FFFFFF' });

  addRect(modalMobile, { name: 'btn-cancel', x: 24, y: SHEET_Y + 260, w: 327, h: 48, fill: '#F8FAFC', radius: 8, strokeColor: '#E2E8F0', strokeWeight: 1 });
  addText(modalMobile, { x: 166, y: SHEET_Y + 278, text: '취소', size: 15, style: 'Medium', color: '#64748B' });

  // ── Done ──────────────────────────────────────────────────────────────────
  figma.viewport.scrollAndZoomIntoView([dbPC]);
  figma.closePlugin('Dashboard + API Key Wireframe 6개 프레임 생성 완료');
})();

(function () {
  // ── Helpers ───────────────────────────────────────────────────────────────
  function hexToRgb(hex) {
    var r = parseInt(hex.slice(1, 3), 16) / 255;
    var g = parseInt(hex.slice(3, 5), 16) / 255;
    var b = parseInt(hex.slice(5, 7), 16) / 255;
    return { r: r, g: g, b: b };
  }

  function solid(hex) {
    return [{ type: 'SOLID', color: hexToRgb(hex) }];
  }

  function addText(parent, opts) {
    var t = figma.createText();
    t.x = opts.x;
    t.y = opts.y;
    t.fontName = { family: 'Inter', style: opts.style || 'Regular' };
    t.fontSize = opts.size || 14;
    t.characters = opts.text;
    t.fills = solid(opts.color || '#0F172A');
    if (opts.w) {
      t.textAutoResize = 'HEIGHT';
      t.resize(opts.w, t.height);
    }
    if (opts.align) {
      t.textAlignHorizontal = opts.align;
    }
    parent.appendChild(t);
    return t;
  }

  function addRect(parent, opts) {
    var r = figma.createRectangle();
    r.name = opts.name || 'rect';
    r.x = opts.x;
    r.y = opts.y;
    r.resize(opts.w, opts.h);
    r.fills = opts.fill ? solid(opts.fill) : [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    if (opts.radius !== undefined) { r.cornerRadius = opts.radius; }
    if (opts.strokeColor) {
      r.strokes = [{ type: 'SOLID', color: hexToRgb(opts.strokeColor) }];
      r.strokeWeight = opts.strokeWeight || 1;
    }
    parent.appendChild(r);
    return r;
  }

  function makeFrame(name, w, h, x, y, bg) {
    var f = figma.createFrame();
    f.name = name;
    f.resize(w, h);
    f.x = x;
    f.y = y;
    f.fills = solid(bg || '#F8FAFC');
    figma.currentPage.appendChild(f);
    return f;
  }

  // ── Constants ─────────────────────────────────────────────────────────────
  var SIDEBAR_W = 240;
  var NAV_ITEMS = [
    { label: '대시보드', id: 'dashboard' },
    { label: 'API Keys', id: 'api-keys' },
    { label: '메시지 도구', id: 'message-tools' },
    { label: '메시지 통계', id: 'message-stats' },
    { label: '파일 변환', id: 'file-convert' },
    { label: '설정', id: 'settings' }
  ];

  var C = {
    primary: '#2563EB',
    primaryLight: '#EFF6FF',
    success: '#22C55E',
    successLight: '#F0FDF4',
    error: '#DC2626',
    errorLight: '#FEF2F2',
    warning: '#F59E0B',
    warningLight: '#FFFBEB',
    surface: '#FFFFFF',
    bg: '#F8FAFC',
    border: '#E2E8F0',
    textPrimary: '#0F172A',
    textSecondary: '#64748B',
    textMuted: '#94A3B8',
    blue: '#3B82F6',
    purple: '#7C3AED',
    green: '#22C55E',
    yellow: '#F59E0B'
  };

  // ── Sidebar ───────────────────────────────────────────────────────────────
  function buildSidebar(frame, h, activeId) {
    addRect(frame, { name: 'sidebar-bg', x: 0, y: 0, w: SIDEBAR_W, h: h, fill: C.surface, strokeColor: C.border });
    addText(frame, { x: 20, y: 22, text: 'Wisecan', size: 18, style: 'Bold', color: C.textPrimary });

    var navY = 72;
    for (var i = 0; i < NAV_ITEMS.length; i++) {
      var item = NAV_ITEMS[i];
      var isActive = item.id === activeId;
      if (isActive) {
        addRect(frame, { name: 'nav-active-bg', x: 8, y: navY - 2, w: SIDEBAR_W - 16, h: 36, fill: C.primaryLight, radius: 8 });
      }
      addText(frame, {
        x: 24, y: navY + 8,
        text: item.label, size: 14,
        style: isActive ? 'Semi Bold' : 'Regular',
        color: isActive ? C.primary : C.textSecondary
      });
      navY += 44;
    }

    // User profile
    var profileY = h - 64;
    addRect(frame, { name: 'profile-bg', x: 8, y: profileY, w: SIDEBAR_W - 16, h: 48, fill: C.bg, radius: 8 });
    addRect(frame, { name: 'avatar', x: 16, y: profileY + 6, w: 36, h: 36, fill: C.primaryLight, radius: 18 });
    addText(frame, { x: 16, y: profileY + 14, text: 'KW', size: 12, style: 'Semi Bold', color: C.primary, w: 36, align: 'CENTER' });
    addText(frame, { x: 60, y: profileY + 10, text: '김위즈캔', size: 13, style: 'Medium', color: C.textPrimary });
    addText(frame, { x: 60, y: profileY + 28, text: 'kim@wisecan.co.kr', size: 11, color: C.textSecondary });
  }

  // ── Mobile Topbar ─────────────────────────────────────────────────────────
  function buildMobileTopbar(frame, title) {
    addRect(frame, { name: 'topbar-bg', x: 0, y: 0, w: frame.width, h: 56, fill: C.surface, strokeColor: C.border });
    addRect(frame, { name: 'menu-btn', x: 16, y: 14, w: 28, h: 28, fill: C.bg, radius: 6 });
    addText(frame, { x: 0, y: 18, text: title, size: 16, style: 'Semi Bold', color: C.textPrimary, w: frame.width, align: 'CENTER' });
  }

  // ── Tab Bar ───────────────────────────────────────────────────────────────
  function buildTabBar(frame, tabs, yOffset) {
    addRect(frame, { name: 'tab-bar-bg', x: SIDEBAR_W, y: yOffset, w: frame.width - SIDEBAR_W, h: 44, fill: C.surface, strokeColor: C.border });
    var tabW = 140;
    var tabX = SIDEBAR_W + 32;
    for (var i = 0; i < tabs.length; i++) {
      var tab = tabs[i];
      if (tab.active) {
        addRect(frame, { name: 'tab-active-indicator', x: tabX, y: yOffset + 40, w: tabW, h: 2, fill: C.primary });
      }
      addText(frame, {
        x: tabX, y: yOffset + 12,
        text: tab.label, size: 14,
        style: tab.active ? 'Semi Bold' : 'Regular',
        color: tab.active ? C.primary : C.textSecondary,
        w: tabW, align: 'CENTER'
      });
      tabX += tabW + 8;
    }
  }

  // ── Form Field ────────────────────────────────────────────────────────────
  function buildFormField(frame, opts) {
    var x = opts.x, y = opts.y, w = opts.w;
    addText(frame, { x: x, y: y, text: opts.label, size: 12, style: 'Medium', color: C.textPrimary });
    var inputH = opts.type === 'textarea' ? 96 : 40;
    addRect(frame, { name: 'input-bg', x: x, y: y + 20, w: w, h: inputH, fill: C.surface, radius: 8, strokeColor: C.border });
    addText(frame, { x: x + 12, y: y + 20 + (inputH / 2) - 8, text: opts.placeholder, size: 13, color: C.textMuted, w: w - 24 });
    if (opts.hint) {
      addText(frame, { x: x, y: y + 20 + inputH + 4, text: opts.hint, size: 11, color: C.textMuted, w: w });
    }
  }

  // ── Badge ─────────────────────────────────────────────────────────────────
  function buildBadge(frame, x, y, label, color, bgColor) {
    addRect(frame, { name: 'badge-bg', x: x, y: y, w: 52, h: 22, fill: bgColor, radius: 11 });
    addText(frame, { x: x, y: y + 4, text: label, size: 11, style: 'Medium', color: color, w: 52, align: 'CENTER' });
  }

  // ── Button ────────────────────────────────────────────────────────────────
  function buildButton(frame, x, y, label, primary) {
    var btnW = 100;
    var fill = primary ? C.primary : C.bg;
    var textColor = primary ? C.surface : C.textSecondary;
    addRect(frame, { name: 'btn-' + label, x: x, y: y, w: btnW, h: 40, fill: fill, radius: 8, strokeColor: primary ? '' : C.border, strokeWeight: primary ? 0 : 1 });
    addText(frame, { x: x, y: y + 11, text: label, size: 14, style: 'Medium', color: textColor, w: btnW, align: 'CENTER' });
  }

  // ── Page Header ───────────────────────────────────────────────────────────
  function buildPageHeader(frame, x, y, w, title, subtitle) {
    addText(frame, { x: x, y: y, text: title, size: 24, style: 'Bold', color: C.textPrimary, w: w });
    if (subtitle) {
      addText(frame, { x: x, y: y + 34, text: subtitle, size: 14, color: C.textSecondary, w: w });
    }
  }

  // ── Error Banner ──────────────────────────────────────────────────────────
  function buildErrorBanner(frame, x, y, w, message) {
    addRect(frame, { name: 'error-banner', x: x, y: y, w: w, h: 52, fill: C.errorLight, radius: 8, strokeColor: '#FECACA' });
    addRect(frame, { name: 'error-left-bar', x: x, y: y, w: 4, h: 52, fill: C.error, radius: 2 });
    addText(frame, { x: x + 16, y: y + 8, text: '! ' + message, size: 13, color: C.error, w: w - 24 });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Font preload (required before any text character assignment)
  // ─────────────────────────────────────────────────────────────────────────
  Promise.all([
    figma.loadFontAsync({ family: 'Inter', style: 'Regular' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Medium' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Bold' })
  ]).then(function () {

  // ─────────────────────────────────────────────────────────────────────────
  // FRAME 1: Message Send - PC (1440 x 900)
  // ─────────────────────────────────────────────────────────────────────────
  var sendPc = makeFrame('Message Send - PC (1440px)', 1440, 900, 0, 0, C.bg);

  buildSidebar(sendPc, 900, 'message-tools');

  var sendContentX = SIDEBAR_W;
  var sendContentW = 1440 - SIDEBAR_W;

  // Tabs
  buildTabBar(sendPc, [
    { label: '메시지 발송', active: true },
    { label: '발송 이력', active: false }
  ], 0);

  // Page header
  var sendPadX = SIDEBAR_W + 32;
  buildPageHeader(sendPc, sendPadX, 60, sendContentW - 64, '메시지 발송', 'MCP 채널을 통해 메시지를 발송하세요');

  // ── Send Form Card (left) ──────────────────────────────────────────────
  var formCardX = sendPadX;
  var formCardY = 128;
  var formCardW = sendContentW - 64 - 376;
  var formCardH = 720;
  addRect(sendPc, { name: 'form-card', x: formCardX, y: formCardY, w: formCardW, h: formCardH, fill: C.surface, radius: 12, strokeColor: C.border });
  addText(sendPc, { x: formCardX + 24, y: formCardY + 24, text: '메시지 발송', size: 16, style: 'Semi Bold', color: C.textPrimary });

  var fieldX = formCardX + 24;
  var fieldW = formCardW - 48;

  buildFormField(sendPc, { x: fieldX, y: formCardY + 64, w: fieldW, label: '수신자 *', placeholder: '예) user@example.com 또는 @slack-user', hint: '이메일, Slack 핸들, 전화번호 등 채널별 수신자 식별자' });
  buildFormField(sendPc, { x: fieldX, y: formCardY + 160, w: fieldW, label: '채널 *', placeholder: '발송 채널을 선택하세요 (이메일 / Slack / SMS / 카카오톡)' });
  buildFormField(sendPc, { x: fieldX, y: formCardY + 244, w: fieldW, label: '제목 (이메일 채널만)', placeholder: '예) [Wisecan] 알림 메시지' });
  buildFormField(sendPc, { x: fieldX, y: formCardY + 328, w: fieldW, label: '본문 *', placeholder: '발송할 메시지 내용을 입력하세요. 최대 2,000자까지 입력 가능합니다.', hint: '남은 글자수: 2,000 / 2,000', type: 'textarea' });

  // File upload area
  addText(sendPc, { x: fieldX, y: formCardY + 476, text: '첨부 파일', size: 12, style: 'Medium', color: C.textPrimary });
  addRect(sendPc, { name: 'file-upload', x: fieldX, y: formCardY + 496, w: fieldW, h: 80, fill: C.bg, radius: 8, strokeColor: C.border });
  addText(sendPc, { x: fieldX, y: formCardY + 528, text: '파일을 드래그하거나 클릭하여 업로드 (PDF, PNG, JPG, DOCX / 최대 10MB)', size: 12, color: C.textMuted, w: fieldW, align: 'CENTER' });

  // Actions
  buildButton(sendPc, fieldX, formCardY + 600, '발송', true);
  buildButton(sendPc, fieldX + 116, formCardY + 600, '초기화', false);

  // Validation error state (shown below actions)
  buildErrorBanner(sendPc, fieldX, formCardY + 660, fieldW, '수신자 이메일 형식이 올바르지 않습니다. 다시 확인해주세요.');

  // ── Preview Card (right) ─────────────────────────────────────────────────
  var previewCardX = formCardX + formCardW + 24;
  var previewCardW = 360;
  addRect(sendPc, { name: 'preview-card', x: previewCardX, y: formCardY, w: previewCardW, h: formCardH, fill: C.surface, radius: 12, strokeColor: C.border });
  addText(sendPc, { x: previewCardX + 24, y: formCardY + 24, text: '미리보기', size: 16, style: 'Semi Bold', color: C.textPrimary });

  // Empty state
  addRect(sendPc, { name: 'preview-empty', x: previewCardX + 16, y: formCardY + 64, w: previewCardW - 32, h: 160, fill: C.bg, radius: 8 });
  addText(sendPc, { x: previewCardX + 16, y: formCardY + 100, text: '내용을 입력하면\n미리보기가 표시됩니다', size: 14, color: C.textMuted, w: previewCardW - 32, align: 'CENTER' });
  addText(sendPc, { x: previewCardX + 16, y: formCardY + 148, text: '채널, 수신자, 본문을 입력해보세요', size: 12, color: C.textMuted, w: previewCardW - 32, align: 'CENTER' });

  // Preview filled state (示)
  addText(sendPc, { x: previewCardX + 24, y: formCardY + 248, text: '채널', size: 11, style: 'Medium', color: C.textSecondary });
  buildBadge(sendPc, previewCardX + 24, formCardY + 264, '이메일', C.primary, C.primaryLight);
  addText(sendPc, { x: previewCardX + 24, y: formCardY + 308, text: '수신자', size: 11, style: 'Medium', color: C.textSecondary });
  addText(sendPc, { x: previewCardX + 24, y: formCardY + 324, text: 'kim@example.com', size: 13, color: C.textPrimary });
  addText(sendPc, { x: previewCardX + 24, y: formCardY + 360, text: '본문', size: 11, style: 'Medium', color: C.textSecondary });
  addText(sendPc, { x: previewCardX + 24, y: formCardY + 376, text: '안녕하세요. Wisecan에서 보내드리는 알림입니다.', size: 13, color: C.textPrimary, w: previewCardW - 48 });

  // ─────────────────────────────────────────────────────────────────────────
  // FRAME 2: Message Send - Mobile (375 x 812)
  // ─────────────────────────────────────────────────────────────────────────
  var sendMob = makeFrame('Message Send - Mobile (375px)', 375, 812, 1480, 0, C.bg);

  buildMobileTopbar(sendMob, '메시지 도구');

  // Mobile tab bar
  addRect(sendMob, { name: 'mobile-tab-bar', x: 0, y: 56, w: 375, h: 44, fill: C.surface, strokeColor: C.border });
  var mobTabs = ['발송', '미리보기', '이력'];
  var mobTabW = 125;
  for (var ti = 0; ti < mobTabs.length; ti++) {
    var isActive = ti === 0;
    addText(sendMob, {
      x: ti * mobTabW, y: 68,
      text: mobTabs[ti], size: 13,
      style: isActive ? 'Semi Bold' : 'Regular',
      color: isActive ? C.primary : C.textSecondary,
      w: mobTabW, align: 'CENTER'
    });
    if (isActive) {
      addRect(sendMob, { name: 'mob-tab-indicator', x: ti * mobTabW + 16, y: 96, w: mobTabW - 32, h: 2, fill: C.primary });
    }
  }

  // Mobile form
  var mobFieldX = 16;
  var mobFieldW = 375 - 32;
  buildFormField(sendMob, { x: mobFieldX, y: 116, w: mobFieldW, label: '수신자 *', placeholder: '예) user@example.com' });
  buildFormField(sendMob, { x: mobFieldX, y: 200, w: mobFieldW, label: '채널 *', placeholder: '발송 채널 선택' });
  buildFormField(sendMob, { x: mobFieldX, y: 284, w: mobFieldW, label: '본문 *', placeholder: '메시지 내용을 입력하세요', type: 'textarea' });

  // Mobile file upload
  addText(sendMob, { x: mobFieldX, y: 432, text: '첨부 파일', size: 12, style: 'Medium', color: C.textPrimary });
  addRect(sendMob, { name: 'mob-file-upload', x: mobFieldX, y: 452, w: mobFieldW, h: 60, fill: C.bg, radius: 8, strokeColor: C.border });
  addText(sendMob, { x: mobFieldX, y: 475, text: '파일 선택 (PDF, PNG, JPG, DOCX)', size: 12, color: C.textMuted, w: mobFieldW, align: 'CENTER' });

  // Mobile send button
  addRect(sendMob, { name: 'mob-send-btn', x: mobFieldX, y: 536, w: mobFieldW, h: 48, fill: C.primary, radius: 8 });
  addText(sendMob, { x: mobFieldX, y: 552, text: '발송', size: 15, style: 'Semi Bold', color: C.surface, w: mobFieldW, align: 'CENTER' });

  // ─────────────────────────────────────────────────────────────────────────
  // FRAME 3: Message Result - PC (1440 x 900)
  // ─────────────────────────────────────────────────────────────────────────
  var resultPc = makeFrame('Message Result - PC (1440px)', 1440, 900, 0, 940, C.bg);

  buildSidebar(resultPc, 900, 'message-tools');

  var resPadX = SIDEBAR_W + 32;
  var resContentW = 1440 - SIDEBAR_W - 64;

  // Back button
  addRect(resultPc, { name: 'back-btn', x: resPadX, y: 24, w: 140, h: 36, fill: C.bg, radius: 8, strokeColor: C.border });
  addText(resultPc, { x: resPadX, y: 32, text: '← 이력으로 돌아가기', size: 13, color: C.textSecondary, w: 140, align: 'CENTER' });

  buildPageHeader(resultPc, resPadX, 72, resContentW, '발송 결과 상세', '메시지 ID 기반 상태 및 응답 상세 정보');

  // Status summary card (4-column)
  var statCardY = 144;
  addRect(resultPc, { name: 'status-card', x: resPadX, y: statCardY, w: resContentW, h: 96, fill: C.surface, radius: 12, strokeColor: C.border });

  var statColW = resContentW / 4;
  var statItems = [
    { label: '메시지 ID', value: 'msg_a1b2c3d4e5f6' },
    { label: '발송 상태', value: '성공' },
    { label: '응답시간', value: '234ms' },
    { label: '발송 시각', value: '2026-04-10 14:23:05' }
  ];
  for (var si = 0; si < statItems.length; si++) {
    var item = statItems[si];
    var colX = resPadX + si * statColW + 24;
    if (si > 0) {
      addRect(resultPc, { name: 'stat-divider-' + si, x: resPadX + si * statColW, y: statCardY + 16, w: 1, h: 64, fill: C.border });
    }
    addText(resultPc, { x: colX, y: statCardY + 18, text: item.label, size: 11, style: 'Medium', color: C.textSecondary });
    if (si === 1) {
      buildBadge(resultPc, colX, statCardY + 40, item.value, C.success, C.successLight);
    } else {
      addText(resultPc, { x: colX, y: statCardY + 40, text: item.value, size: 16, style: 'Bold', color: C.textPrimary });
    }
  }

  // Detail info card
  var detailCardY = statCardY + 112;
  addRect(resultPc, { name: 'detail-card', x: resPadX, y: detailCardY, w: resContentW, h: 260, fill: C.surface, radius: 12, strokeColor: C.border });
  addText(resultPc, { x: resPadX + 24, y: detailCardY + 20, text: '발송 정보', size: 15, style: 'Semi Bold', color: C.textPrimary });

  var detailRows = [
    { label: '채널', value: '이메일' },
    { label: '수신자', value: 'kim@example.com' },
    { label: '제목', value: '[Wisecan] 알림 메시지' },
    { label: '본문', value: '안녕하세요. Wisecan에서 보내드리는 알림입니다.' },
    { label: 'API Key', value: 'wc_a1b2c3d4 (프로덕션 키)' }
  ];
  for (var di = 0; di < detailRows.length; di++) {
    var row = detailRows[di];
    var rowY = detailCardY + 56 + di * 36;
    addText(resultPc, { x: resPadX + 24, y: rowY, text: row.label, size: 12, style: 'Medium', color: C.textSecondary, w: 120 });
    if (di === 0) {
      buildBadge(resultPc, resPadX + 160, rowY - 2, row.value, C.primary, C.primaryLight);
    } else {
      addText(resultPc, { x: resPadX + 160, y: rowY, text: row.value, size: 13, color: C.textPrimary });
    }
  }

  // Error detail card (failure state)
  var errCardY = detailCardY + 276;
  var errCardH = 196;
  addRect(resultPc, { name: 'error-card', x: resPadX, y: errCardY, w: resContentW, h: errCardH, fill: C.errorLight, radius: 12, strokeColor: '#FECACA' });
  addRect(resultPc, { name: 'error-left-accent', x: resPadX, y: errCardY, w: 4, h: errCardH, fill: C.error, radius: 2 });
  addText(resultPc, { x: resPadX + 24, y: errCardY + 20, text: '! 에러 정보 (실패 시 표시)', size: 14, style: 'Semi Bold', color: C.error });
  addText(resultPc, { x: resPadX + 24, y: errCardY + 50, text: '에러 코드', size: 11, style: 'Medium', color: C.error });
  addText(resultPc, { x: resPadX + 120, y: errCardY + 50, text: 'ERR_CHANNEL_TIMEOUT', size: 12, style: 'Medium', color: C.textPrimary });
  addText(resultPc, { x: resPadX + 24, y: errCardY + 78, text: '에러 메시지', size: 11, style: 'Medium', color: C.error });
  addText(resultPc, { x: resPadX + 120, y: errCardY + 78, text: '채널 연결 시간이 초과되었습니다. 채널 설정을 확인하거나 잠시 후 재시도하세요.', size: 12, color: C.textPrimary, w: resContentW - 160 });
  buildButton(resultPc, resPadX + 24, errCardY + 140, '재발송', true);
  buildButton(resultPc, resPadX + 140, errCardY + 140, '지원 문의', false);

  // ─────────────────────────────────────────────────────────────────────────
  // FRAME 4: Message Result - Mobile (375 x 812)
  // ─────────────────────────────────────────────────────────────────────────
  var resultMob = makeFrame('Message Result - Mobile (375px)', 375, 812, 1480, 940, C.bg);

  buildMobileTopbar(resultMob, '발송 결과 상세');

  // Back button glyph centered inside the topbar menu-btn rect (x:16 y:14 w:28 h:28)
  addText(resultMob, { x: 16, y: 20, text: '←', size: 16, style: 'Medium', color: C.textPrimary, w: 28, align: 'CENTER' });

  // Status card 2x2
  addRect(resultMob, { name: 'mob-status-card', x: 16, y: 72, w: 343, h: 100, fill: C.surface, radius: 12, strokeColor: C.border });
  var mobStatItems = ['메시지 ID', '발송 상태', '응답시간', '발송 시각'];
  var mobStatVals = ['msg_a1b2c3d4', '성공', '234ms', '04-10 14:23'];
  for (var msi = 0; msi < 4; msi++) {
    var msiCol = msi % 2;
    var msiRow = Math.floor(msi / 2);
    var msiX = 16 + 24 + msiCol * 170;
    var msiY = 72 + 14 + msiRow * 44;
    addText(resultMob, { x: msiX, y: msiY, text: mobStatItems[msi], size: 10, color: C.textSecondary });
    if (msi === 1) {
      buildBadge(resultMob, msiX, msiY + 14, mobStatVals[msi], C.success, C.successLight);
    } else {
      addText(resultMob, { x: msiX, y: msiY + 14, text: mobStatVals[msi], size: 13, style: 'Bold', color: C.textPrimary });
    }
  }

  // Detail card
  addRect(resultMob, { name: 'mob-detail-card', x: 16, y: 188, w: 343, h: 220, fill: C.surface, radius: 12, strokeColor: C.border });
  addText(resultMob, { x: 40, y: 204, text: '발송 정보', size: 14, style: 'Semi Bold', color: C.textPrimary });
  var mobDetail = [
    { label: '채널', value: '이메일' },
    { label: '수신자', value: 'kim@example.com' },
    { label: '제목', value: '[Wisecan] 알림 메시지' },
    { label: '본문', value: '안녕하세요. Wisecan에서 보내드리는 알림입니다.' }
  ];
  for (var mdi = 0; mdi < mobDetail.length; mdi++) {
    var mRow = mobDetail[mdi];
    var mRowY = 232 + mdi * 40;
    addText(resultMob, { x: 40, y: mRowY, text: mRow.label, size: 11, style: 'Medium', color: C.textSecondary, w: 80 });
    addText(resultMob, { x: 120, y: mRowY, text: mRow.value, size: 12, color: C.textPrimary, w: 220 });
  }

  // Mobile error card
  var mobErrY = 424;
  var mobErrH = 216;
  addRect(resultMob, { name: 'mob-error-card', x: 16, y: mobErrY, w: 343, h: mobErrH, fill: C.errorLight, radius: 12, strokeColor: '#FECACA' });
  addRect(resultMob, { name: 'mob-error-bar', x: 16, y: mobErrY, w: 4, h: mobErrH, fill: C.error, radius: 2 });
  addText(resultMob, { x: 36, y: mobErrY + 16, text: '! 에러 정보 (실패 시 표시)', size: 12, style: 'Semi Bold', color: C.error });
  addText(resultMob, { x: 36, y: mobErrY + 44, text: '에러 코드', size: 10, style: 'Medium', color: C.error });
  addText(resultMob, { x: 36, y: mobErrY + 60, text: 'ERR_CHANNEL_TIMEOUT', size: 12, style: 'Medium', color: C.textPrimary });
  addText(resultMob, { x: 36, y: mobErrY + 88, text: '에러 메시지', size: 10, style: 'Medium', color: C.error });
  addText(resultMob, { x: 36, y: mobErrY + 104, text: '채널 연결 시간이 초과되었습니다. 채널 설정을 확인하거나 잠시 후 다시 시도하세요.', size: 12, color: C.textPrimary, w: 311 });
  addRect(resultMob, { name: 'mob-retry-btn', x: 36, y: mobErrY + 164, w: 140, h: 40, fill: C.error, radius: 8 });
  addText(resultMob, { x: 36, y: mobErrY + 176, text: '재발송', size: 13, style: 'Medium', color: C.surface, w: 140, align: 'CENTER' });
  addRect(resultMob, { name: 'mob-support-btn', x: 188, y: mobErrY + 164, w: 155, h: 40, fill: C.errorLight, radius: 8, strokeColor: C.error });
  addText(resultMob, { x: 188, y: mobErrY + 176, text: '지원 문의', size: 13, style: 'Medium', color: C.error, w: 155, align: 'CENTER' });

  // ─────────────────────────────────────────────────────────────────────────
  // FRAME 5: Message Search - PC (1440 x 900)
  // ─────────────────────────────────────────────────────────────────────────
  var searchPc = makeFrame('Message Search - PC (1440px)', 1440, 900, 0, 1880, C.bg);

  buildSidebar(searchPc, 900, 'message-tools');

  var srchPadX = SIDEBAR_W + 32;
  var srchContentW = 1440 - SIDEBAR_W - 64;

  buildTabBar(searchPc, [
    { label: '메시지 발송', active: false },
    { label: '발송 이력', active: true }
  ], 0);

  buildPageHeader(searchPc, srchPadX, 60, srchContentW, '발송 이력', '채널, 상태, 기간 조건으로 발송 이력을 검색하세요');

  // Filter bar
  var filterBarY = 128;
  addRect(searchPc, { name: 'filter-bar', x: srchPadX, y: filterBarY, w: srchContentW, h: 96, fill: C.surface, radius: 12, strokeColor: C.border });

  addText(searchPc, { x: srchPadX + 24, y: filterBarY + 10, text: '기간', size: 11, style: 'Medium', color: C.textSecondary });
  addRect(searchPc, { name: 'filter-date', x: srchPadX + 24, y: filterBarY + 26, w: 200, h: 32, fill: C.bg, radius: 6, strokeColor: C.border });
  addText(searchPc, { x: srchPadX + 36, y: filterBarY + 34, text: '최근 7일', size: 12, color: C.textPrimary });

  addText(searchPc, { x: srchPadX + 240, y: filterBarY + 10, text: '상태', size: 11, style: 'Medium', color: C.textSecondary });
  addRect(searchPc, { name: 'filter-status', x: srchPadX + 240, y: filterBarY + 26, w: 120, h: 32, fill: C.bg, radius: 6, strokeColor: C.border });
  addText(searchPc, { x: srchPadX + 252, y: filterBarY + 34, text: '전체', size: 12, color: C.textPrimary });

  addText(searchPc, { x: srchPadX + 376, y: filterBarY + 10, text: '채널', size: 11, style: 'Medium', color: C.textSecondary });
  addRect(searchPc, { name: 'filter-channel', x: srchPadX + 376, y: filterBarY + 26, w: 120, h: 32, fill: C.bg, radius: 6, strokeColor: C.border });
  addText(searchPc, { x: srchPadX + 388, y: filterBarY + 34, text: '전체', size: 12, color: C.textPrimary });

  addText(searchPc, { x: srchPadX + 512, y: filterBarY + 10, text: '수신자 검색', size: 11, style: 'Medium', color: C.textSecondary });
  addRect(searchPc, { name: 'filter-keyword', x: srchPadX + 512, y: filterBarY + 26, w: 180, h: 32, fill: C.bg, radius: 6, strokeColor: C.border });
  addText(searchPc, { x: srchPadX + 524, y: filterBarY + 34, text: '수신자 이메일 또는 ID', size: 12, color: C.textMuted });

  addRect(searchPc, { name: 'search-btn', x: srchPadX + 708, y: filterBarY + 26, w: 80, h: 32, fill: C.primary, radius: 6 });
  addText(searchPc, { x: srchPadX + 708, y: filterBarY + 34, text: '검색', size: 13, style: 'Medium', color: C.surface, w: 80, align: 'CENTER' });

  // Active filter badges
  addText(searchPc, { x: srchPadX + 24, y: filterBarY + 64, text: '적용된 필터:', size: 11, color: C.textMuted });
  addRect(searchPc, { name: 'active-filter-1', x: srchPadX + 100, y: filterBarY + 60, w: 100, h: 22, fill: C.primaryLight, radius: 11 });
  addText(searchPc, { x: srchPadX + 100, y: filterBarY + 64, text: '기간: 최근 7일 ×', size: 11, color: C.primary, w: 100, align: 'CENTER' });

  // Table
  var tableY = filterBarY + 116;
  var tableH = 900 - tableY - 16;
  addRect(searchPc, { name: 'table-card', x: srchPadX, y: tableY, w: srchContentW, h: tableH, fill: C.surface, radius: 12, strokeColor: C.border });

  // Table header row
  addText(searchPc, { x: srchPadX + 24, y: tableY + 20, text: '검색 결과', size: 14, style: 'Semi Bold', color: C.textPrimary });
  addText(searchPc, { x: srchPadX + 24, y: tableY + 40, text: '총 47건', size: 12, color: C.textSecondary });

  addRect(searchPc, { name: 'table-header-bg', x: srchPadX, y: tableY + 64, w: srchContentW, h: 44, fill: C.bg });
  var tblCols = ['메시지 ID', '채널', '수신자', '상태', '응답시간', '발송 시각', ''];
  var tblColW = [160, 100, 200, 100, 100, 160, 80];
  var colStartX = srchPadX + 24;
  for (var ci = 0; ci < tblCols.length; ci++) {
    addText(searchPc, { x: colStartX, y: tableY + 80, text: tblCols[ci], size: 12, style: 'Medium', color: C.textSecondary });
    colStartX += tblColW[ci];
  }

  // Table rows
  var tblRows = [
    { msgId: 'msg_a1b2c3d4', channel: '이메일', recipient: 'kim@example.com', status: '성공', statusOk: true, time: '234ms', sentAt: '04-10 14:23:05' },
    { msgId: 'msg_e5f6g7h8', channel: 'Slack', recipient: '@lee.devteam', status: '성공', statusOk: true, time: '89ms', sentAt: '04-10 13:45:22' },
    { msgId: 'msg_i9j0k1l2', channel: 'SMS', recipient: '010-1234-5678', status: '실패', statusOk: false, time: '1,203ms', sentAt: '04-10 12:11:44' },
    { msgId: 'msg_m3n4o5p6', channel: '카카오톡', recipient: 'park.wisecan', status: '성공', statusOk: true, time: '312ms', sentAt: '04-10 11:30:01' },
    { msgId: 'msg_q7r8s9t0', channel: '이메일', recipient: 'choi@company.io', status: '처리중', statusOk: null, time: '-', sentAt: '04-10 10:58:17' }
  ];
  for (var ri = 0; ri < tblRows.length; ri++) {
    var row = tblRows[ri];
    var rowY = tableY + 108 + ri * 52;
    addRect(searchPc, { name: 'row-divider-' + ri, x: srchPadX, y: rowY - 1, w: srchContentW, h: 1, fill: C.border });
    var cellX = srchPadX + 24;
    addText(searchPc, { x: cellX, y: rowY + 16, text: row.msgId, size: 12, color: C.textSecondary }); cellX += 160;
    buildBadge(searchPc, cellX, rowY + 14, row.channel, C.primary, C.primaryLight); cellX += 100;
    addText(searchPc, { x: cellX, y: rowY + 16, text: row.recipient, size: 13, color: C.textPrimary }); cellX += 200;
    var statusColor = row.statusOk === true ? C.success : (row.statusOk === false ? C.error : C.warning);
    var statusBg = row.statusOk === true ? C.successLight : (row.statusOk === false ? C.errorLight : C.warningLight);
    buildBadge(searchPc, cellX, rowY + 14, row.status, statusColor, statusBg); cellX += 100;
    addText(searchPc, { x: cellX, y: rowY + 16, text: row.time, size: 13, color: C.textPrimary }); cellX += 100;
    addText(searchPc, { x: cellX, y: rowY + 16, text: row.sentAt, size: 12, color: C.textSecondary }); cellX += 160;
    addRect(searchPc, { name: 'detail-btn-' + ri, x: cellX, y: rowY + 12, w: 52, h: 28, fill: C.bg, radius: 6, strokeColor: C.border });
    addText(searchPc, { x: cellX, y: rowY + 20, text: '상세', size: 12, color: C.textSecondary, w: 52, align: 'CENTER' });
  }

  // Pagination
  var paginY = tableY + tableH - 48;
  addRect(searchPc, { name: 'pagination-bar', x: srchPadX, y: paginY, w: srchContentW, h: 1, fill: C.border });
  addText(searchPc, { x: srchPadX + 24, y: paginY + 12, text: '총 47건 | 1 / 10 페이지', size: 12, color: C.textSecondary });
  var paginBtns = ['<', '1', '2', '3', '...', '10', '>'];
  var paginX = srchPadX + srchContentW - 260;
  for (var pi = 0; pi < paginBtns.length; pi++) {
    var isCurrentPage = paginBtns[pi] === '1';
    addRect(searchPc, { name: 'pag-' + pi, x: paginX + pi * 32, y: paginY + 8, w: 28, h: 28, fill: isCurrentPage ? C.primary : C.bg, radius: 6, strokeColor: isCurrentPage ? '' : C.border });
    addText(searchPc, { x: paginX + pi * 32, y: paginY + 16, text: paginBtns[pi], size: 12, style: isCurrentPage ? 'Medium' : 'Regular', color: isCurrentPage ? C.surface : C.textSecondary, w: 28, align: 'CENTER' });
  }

  // (Empty-state panel omitted from PC frame to avoid out-of-canvas overflow —
  //  see searchMob empty indicator + MessageSearchPanel.tsx for the real empty state.)

  // ─────────────────────────────────────────────────────────────────────────
  // FRAME 6: Message Search - Mobile (375 x 812)
  // ─────────────────────────────────────────────────────────────────────────
  var searchMob = makeFrame('Message Search - Mobile (375px)', 375, 812, 1480, 1880, C.bg);

  buildMobileTopbar(searchMob, '발송 이력');

  // Filter icon in topbar right
  addRect(searchMob, { name: 'mob-filter-btn', x: 331, y: 14, w: 28, h: 28, fill: C.bg, radius: 6 });
  addText(searchMob, { x: 331, y: 22, text: '⚙', size: 14, color: C.textSecondary, w: 28, align: 'CENTER' });

  // Filter accordion — includes filter inputs AND action buttons inside the same card
  addRect(searchMob, { name: 'mob-filter-accordion', x: 0, y: 56, w: 375, h: 208, fill: C.surface, strokeColor: C.border });
  addText(searchMob, { x: 16, y: 66, text: '검색 조건', size: 13, style: 'Semi Bold', color: C.textPrimary });

  addText(searchMob, { x: 16, y: 90, text: '기간', size: 11, style: 'Medium', color: C.textSecondary });
  addRect(searchMob, { name: 'mob-filter-date', x: 16, y: 106, w: 160, h: 32, fill: C.bg, radius: 6, strokeColor: C.border });
  addText(searchMob, { x: 28, y: 114, text: '최근 7일', size: 12, color: C.textPrimary });

  addText(searchMob, { x: 188, y: 90, text: '상태', size: 11, style: 'Medium', color: C.textSecondary });
  addRect(searchMob, { name: 'mob-filter-status', x: 188, y: 106, w: 171, h: 32, fill: C.bg, radius: 6, strokeColor: C.border });
  addText(searchMob, { x: 200, y: 114, text: '전체', size: 12, color: C.textPrimary });

  addText(searchMob, { x: 16, y: 148, text: '채널', size: 11, style: 'Medium', color: C.textSecondary });
  addRect(searchMob, { name: 'mob-filter-channel', x: 16, y: 164, w: 343, h: 32, fill: C.bg, radius: 6, strokeColor: C.border });
  addText(searchMob, { x: 28, y: 172, text: '전체', size: 12, color: C.textPrimary });

  // Filter action buttons
  addRect(searchMob, { name: 'mob-search-btn', x: 16, y: 208, w: 160, h: 36, fill: C.primary, radius: 8 });
  addText(searchMob, { x: 16, y: 220, text: '검색', size: 13, style: 'Semi Bold', color: C.surface, w: 160, align: 'CENTER' });
  addRect(searchMob, { name: 'mob-reset-btn', x: 184, y: 208, w: 175, h: 36, fill: C.bg, radius: 8, strokeColor: C.border });
  addText(searchMob, { x: 184, y: 220, text: '초기화', size: 13, color: C.textSecondary, w: 175, align: 'CENTER' });

  // Result count
  addText(searchMob, { x: 16, y: 280, text: '총 47건', size: 12, color: C.textSecondary });

  // Card list
  var mobCards = [
    { msgId: 'msg_a1b2c3d4', channel: '이메일', recipient: 'kim@example.com', status: '성공', ok: true, time: '234ms', sentAt: '04-10 14:23' },
    { msgId: 'msg_e5f6g7h8', channel: 'Slack', recipient: '@lee.devteam', status: '성공', ok: true, time: '89ms', sentAt: '04-10 13:45' },
    { msgId: 'msg_i9j0k1l2', channel: 'SMS', recipient: '010-1234-5678', status: '실패', ok: false, time: '1,203ms', sentAt: '04-10 12:11' }
  ];
  var mobCardY = 300;
  for (var mci = 0; mci < mobCards.length; mci++) {
    var mc = mobCards[mci];
    var cardH = 84;
    addRect(searchMob, { name: 'mob-card-' + mci, x: 16, y: mobCardY, w: 343, h: cardH, fill: C.surface, radius: 12, strokeColor: C.border });
    // Top row: msgId + status badge
    addText(searchMob, { x: 28, y: mobCardY + 12, text: mc.msgId, size: 11, color: C.textSecondary });
    var mcStatusColor = mc.ok ? C.success : C.error;
    var mcStatusBg = mc.ok ? C.successLight : C.errorLight;
    buildBadge(searchMob, 285, mobCardY + 10, mc.status, mcStatusColor, mcStatusBg);
    // Middle row: channel badge + recipient
    buildBadge(searchMob, 28, mobCardY + 34, mc.channel, C.primary, C.primaryLight);
    addText(searchMob, { x: 88, y: mobCardY + 38, text: mc.recipient, size: 12, color: C.textPrimary });
    // Bottom row: sentAt + responseTime
    addText(searchMob, { x: 28, y: mobCardY + 60, text: mc.sentAt, size: 11, color: C.textMuted });
    addText(searchMob, { x: 200, y: mobCardY + 60, text: '응답: ' + mc.time, size: 11, color: C.textMuted });
    // Detail link
    addText(searchMob, { x: 290, y: mobCardY + 60, text: '상세 >', size: 11, style: 'Medium', color: C.primary });
    mobCardY += cardH + 12;
  }

  // Mobile empty state
  var mobEmptyY = mobCardY + 20;
  addRect(searchMob, { name: 'mob-empty-indicator', x: 16, y: mobEmptyY, w: 343, h: 16, fill: C.bg, radius: 4 });
  addText(searchMob, { x: 16, y: mobEmptyY + 2, text: '▲ 결과 없을 때: 빈 상태 메시지 표시', size: 9, color: C.textMuted, w: 343, align: 'CENTER' });

  // ─────────────────────────────────────────────────────────────────────────
  // Finalize
  // ─────────────────────────────────────────────────────────────────────────
  figma.currentPage.name = 'Message Tools Wireframe';
  figma.viewport.scrollAndZoomIntoView([sendPc, sendMob, resultPc, resultMob, searchPc, searchMob]);
  figma.closePlugin('Message Tools Wireframe: 6 frames generated');

  }).catch(function (err) {
    figma.closePlugin('Font load failed: ' + (err && err.message ? err.message : String(err)));
  });
}());

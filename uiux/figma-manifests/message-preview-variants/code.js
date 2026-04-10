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

  // ── Color tokens ──────────────────────────────────────────────────────────
  var C = {
    primary: '#2563EB',
    primaryLight: '#EFF6FF',
    success: '#22C55E',
    error: '#DC2626',
    warning: '#F59E0B',
    surface: '#FFFFFF',
    bg: '#F8FAFC',
    border: '#E2E8F0',
    textPrimary: '#0F172A',
    textSecondary: '#64748B',
    textMuted: '#94A3B8',
    // iOS
    iosBlueBubble: '#007AFF',
    iosGrayBubble: '#E5E5EA',
    iosStatusBar: '#F2F2F7',
    // Kakao
    kakaoYellow: '#FAE100',
    kakaoChatBg: '#B2C7D9',
    kakaoCardBg: '#FFFFFF',
    kakaoButtonBorder: '#C8C8C8',
    // RCS
    rcsChipBorder: '#2563EB',
    rcsChatBg: '#F8FAFC',
    // Overview
    overviewBg: '#F1F5F9'
  };

  // ── Shared: iOS status bar ────────────────────────────────────────────────
  function buildStatusBar(frame, timeText) {
    addRect(frame, { name: 'status-bar', x: 0, y: 0, w: frame.width, h: 20, fill: C.iosStatusBar });
    addText(frame, { x: 16, y: 3, text: timeText || '9:41', size: 11, style: 'Medium', color: C.textPrimary });
    addText(frame, { x: 0, y: 3, text: 'WiFi  100%', size: 11, color: C.textSecondary, w: frame.width - 12, align: 'RIGHT' });
  }

  // ── Shared: chat header for iOS-style ─────────────────────────────────────
  function buildChatHeader(frame, yOffset, label, sublabel) {
    addRect(frame, { name: 'chat-header-bg', x: 0, y: yOffset, w: frame.width, h: 52, fill: C.surface, strokeColor: C.border });
    addRect(frame, { name: 'back-btn', x: 12, y: yOffset + 14, w: 24, h: 24, fill: C.bg, radius: 6 });
    addText(frame, { x: 12, y: yOffset + 18, text: '<', size: 13, style: 'Bold', color: C.primary });
    addText(frame, { x: 0, y: yOffset + 10, text: label, size: 15, style: 'Semi Bold', color: C.textPrimary, w: frame.width, align: 'CENTER' });
    if (sublabel) {
      addText(frame, { x: 0, y: yOffset + 30, text: sublabel, size: 11, color: C.textSecondary, w: frame.width, align: 'CENTER' });
    }
  }

  // ── Shared: scroll hint ───────────────────────────────────────────────────
  function buildScrollHint(frame, yOffset) {
    addText(frame, { x: 0, y: yOffset, text: '↑ 스크롤하여 더 보기', size: 11, color: C.textMuted, w: frame.width, align: 'CENTER' });
  }

  // ── Shared: input bar hint ────────────────────────────────────────────────
  function buildInputBarHint(frame, yOffset) {
    addRect(frame, { name: 'input-bar-bg', x: 0, y: yOffset, w: frame.width, h: 56, fill: C.surface, strokeColor: C.border });
    addRect(frame, { name: 'input-field', x: 12, y: yOffset + 10, w: frame.width - 72, h: 36, fill: C.bg, radius: 18, strokeColor: C.border });
    addText(frame, { x: 24, y: yOffset + 20, text: '메시지 입력...', size: 13, color: C.textMuted });
    addRect(frame, { name: 'send-btn', x: frame.width - 52, y: yOffset + 10, w: 36, h: 36, fill: C.iosBlueBubble, radius: 18 });
    addText(frame, { x: frame.width - 52, y: yOffset + 18, text: '↑', size: 16, style: 'Bold', color: '#FFFFFF', w: 36, align: 'CENTER' });
  }

  // ── Shared: iOS blue bubble (sender) ─────────────────────────────────────
  function buildBlueBubble(frame, yOffset, text, timestamp, status, isPanel) {
    var panelW = frame.width;
    var bubbleMaxW = Math.floor(panelW * 0.75);
    var bubblePadH = 12;
    var bubblePadV = 10;

    // Estimate text lines (approx 36 chars per line at 14px in bubbleMaxW-24)
    var charsPerLine = Math.floor((bubbleMaxW - bubblePadH * 2) / 8);
    var lines = Math.ceil(text.length / charsPerLine);
    if (lines < 1) { lines = 1; }
    var bubbleH = lines * 20 + bubblePadV * 2 + 4;
    if (bubbleH < 44) { bubbleH = 44; }

    var bubbleX = panelW - bubbleMaxW - 16;
    addRect(frame, { name: 'blue-bubble', x: bubbleX, y: yOffset, w: bubbleMaxW, h: bubbleH, fill: C.iosBlueBubble, radius: 18 });
    // tail override right-bottom corner
    addRect(frame, { name: 'bubble-tail', x: panelW - 20, y: yOffset + bubbleH - 18, w: 12, h: 12, fill: C.iosBlueBubble, radius: 0 });
    addText(frame, { x: bubbleX + bubblePadH, y: yOffset + bubblePadV, text: text, size: 14, color: '#FFFFFF', w: bubbleMaxW - bubblePadH * 2 });

    var metaY = yOffset + bubbleH + 4;
    addText(frame, { x: panelW - 120, y: metaY, text: timestamp + '  ' + status, size: 11, color: C.textMuted, w: 104, align: 'RIGHT' });

    return metaY + 18;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 1 — Empty State (PC Panel 360x640)
  // ══════════════════════════════════════════════════════════════════════════
  function buildEmptyStatePC(xOffset) {
    var f = makeFrame('Message Preview - Empty State (PC Panel)', 360, 640, xOffset, 0, C.bg);

    // Panel border
    addRect(f, { name: 'panel-border', x: 0, y: 0, w: 360, h: 640, fill: C.surface, strokeColor: C.border });

    // Panel header
    addRect(f, { name: 'panel-header', x: 0, y: 0, w: 360, h: 48, fill: C.surface, strokeColor: C.border });
    addText(f, { x: 16, y: 14, text: '미리보기', size: 14, style: 'Semi Bold', color: C.textPrimary });
    // Channel badge
    addRect(f, { name: 'channel-badge', x: 264, y: 12, w: 80, h: 24, fill: C.bg, radius: 12, strokeColor: C.border });
    addText(f, { x: 264, y: 16, text: 'SMS', size: 12, style: 'Medium', color: C.textSecondary, w: 80, align: 'CENTER' });

    // Center empty state
    var centerX = 180;
    var centerY = 280;
    // Icon circle
    addRect(f, { name: 'icon-circle', x: centerX - 32, y: centerY - 32, w: 64, h: 64, fill: '#E2E8F0', radius: 32 });
    addText(f, { x: centerX - 32, y: centerY - 12, text: '✉', size: 24, color: C.textMuted, w: 64, align: 'CENTER' });

    addText(f, { x: 40, y: centerY + 44, text: '미리보기', size: 16, style: 'Medium', color: C.textSecondary, w: 280, align: 'CENTER' });
    addText(f, { x: 40, y: centerY + 68, text: '수신자와 본문을 입력하면', size: 13, color: C.textMuted, w: 280, align: 'CENTER' });
    addText(f, { x: 40, y: centerY + 86, text: '메시지가 여기에 표시됩니다.', size: 13, color: C.textMuted, w: 280, align: 'CENTER' });

    // Channel hint chips
    var chipY = centerY + 128;
    var chipLabels = ['SMS', '알림톡', 'RCS'];
    var chipX = 60;
    var i;
    for (i = 0; i < chipLabels.length; i++) {
      addRect(f, { name: 'chip-' + chipLabels[i], x: chipX, y: chipY, w: 64, h: 28, fill: C.surface, radius: 14, strokeColor: C.border });
      addText(f, { x: chipX, y: chipY + 7, text: chipLabels[i], size: 12, color: C.textSecondary, w: 64, align: 'CENTER' });
      chipX += 76;
    }
    addText(f, { x: 40, y: chipY + 36, text: '채널을 선택하여 미리보기 형식을 변경할 수 있습니다.', size: 11, color: C.textMuted, w: 280, align: 'CENTER' });

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 2 — SMS PC Panel (360x640)
  // ══════════════════════════════════════════════════════════════════════════
  function buildSmsPC(xOffset) {
    var f = makeFrame('Message Preview - SMS (PC Panel)', 360, 640, xOffset, 0, C.surface);

    // Panel header
    addRect(f, { name: 'panel-header', x: 0, y: 0, w: 360, h: 52, fill: C.surface, strokeColor: C.border });
    addText(f, { x: 16, y: 10, text: '01012345678', size: 14, style: 'Semi Bold', color: C.textPrimary });
    addText(f, { x: 16, y: 30, text: '수신자', size: 11, color: C.textMuted });
    // SMS type badge
    addRect(f, { name: 'sms-badge', x: 268, y: 14, w: 76, h: 24, fill: C.primaryLight, radius: 12 });
    addText(f, { x: 268, y: 18, text: 'SMS 80byte', size: 11, style: 'Medium', color: C.primary, w: 76, align: 'CENTER' });

    // Chat area background
    addRect(f, { name: 'chat-bg', x: 0, y: 52, w: 360, h: 540, fill: C.surface });

    // Date separator
    addRect(f, { name: 'date-separator-line', x: 80, y: 72, w: 200, h: 1, fill: C.border });
    addText(f, { x: 0, y: 64, text: '오늘', size: 11, color: C.textMuted, w: 360, align: 'CENTER' });

    // Blue bubble
    var nextY = buildBlueBubble(f, 96, '[Wisecan] 오늘 오후 3시 회의가 있습니다. 일정을 확인해주세요.', '방금 전', '전송됨', true);

    // Status area
    addText(f, { x: 16, y: nextY + 12, text: '전송 완료', size: 12, style: 'Medium', color: C.success });
    addRect(f, { name: 'status-dot', x: 8, y: nextY + 16, w: 6, h: 6, fill: C.success, radius: 3 });

    // Bottom bar
    addRect(f, { name: 'bottom-bar', x: 0, y: 592, w: 360, h: 48, fill: C.bg, strokeColor: C.border });
    addText(f, { x: 0, y: 606, text: '발송 예정 · 문자 메시지(SMS)', size: 11, color: C.textMuted, w: 360, align: 'CENTER' });

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 3 — LMS PC Panel (360x640)
  // ══════════════════════════════════════════════════════════════════════════
  function buildLmsPC(xOffset) {
    var f = makeFrame('Message Preview - LMS (PC Panel)', 360, 640, xOffset, 0, C.surface);

    // Panel header
    addRect(f, { name: 'panel-header', x: 0, y: 0, w: 360, h: 52, fill: C.surface, strokeColor: C.border });
    addText(f, { x: 16, y: 10, text: '01012345678', size: 14, style: 'Semi Bold', color: C.textPrimary });
    addText(f, { x: 16, y: 30, text: '수신자', size: 11, color: C.textMuted });
    addRect(f, { name: 'lms-badge', x: 268, y: 14, w: 76, h: 24, fill: '#FFF7ED', radius: 12 });
    addText(f, { x: 268, y: 18, text: 'LMS 213byte', size: 11, style: 'Medium', color: '#C2410C', w: 76, align: 'CENTER' });

    // Chat area
    addRect(f, { name: 'chat-bg', x: 0, y: 52, w: 360, h: 540, fill: C.surface });

    // Date separator
    addRect(f, { name: 'date-line', x: 80, y: 72, w: 200, h: 1, fill: C.border });
    addText(f, { x: 0, y: 64, text: '오늘', size: 11, color: C.textMuted, w: 360, align: 'CENTER' });

    // Long message bubble
    var lmsText = '[Wisecan \uC54C\uB9BC] \uC548\uB155\uD558\uC138\uC694 \uAE40OO \uACE0\uAC1D\uB2D8.\n\uC694\uCCAD\uD558\uC2E0 \uC0C1\uC138 \uB0B4\uC6A9\uC744 \uC544\uB798\uC640 \uAC19\uC774 \uC548\uB0B4\uB4DC\uB9BD\uB2C8\uB2E4.\n\n1) \uD56D\uBAA9 A \u2014 \uACB0\uC81C \uC644\uB8CC (2026-04-10)\n2) \uD56D\uBAA9 B \u2014 \uBC30\uC1A1 \uC900\uBE44 \uC911 (\uC608\uC0C1 3~5\uC77C)\n3) \uD56D\uBAA9 C \u2014 \uACE0\uAC1D\uC13C\uD130 \uBB38\uC758 \uAC00\uB2A5\n\n\uCD94\uAC00 \uBB38\uC758\uB294 1588-XXXX\uB85C \uC5F0\uB77D\uC8FC\uC138\uC694.';
    var bubbleMaxW = 268;
    var bubbleX = 360 - bubbleMaxW - 16;
    var bubbleH = 148;
    addRect(f, { name: 'lms-bubble', x: bubbleX, y: 88, w: bubbleMaxW, h: bubbleH, fill: C.iosBlueBubble, radius: 18 });
    addRect(f, { name: 'bubble-tail', x: 340, y: 88 + bubbleH - 18, w: 12, h: 12, fill: C.iosBlueBubble });
    addText(f, { x: bubbleX + 12, y: 100, text: lmsText, size: 13, color: '#FFFFFF', w: bubbleMaxW - 24 });

    // Timestamp + status
    addText(f, { x: 240, y: 88 + bubbleH + 4, text: '\uBC29\uAE08 \uC804  \uC804\uC1A1\uB428', size: 11, color: C.textMuted, w: 104, align: 'RIGHT' });

    // Scroll hint
    buildScrollHint(f, 260);

    // Bottom bar
    addRect(f, { name: 'bottom-bar', x: 0, y: 592, w: 360, h: 48, fill: C.bg, strokeColor: C.border });
    addText(f, { x: 0, y: 606, text: '\uBC1C\uC1A1 \uC608\uC815 \u00B7 \uC7A5\uBB38 \uBA54\uC2DC\uC9C0(LMS)', size: 11, color: C.textMuted, w: 360, align: 'CENTER' });

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 4 — MMS PC Panel (360x640)
  // ══════════════════════════════════════════════════════════════════════════
  function buildMmsPC(xOffset) {
    var f = makeFrame('Message Preview - MMS (PC Panel)', 360, 640, xOffset, 0, C.surface);

    // Panel header
    addRect(f, { name: 'panel-header', x: 0, y: 0, w: 360, h: 52, fill: C.surface, strokeColor: C.border });
    addText(f, { x: 16, y: 10, text: '01012345678', size: 14, style: 'Semi Bold', color: C.textPrimary });
    addText(f, { x: 16, y: 30, text: '\uC218\uC2E0\uC790', size: 11, color: C.textMuted });
    addRect(f, { name: 'mms-badge', x: 272, y: 14, w: 72, h: 24, fill: '#F0FDF4', radius: 12 });
    addText(f, { x: 272, y: 18, text: 'MMS \uCCA8\uBD80', size: 11, style: 'Medium', color: '#16A34A', w: 72, align: 'CENTER' });

    // Chat area
    addRect(f, { name: 'chat-bg', x: 0, y: 52, w: 360, h: 540, fill: C.surface });

    // Date separator
    addRect(f, { name: 'date-line', x: 80, y: 72, w: 200, h: 1, fill: C.border });
    addText(f, { x: 0, y: 64, text: '\uC624\uB298', size: 11, color: C.textMuted, w: 360, align: 'CENTER' });

    // MMS bubble with image
    var bubbleMaxW = 268;
    var bubbleX = 360 - bubbleMaxW - 16;
    var imageH = 150;
    var captionH = 44;
    var bubbleH = imageH + captionH;

    // Image area (top of bubble, rounded top corners only)
    addRect(f, { name: 'mms-image-thumb', x: bubbleX, y: 88, w: bubbleMaxW, h: imageH, fill: '#C8D6E5', radius: 18 });
    // Darken bottom of image to blend with bubble
    addRect(f, { name: 'image-overlay', x: bubbleX, y: 88 + imageH - 18, w: bubbleMaxW, h: 18, fill: C.iosBlueBubble });
    addText(f, { x: bubbleX, y: 88 + imageH / 2 - 10, text: '\uC774\uBCA4\uD2B8 \uBC30\uB108', size: 13, style: 'Medium', color: '#64748B', w: bubbleMaxW, align: 'CENTER' });
    addText(f, { x: bubbleX, y: 88 + imageH / 2 + 8, text: 'JPG \u00B7 1.2MB', size: 11, color: '#94A3B8', w: bubbleMaxW, align: 'CENTER' });

    // Caption bubble
    addRect(f, { name: 'mms-caption-bubble', x: bubbleX, y: 88 + imageH, w: bubbleMaxW, h: captionH, fill: C.iosBlueBubble, radius: 0 });
    // bottom corners round
    addRect(f, { name: 'caption-tail', x: 340, y: 88 + imageH + captionH - 18, w: 12, h: 12, fill: C.iosBlueBubble });
    addText(f, { x: bubbleX + 12, y: 88 + imageH + 12, text: '[Wisecan \uC774\uBCA4\uD2B8] \uC2E0\uADDC \uACE0\uAC1D 20% \uD560\uC778 \uCFE0\uD3F0\uC785\uB2C8\uB2E4.', size: 13, color: '#FFFFFF', w: bubbleMaxW - 24 });

    // Timestamp
    addText(f, { x: 240, y: 88 + bubbleH + 4, text: '\uBC29\uAE08 \uC804  \uC804\uC1A1\uB428', size: 11, color: C.textMuted, w: 104, align: 'RIGHT' });

    // Bottom bar
    addRect(f, { name: 'bottom-bar', x: 0, y: 592, w: 360, h: 48, fill: C.bg, strokeColor: C.border });
    addText(f, { x: 0, y: 606, text: '\uBC1C\uC1A1 \uC608\uC815 \u00B7 \uBA40\uD2F0\uBBF8\uB514\uC5B4(MMS)', size: 11, color: C.textMuted, w: 360, align: 'CENTER' });

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 5 — SMS Mobile Full (375x812)
  // ══════════════════════════════════════════════════════════════════════════
  function buildSmsMobile(xOffset) {
    var f = makeFrame('Message Preview - SMS (Mobile Full)', 375, 812, xOffset, 0, C.surface);

    buildStatusBar(f, '9:41');
    buildChatHeader(f, 20, '01012345678', '\uC218\uC2E0\uC790');

    // Chat area
    addRect(f, { name: 'chat-bg', x: 0, y: 72, w: 375, h: 684, fill: C.surface });

    // Date separator
    addRect(f, { name: 'date-line', x: 87, y: 90, w: 200, h: 1, fill: C.border });
    addText(f, { x: 0, y: 82, text: '\uC624\uB298', size: 11, color: C.textMuted, w: 375, align: 'CENTER' });

    // Blue bubble (mobile wider)
    var bubbleMaxW = 280;
    var bubbleX = 375 - bubbleMaxW - 12;
    var bubbleH = 72;
    addRect(f, { name: 'blue-bubble', x: bubbleX, y: 108, w: bubbleMaxW, h: bubbleH, fill: C.iosBlueBubble, radius: 18 });
    addRect(f, { name: 'bubble-tail', x: 363, y: 108 + bubbleH - 18, w: 12, h: 12, fill: C.iosBlueBubble });
    addText(f, { x: bubbleX + 12, y: 120, text: '[Wisecan] \uC624\uB298 \uC624\uD6C4 3\uC2DC \uD68C\uC758\uAC00 \uC788\uC2B5\uB2C8\uB2E4. \uC77C\uC815\uC744 \uD655\uC778\uD574\uC8FC\uC138\uC694.', size: 14, color: '#FFFFFF', w: bubbleMaxW - 24 });

    // Timestamp + read status
    addText(f, { x: 255, y: 108 + bubbleH + 4, text: '\uC624\uD6C4 3:00  \uC77D\uC74C', size: 11, color: C.textMuted, w: 108, align: 'RIGHT' });

    // Input bar
    buildInputBarHint(f, 756);

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 6 — Kakao PC Panel (360x640)
  // ══════════════════════════════════════════════════════════════════════════
  function buildKakaoPC(xOffset) {
    var f = makeFrame('Message Preview - \uCE74\uCE74\uC624 \uC54C\uB9BC\uD1A1 (PC Panel)', 360, 640, xOffset, 0, C.kakaoChatBg);

    // Panel header (kakao-style)
    addRect(f, { name: 'panel-header', x: 0, y: 0, w: 360, h: 52, fill: C.kakaoYellow });
    addText(f, { x: 0, y: 16, text: 'Wisecan', size: 16, style: 'Bold', color: C.textPrimary, w: 360, align: 'CENTER' });
    addText(f, { x: 0, y: 36, text: '\uBE44\uC988\uB2C8\uC2A4 \uCC44\uB110', size: 11, color: '#665500', w: 360, align: 'CENTER' });

    // Channel profile row
    addRect(f, { name: 'profile-icon', x: 16, y: 68, w: 40, h: 40, fill: C.kakaoYellow, radius: 12 });
    addText(f, { x: 16, y: 80, text: 'W', size: 18, style: 'Bold', color: C.textPrimary, w: 40, align: 'CENTER' });
    addText(f, { x: 64, y: 72, text: 'Wisecan', size: 13, style: 'Semi Bold', color: C.textPrimary });
    // Verified badge
    addRect(f, { name: 'verified-bg', x: 116, y: 74, w: 48, h: 18, fill: '#F0FDF4', radius: 9 });
    addText(f, { x: 116, y: 77, text: '\u2713 \uC778\uC99D', size: 10, style: 'Medium', color: '#16A34A', w: 48, align: 'CENTER' });
    addText(f, { x: 64, y: 90, text: '\uBE44\uC988\uB2C8\uC2A4 \uCC44\uB110', size: 11, color: C.textMuted });

    // Alimtalk card
    var cardY = 120;
    var cardW = 312;
    var cardX = 16;
    var cardH = 240;
    addRect(f, { name: 'alimtalk-card', x: cardX, y: cardY, w: cardW, h: cardH, fill: C.kakaoCardBg, radius: 12, strokeColor: '#D1D5DB' });

    // Card header
    addRect(f, { name: 'card-header', x: cardX, y: cardY, w: cardW, h: 36, fill: '#FFFBEB', radius: 0 });
    // Round top corners manually with overlay
    addRect(f, { name: 'card-header-top', x: cardX, y: cardY, w: cardW, h: 12, fill: '#FFFBEB', radius: 12 });
    addRect(f, { name: 'card-header-bottom', x: cardX, y: cardY + 24, w: cardW, h: 12, fill: '#FFFBEB' });
    addText(f, { x: cardX + 12, y: cardY + 10, text: '\uACB0\uC81C \uC644\uB8CC \uC54C\uB9BC', size: 12, style: 'Semi Bold', color: '#92400E', w: cardW - 24 });

    // Card body
    var bodyText = '\uC8FC\uBB38 \uBC88\uD638 #12345 \uACB0\uC81C\uAC00 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.\n\n- \uC0C1\uD488\uBA85: Wisecan MCP \uC774\uC6A9\uAD8C (1\uAC1C\uC6D4)\n- \uACB0\uC81C\uAE08\uC561: 29,000\uC6D0\n- \uACB0\uC81C\uC77C\uC2DC: 2026-04-10 14:32';
    addText(f, { x: cardX + 12, y: cardY + 46, text: bodyText, size: 13, color: C.textPrimary, w: cardW - 24 });

    // Separator
    addRect(f, { name: 'card-separator', x: cardX, y: cardY + 150, w: cardW, h: 1, fill: C.border });

    // Buttons
    addRect(f, { name: 'btn1-bg', x: cardX + 8, y: cardY + 158, w: cardW - 16, h: 36, fill: '#FFFFFF', radius: 8, strokeColor: C.kakaoButtonBorder });
    addText(f, { x: cardX + 8, y: cardY + 167, text: '\uC8FC\uBB38 \uC0C1\uC138 \uBCF4\uAE30', size: 13, style: 'Medium', color: C.textPrimary, w: cardW - 16, align: 'CENTER' });

    addRect(f, { name: 'btn2-bg', x: cardX + 8, y: cardY + 198, w: cardW - 16, h: 36, fill: '#FFFFFF', radius: 8, strokeColor: C.kakaoButtonBorder });
    addText(f, { x: cardX + 8, y: cardY + 207, text: '\uACE0\uAC1D\uC13C\uD130', size: 13, style: 'Medium', color: C.textPrimary, w: cardW - 16, align: 'CENTER' });

    // Timestamp
    addText(f, { x: cardX + cardW + 4, y: cardY + cardH - 16, text: '\uC624\uD6C4 2:32', size: 11, color: C.textMuted });

    // Disclaimer
    addText(f, { x: 16, y: cardY + cardH + 12, text: '\uC774 \uBA54\uC2DC\uC9C0\uB294 Wisecan\uC5D0\uC11C \uBCF4\uB0B8 \uC54C\uB9BC \uBA54\uC2DC\uC9C0\uC785\uB2C8\uB2E4.', size: 11, color: '#5C4A00', w: 328 });

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 7 — Kakao Mobile Full (375x812)
  // ══════════════════════════════════════════════════════════════════════════
  function buildKakaoMobile(xOffset) {
    var f = makeFrame('Message Preview - \uCE74\uCE74\uC624 \uC54C\uB9BC\uD1A1 (Mobile Full)', 375, 812, xOffset, 0, C.kakaoChatBg);

    // Kakao top bar
    addRect(f, { name: 'kakao-topbar', x: 0, y: 0, w: 375, h: 52, fill: C.kakaoYellow });
    addRect(f, { name: 'back-btn', x: 12, y: 14, w: 24, h: 24, fill: 'transparent' });
    addText(f, { x: 12, y: 18, text: '<', size: 14, style: 'Bold', color: '#4A3800' });
    addText(f, { x: 0, y: 8, text: 'Wisecan', size: 16, style: 'Bold', color: C.textPrimary, w: 375, align: 'CENTER' });
    addText(f, { x: 0, y: 28, text: '\uBE44\uC988\uB2C8\uC2A4 \uCC44\uB110', size: 11, color: '#665500', w: 375, align: 'CENTER' });

    // Profile row
    addRect(f, { name: 'profile-icon', x: 16, y: 68, w: 40, h: 40, fill: C.kakaoYellow, radius: 12 });
    addText(f, { x: 16, y: 80, text: 'W', size: 18, style: 'Bold', color: C.textPrimary, w: 40, align: 'CENTER' });
    addText(f, { x: 64, y: 75, text: 'Wisecan', size: 13, style: 'Semi Bold', color: C.textPrimary });

    // Card
    var cardY = 120;
    var cardW = 328;
    var cardX = 16;
    var cardH = 228;
    addRect(f, { name: 'alimtalk-card', x: cardX, y: cardY, w: cardW, h: cardH, fill: C.kakaoCardBg, radius: 12, strokeColor: '#D1D5DB' });

    addRect(f, { name: 'card-header', x: cardX, y: cardY, w: cardW, h: 36, fill: '#FFFBEB', radius: 12 });
    addRect(f, { name: 'card-header-cover', x: cardX, y: cardY + 24, w: cardW, h: 12, fill: '#FFFBEB' });
    addText(f, { x: cardX + 12, y: cardY + 10, text: '\uACB0\uC81C \uC644\uB8CC \uC54C\uB9BC', size: 12, style: 'Semi Bold', color: '#92400E', w: cardW - 24 });

    var bodyText = '\uC8FC\uBB38 \uBC88\uD638 #12345 \uACB0\uC81C\uAC00 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.\n\n- \uC0C1\uD488\uBA85: Wisecan MCP \uC774\uC6A9\uAD8C (1\uAC1C\uC6D4)\n- \uACB0\uC81C\uAE08\uC561: 29,000\uC6D0';
    addText(f, { x: cardX + 12, y: cardY + 46, text: bodyText, size: 13, color: C.textPrimary, w: cardW - 24 });

    addRect(f, { name: 'card-separator', x: cardX, y: cardY + 136, w: cardW, h: 1, fill: C.border });

    addRect(f, { name: 'btn1-bg', x: cardX + 8, y: cardY + 144, w: cardW - 16, h: 36, fill: '#FFFFFF', radius: 8, strokeColor: C.kakaoButtonBorder });
    addText(f, { x: cardX + 8, y: cardY + 153, text: '\uC8FC\uBB38 \uC0C1\uC138 \uBCF4\uAE30', size: 13, style: 'Medium', color: C.textPrimary, w: cardW - 16, align: 'CENTER' });

    addRect(f, { name: 'btn2-bg', x: cardX + 8, y: cardY + 184, w: cardW - 16, h: 36, fill: '#FFFFFF', radius: 8, strokeColor: C.kakaoButtonBorder });
    addText(f, { x: cardX + 8, y: cardY + 193, text: '\uACE0\uAC1D\uC13C\uD130', size: 13, style: 'Medium', color: C.textPrimary, w: cardW - 16, align: 'CENTER' });

    addText(f, { x: 16, y: cardY + cardH + 12, text: '\uC774 \uBA54\uC2DC\uC9C0\uB294 Wisecan\uC5D0\uC11C \uBCF4\uB0B8 \uC54C\uB9BC \uBA54\uC2DC\uC9C0\uC785\uB2C8\uB2E4.', size: 11, color: '#5C4A00', w: 343 });

    // Input bar
    buildInputBarHint(f, 756);

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 8 — RCS PC Panel (360x640)
  // ══════════════════════════════════════════════════════════════════════════
  function buildRcsPC(xOffset) {
    var f = makeFrame('Message Preview - RCS (PC Panel)', 360, 640, xOffset, 0, C.rcsChatBg);

    // Panel header
    addRect(f, { name: 'panel-header', x: 0, y: 0, w: 360, h: 52, fill: C.surface, strokeColor: C.border });
    addText(f, { x: 16, y: 10, text: 'Wisecan', size: 15, style: 'Semi Bold', color: C.textPrimary });
    // Verified badge
    addRect(f, { name: 'verified-bg', x: 92, y: 12, w: 56, h: 20, fill: '#F0FDF4', radius: 10 });
    addText(f, { x: 92, y: 15, text: '\u2713 \uC778\uC99D\uB428', size: 10, style: 'Medium', color: '#16A34A', w: 56, align: 'CENTER' });
    addText(f, { x: 16, y: 32, text: 'RCS \uBE44\uC988\uB2C8\uC2A4 \uBA54\uC2DC\uC9C0', size: 11, color: C.textMuted });

    // Chat area bg
    addRect(f, { name: 'chat-area', x: 0, y: 52, w: 360, h: 540, fill: C.rcsChatBg });

    // Sender profile row
    addRect(f, { name: 'sender-avatar', x: 16, y: 68, w: 36, h: 36, fill: C.primaryLight, radius: 18 });
    addText(f, { x: 16, y: 80, text: 'W', size: 16, style: 'Bold', color: C.primary, w: 36, align: 'CENTER' });
    addText(f, { x: 60, y: 72, text: 'Wisecan', size: 13, style: 'Medium', color: C.textPrimary });
    addRect(f, { name: 'rcs-verified', x: 106, y: 74, w: 48, h: 18, fill: '#DCFCE7', radius: 9 });
    addText(f, { x: 106, y: 77, text: '\u2713 \uC778\uC99D', size: 10, style: 'Medium', color: '#16A34A', w: 48, align: 'CENTER' });

    // RCS Card
    var cardY = 116;
    var cardX = 16;
    var cardW = 328;
    addRect(f, { name: 'rcs-card', x: cardX, y: cardY, w: cardW, h: 320, fill: C.rcsCardBg, radius: 12, strokeColor: C.border });

    // Card image placeholder (16:9 -> 328 * 184)
    addRect(f, { name: 'rcs-card-image', x: cardX, y: cardY, w: cardW, h: 184, fill: '#CBD5E1', radius: 12 });
    addRect(f, { name: 'img-bottom-cover', x: cardX, y: cardY + 172, w: cardW, h: 12, fill: '#CBD5E1' });
    addText(f, { x: cardX, y: cardY + 80, text: '\uC8FC\uB9D0 \uD2B9\uBCC4 \uC774\uBCA4\uD2B8 \uBC30\uB108', size: 14, style: 'Medium', color: '#475569', w: cardW, align: 'CENTER' });
    addText(f, { x: cardX, y: cardY + 100, text: '16:9 \uC774\uBBF8\uC9C0 \uC601\uC5ED', size: 11, color: '#94A3B8', w: cardW, align: 'CENTER' });

    // Card content
    addText(f, { x: cardX + 16, y: cardY + 196, text: '\uC8FC\uB9D0 \uD2B9\uBCC4 \uC774\uBCA4\uD2B8', size: 16, style: 'Semi Bold', color: C.textPrimary, w: cardW - 32 });
    addText(f, { x: cardX + 16, y: cardY + 220, text: '\uC774\uBC88 \uC8FC\uB9D0 \uD55C\uC815! Wisecan \uC2E0\uADDC \uAC00\uC785 \uACE0\uAC1D\uC5D0\uAC8C \uD504\uB9AC\uBBF8\uC5C4 \uD50C\uB79C 1\uAC1C\uC6D4 \uBB34\uB8CC \uC81C\uACF5. \uC9C0\uAE08 \uBC14\uB85C \uD655\uC778\uD558\uC138\uC694.', size: 13, color: C.textSecondary, w: cardW - 32 });

    // Separator
    addRect(f, { name: 'card-sep', x: cardX, y: cardY + 278, w: cardW, h: 1, fill: C.border });

    // Action chips
    var chipLabels = ['\uC608\uC57D\uD558\uAE30', '\uC9C0\uB3C4 \uBCF4\uAE30', '\uACF5\uC720'];
    var chipY = cardY + 287;
    var chipX = cardX + 8;
    var i;
    for (i = 0; i < chipLabels.length; i++) {
      var chipW = 88;
      addRect(f, { name: 'chip-' + i, x: chipX, y: chipY, w: chipW, h: 28, fill: '#FFFFFF', radius: 14, strokeColor: C.rcsChipBorder });
      addText(f, { x: chipX, y: chipY + 7, text: chipLabels[i], size: 12, style: 'Medium', color: C.primary, w: chipW, align: 'CENTER' });
      chipX += chipW + 8;
    }

    // Timestamp
    addText(f, { x: cardX + cardW + 4, y: cardY + 300, text: '\uC624\uD6C4 4:15', size: 11, color: C.textMuted });

    // Footer
    addText(f, { x: 0, y: 590, text: '\uC548\uC804\uD55C RCS \uBA54\uC2DC\uC9C0 \u00B7 Wisecan \uC778\uC99D \uBC1C\uC2E0', size: 11, color: C.textMuted, w: 360, align: 'CENTER' });

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 9 — RCS Mobile Full (375x812)
  // ══════════════════════════════════════════════════════════════════════════
  function buildRcsMobile(xOffset) {
    var f = makeFrame('Message Preview - RCS (Mobile Full)', 375, 812, xOffset, 0, C.rcsChatBg);

    buildStatusBar(f, '9:41');

    // Chat header
    addRect(f, { name: 'chat-header', x: 0, y: 20, w: 375, h: 52, fill: C.surface, strokeColor: C.border });
    addRect(f, { name: 'back-btn', x: 12, y: 34, w: 24, h: 24, fill: C.bg, radius: 6 });
    addText(f, { x: 12, y: 38, text: '<', size: 13, style: 'Bold', color: C.primary });
    addText(f, { x: 0, y: 30, text: 'Wisecan', size: 15, style: 'Semi Bold', color: C.textPrimary, w: 375, align: 'CENTER' });
    addRect(f, { name: 'verified-badge', x: 196, y: 34, w: 52, h: 18, fill: '#DCFCE7', radius: 9 });
    addText(f, { x: 196, y: 37, text: '\u2713 \uC778\uC99D\uB428', size: 10, style: 'Medium', color: '#16A34A', w: 52, align: 'CENTER' });
    addText(f, { x: 0, y: 50, text: 'RCS \uBE44\uC988\uB2C8\uC2A4 \uBA54\uC2DC\uC9C0', size: 11, color: C.textMuted, w: 375, align: 'CENTER' });

    // Sender profile
    addRect(f, { name: 'sender-avatar', x: 16, y: 84, w: 36, h: 36, fill: C.primaryLight, radius: 18 });
    addText(f, { x: 16, y: 96, text: 'W', size: 16, style: 'Bold', color: C.primary, w: 36, align: 'CENTER' });
    addText(f, { x: 60, y: 92, text: 'Wisecan', size: 13, style: 'Medium', color: C.textPrimary });

    // RCS card
    var cardY = 132;
    var cardX = 16;
    var cardW = 343;
    addRect(f, { name: 'rcs-card', x: cardX, y: cardY, w: cardW, h: 328, fill: C.surface, radius: 12, strokeColor: C.border });

    // Image
    addRect(f, { name: 'card-image', x: cardX, y: cardY, w: cardW, h: 193, fill: '#CBD5E1', radius: 12 });
    addRect(f, { name: 'img-cover', x: cardX, y: cardY + 181, w: cardW, h: 12, fill: '#CBD5E1' });
    addText(f, { x: cardX, y: cardY + 84, text: '\uC8FC\uB9D0 \uD2B9\uBCC4 \uC774\uBCA4\uD2B8 \uBC30\uB108', size: 14, style: 'Medium', color: '#475569', w: cardW, align: 'CENTER' });

    addText(f, { x: cardX + 16, y: cardY + 205, text: '\uC8FC\uB9D0 \uD2B9\uBCC4 \uC774\uBCA4\uD2B8', size: 16, style: 'Semi Bold', color: C.textPrimary, w: cardW - 32 });
    addText(f, { x: cardX + 16, y: cardY + 229, text: '\uC774\uBC88 \uC8FC\uB9D0 \uD55C\uC815! Wisecan \uC2E0\uADDC \uAC00\uC785 \uACE0\uAC1D\uC5D0\uAC8C \uD504\uB9AC\uBBF8\uC5C4 \uD50C\uB79C 1\uAC1C\uC6D4 \uBB34\uB8CC \uC81C\uACF5.', size: 13, color: C.textSecondary, w: cardW - 32 });

    addRect(f, { name: 'card-sep', x: cardX, y: cardY + 280, w: cardW, h: 1, fill: C.border });

    // Action chips
    var chipLabels = ['\uC608\uC57D\uD558\uAE30', '\uC9C0\uB3C4 \uBCF4\uAE30', '\uACF5\uC720'];
    var chipY = cardY + 292;
    var chipX = cardX + 8;
    var i;
    for (i = 0; i < chipLabels.length; i++) {
      var chipW = 96;
      addRect(f, { name: 'chip-' + i, x: chipX, y: chipY, w: chipW, h: 28, fill: '#FFFFFF', radius: 14, strokeColor: C.rcsChipBorder });
      addText(f, { x: chipX, y: chipY + 7, text: chipLabels[i], size: 12, style: 'Medium', color: C.primary, w: chipW, align: 'CENTER' });
      chipX += chipW + 8;
    }

    addText(f, { x: 0, y: cardY + 328 + 12, text: '\uC548\uC804\uD55C RCS \uBA54\uC2DC\uC9C0 \u00B7 Wisecan \uC778\uC99D \uBC1C\uC2E0', size: 11, color: C.textMuted, w: 375, align: 'CENTER' });

    buildInputBarHint(f, 756);

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FRAME 10 — Overview (1200x720)
  // ══════════════════════════════════════════════════════════════════════════
  function buildOverview(xOffset) {
    var f = makeFrame('Message Preview - Overview (All Variants)', 1200, 720, xOffset, 0, C.overviewBg);

    // Title
    addText(f, { x: 0, y: 28, text: '메시지 미리보기 컴포넌트 — 채널별 Variant', size: 22, style: 'Bold', color: C.textPrimary, w: 1200, align: 'CENTER' });
    addText(f, { x: 0, y: 60, text: 'SendMessageForm 우측 미리보기 패널 (360×640) — 채널 선택에 따라 렌더링 variant 전환', size: 14, color: C.textSecondary, w: 1200, align: 'CENTER' });

    // Panel slots (3 × 360 wide, starting x=40, gap=40)
    var panelConfigs = [
      { label: 'SMS / LMS / MMS', desc: '문자 메시지', color: C.iosBlueBubble, badge: 'SMS' },
      { label: '카카오 알림톡', desc: 'KakaoTalk Business', color: C.kakaoYellow, badge: 'Alimtalk' },
      { label: 'RCS', desc: 'Rich Communication Services', color: C.primary, badge: 'RCS' }
    ];
    var panelX = 40;
    var panelY = 96;
    var panelW = 340;
    var panelH = 560;
    var i;
    for (i = 0; i < panelConfigs.length; i++) {
      var cfg = panelConfigs[i];

      // Label above panel
      addText(f, { x: panelX, y: panelY - 36, text: cfg.label, size: 16, style: 'Semi Bold', color: C.textPrimary, w: panelW });
      addText(f, { x: panelX, y: panelY - 16, text: cfg.desc, size: 12, color: C.textSecondary, w: panelW });

      // Panel shadow / frame
      addRect(f, { name: 'panel-shadow-' + i, x: panelX + 4, y: panelY + 4, w: panelW, h: panelH, fill: '#D1D5DB', radius: 16 });
      addRect(f, { name: 'panel-bg-' + i, x: panelX, y: panelY, w: panelW, h: panelH, fill: C.surface, radius: 16, strokeColor: C.border });

      // Channel color header
      addRect(f, { name: 'panel-header-' + i, x: panelX, y: panelY, w: panelW, h: 48, fill: cfg.color, radius: 16 });
      addRect(f, { name: 'panel-header-bottom-cover-' + i, x: panelX, y: panelY + 36, w: panelW, h: 12, fill: cfg.color });

      // Header text (use black for kakao yellow, white for others)
      var headerTextColor = (i === 1) ? '#0F172A' : '#FFFFFF';
      addText(f, { x: panelX, y: panelY + 14, text: cfg.label, size: 14, style: 'Semi Bold', color: headerTextColor, w: panelW, align: 'CENTER' });

      // Preview body placeholder
      addRect(f, { name: 'preview-body-bg-' + i, x: panelX + 12, y: panelY + 60, w: panelW - 24, h: panelH - 120, fill: C.bg, radius: 8 });

      // Mock bubble or card inside
      if (i === 0) {
        // SMS bubble style
        addRect(f, { name: 'mock-bubble-' + i, x: panelX + 48, y: panelY + 84, w: 240, h: 60, fill: C.iosBlueBubble, radius: 16 });
        addText(f, { x: panelX + 60, y: panelY + 96, text: '[Wisecan] 오늘 오후 3시\n회의가 있습니다.', size: 12, color: '#FFFFFF', w: 216 });
        addText(f, { x: panelX + 220, y: panelY + 152, text: '방금 전  전송됨', size: 10, color: C.textMuted, w: 100, align: 'RIGHT' });
      } else if (i === 1) {
        // Kakao card style
        addRect(f, { name: 'mock-card-' + i, x: panelX + 12, y: panelY + 80, w: panelW - 24, h: 200, fill: '#FFFFFF', radius: 10, strokeColor: '#D1D5DB' });
        addRect(f, { name: 'mock-card-header-' + i, x: panelX + 12, y: panelY + 80, w: panelW - 24, h: 32, fill: '#FFFBEB', radius: 10 });
        addRect(f, { name: 'mock-card-hdr-cover-' + i, x: panelX + 12, y: panelY + 102, w: panelW - 24, h: 10, fill: '#FFFBEB' });
        addText(f, { x: panelX + 24, y: panelY + 92, text: '결제 완료 알림', size: 12, style: 'Semi Bold', color: '#92400E', w: panelW - 48 });
        addText(f, { x: panelX + 24, y: panelY + 124, text: '주문 번호 #12345\n결제가 완료되었습니다.', size: 12, color: C.textPrimary, w: panelW - 48 });
        addRect(f, { name: 'mock-sep-' + i, x: panelX + 12, y: panelY + 200, w: panelW - 24, h: 1, fill: C.border });
        addRect(f, { name: 'mock-btn-' + i, x: panelX + 20, y: panelY + 210, w: panelW - 40, h: 32, fill: '#FFFFFF', radius: 6, strokeColor: C.kakaoButtonBorder });
        addText(f, { x: panelX + 20, y: panelY + 220, text: '주문 상세 보기', size: 12, color: C.textPrimary, w: panelW - 40, align: 'CENTER' });
      } else {
        // RCS card style
        addRect(f, { name: 'mock-rcs-card-' + i, x: panelX + 12, y: panelY + 80, w: panelW - 24, h: 220, fill: '#FFFFFF', radius: 10, strokeColor: C.border });
        addRect(f, { name: 'mock-rcs-img-' + i, x: panelX + 12, y: panelY + 80, w: panelW - 24, h: 120, fill: '#CBD5E1', radius: 10 });
        addRect(f, { name: 'mock-rcs-img-cover-' + i, x: panelX + 12, y: panelY + 190, w: panelW - 24, h: 10, fill: '#CBD5E1' });
        addText(f, { x: panelX + 12, y: panelY + 128, text: '이벤트 배너', size: 12, color: '#475569', w: panelW - 24, align: 'CENTER' });
        addText(f, { x: panelX + 24, y: panelY + 208, text: '주말 특별 이벤트', size: 13, style: 'Semi Bold', color: C.textPrimary, w: panelW - 48 });
        // Chips
        var chipLabels2 = ['예약하기', '지도 보기', '공유'];
        var cX = panelX + 24;
        var j;
        for (j = 0; j < chipLabels2.length; j++) {
          var cW = 76;
          addRect(f, { name: 'ov-chip-' + i + '-' + j, x: cX, y: panelY + 272, w: cW, h: 24, fill: '#FFFFFF', radius: 12, strokeColor: C.primary });
          addText(f, { x: cX, y: panelY + 279, text: chipLabels2[j], size: 10, style: 'Medium', color: C.primary, w: cW, align: 'CENTER' });
          cX += cW + 6;
        }
      }

      // Bottom info
      addRect(f, { name: 'panel-footer-' + i, x: panelX, y: panelY + panelH - 48, w: panelW, h: 48, fill: C.bg, radius: 0 });
      addRect(f, { name: 'panel-footer-radius-cover-' + i, x: panelX, y: panelY + panelH - 12, w: panelW, h: 12, fill: C.surface, radius: 16 });
      addText(f, { x: panelX, y: panelY + panelH - 34, text: cfg.badge + ' variant', size: 11, style: 'Medium', color: C.textSecondary, w: panelW, align: 'CENTER' });

      panelX += panelW + 40;
    }

    // Footer note
    addText(f, { x: 0, y: 676, text: '* 각 패널은 SendMessageForm 우측 360px 영역에 렌더링. 채널 탭 선택으로 variant 전환.', size: 11, color: C.textMuted, w: 1200, align: 'CENTER' });

    return f;
  }

  // ══════════════════════════════════════════════════════════════════════════
  // MAIN — Font preload + frame generation
  // ══════════════════════════════════════════════════════════════════════════
  Promise.all([
    figma.loadFontAsync({ family: 'Inter', style: 'Regular' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Medium' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' }),
    figma.loadFontAsync({ family: 'Inter', style: 'Bold' })
  ]).then(function () {

    // Row 0 — PC Panels (y=0): Empty, SMS, LMS, MMS  (360px wide, gap 40px)
    buildEmptyStatePC(0);     // x=0
    buildSmsPC(400);          // x=400
    buildLmsPC(800);          // x=800
    buildMmsPC(1200);         // x=1200

    // Row 0 — PC Panels continued: Kakao PC, RCS PC
    buildKakaoPC(1600);       // x=1600
    buildRcsPC(2000);         // x=2000

    // Row 1 — Mobile Full frames (y=700): SMS, Kakao, RCS
    var smsMob  = buildSmsMobile(0);
    var kakaoMob = buildKakaoMobile(400);
    var rcsMob   = buildRcsMobile(800);
    smsMob.y  = 700;
    kakaoMob.y = 700;
    rcsMob.y   = 700;

    // Row 2 — Overview (y=1560)
    var overviewF = buildOverview(0);
    overviewF.y = 1560;

    figma.viewport.scrollAndZoomIntoView(figma.currentPage.children);
    figma.closePlugin('메시지 미리보기 Variant 와이어프레임 생성 완료 (10개 프레임)');
  });

})();

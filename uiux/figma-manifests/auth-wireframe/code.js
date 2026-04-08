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
  page.name = 'Auth Wireframe';

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
    if (opts.fill) {
      r.fills = solid(opts.fill);
    } else {
      r.fills = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    }
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
    f.fills = solid(bg || '#FFFFFF');
    page.appendChild(f);
    return f;
  }

  // ── Input field builder ───────────────────────────────────────────────────
  function addInputField(parent, x, y, w, label, placeholder, opts = {}) {
    addText(parent, { x, y, text: label, size: 13, style: 'Medium', color: '#0F172A' });
    addRect(parent, {
      name: `input-${label}`, x, y: y + 22, w, h: 40,
      fill: '#FFFFFF', radius: 8,
      strokeColor: opts.error ? '#EF4444' : '#E2E8F0', strokeWeight: 1,
    });
    addText(parent, { x: x + 12, y: y + 32, text: placeholder, size: 13, style: 'Regular', color: '#94A3B8' });
    if (opts.hint) {
      addText(parent, { x, y: y + 66, text: opts.hint, size: 11, style: 'Regular', color: '#94A3B8' });
    }
    return opts.hint ? 80 : 74;
  }

  // ── Brand panel builder (for PC left column) ──────────────────────────────
  function buildBrandPanel(frame, w, h, headline, subtext, bullets) {
    // Blue gradient background
    addRect(frame, { name: 'brand-bg', x: 0, y: 0, w, h, fill: '#2563EB', radius: 0 });
    // Overlay gradient feel — darker rect at bottom
    addRect(frame, { name: 'brand-gradient', x: 0, y: h * 0.5, w, h: h * 0.5, fill: '#1E40AF', radius: 0 });

    // Logo
    addText(frame, { x: 56, y: 56, text: 'Wisecan', size: 24, style: 'Bold', color: '#FFFFFF' });

    // Headline
    const headlineNode = figma.createText();
    headlineNode.x = 56;
    headlineNode.y = h / 2 - 120;
    headlineNode.characters = headline;
    headlineNode.fontSize = 36;
    headlineNode.fontName = { family: 'Inter', style: 'Extra Bold' };
    headlineNode.fills = solid('#FFFFFF');
    headlineNode.textAutoResize = 'HEIGHT';
    headlineNode.resize(w - 112, headlineNode.height);
    frame.appendChild(headlineNode);

    // Subtext
    addText(frame, {
      x: 56, y: h / 2 - 30,
      text: subtext, size: 16, style: 'Regular', color: '#BFDBFE',
      w: w - 112,
    });

    // Bullets
    let bulletY = h / 2 + 40;
    for (const bullet of bullets) {
      addRect(frame, { name: 'bullet-dot', x: 56, y: bulletY + 4, w: 8, h: 8, fill: '#93C5FD', radius: 4 });
      addText(frame, { x: 76, y: bulletY, text: bullet, size: 15, style: 'Regular', color: '#E0F2FE' });
      bulletY += 32;
    }
  }

  // ── Error banner builder (알림 배너 스타일) ──────────────────────────────
  function addErrorBanner(parent, x, y, w, msg) {
    // 배경: 연한 빨강 + 좌측 빨간 강조 바
    addRect(parent, { name: 'error-banner-bg', x, y, w, h: 52, fill: '#FEF2F2', radius: 6, strokeColor: '#FECACA', strokeWeight: 1 });
    addRect(parent, { name: 'error-bar', x, y, w: 4, h: 52, fill: '#EF4444', radius: 0 });
    // 경고 아이콘 (원형)
    addRect(parent, { name: 'error-icon-bg', x: x + 16, y: y + 14, w: 24, h: 24, fill: '#FEE2E2', radius: 12 });
    addText(parent, { x: x + 25, y: y + 17, text: '!', size: 14, style: 'Bold', color: '#EF4444' });
    // 에러 메시지
    addText(parent, { x: x + 48, y: y + 17, text: msg, size: 13, style: 'Regular', color: '#991B1B' });
    return 64;
  }

  // ── Submit button ─────────────────────────────────────────────────────────
  function addSubmitButton(parent, x, y, w, h, label) {
    addRect(parent, { name: 'btn-submit', x, y, w, h, fill: '#2563EB', radius: 8 });
    addText(parent, { x: x + w / 2 - label.length * 4, y: y + (h - 14) / 2, text: label, size: 14, style: 'Semi Bold', color: '#FFFFFF' });
  }

  // ── Footer link ───────────────────────────────────────────────────────────
  function addFooterLink(parent, x, y, text, linkLabel) {
    addText(parent, { x, y, text, size: 13, style: 'Regular', color: '#64748B' });
    addText(parent, { x: x + text.length * 7 + 4, y, text: linkLabel, size: 13, style: 'Semi Bold', color: '#2563EB' });
  }

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 1: Login - PC (1440 x 900)
  // ════════════════════════════════════════════════════════════════════════
  const loginPC = makeFrame('Login - PC (1440px)', 1440, 900, 0, 0, '#FFFFFF');

  const LEFT_W = 720;
  buildBrandPanel(loginPC, LEFT_W, 900,
    'MCP 도구로 연결하는\n새로운 비즈니스 방식',
    '메시지 발송부터 파일 변환까지 — API 하나로 모든 MCP 도구를 연결하세요.',
    ['MCP 메시지 발송 도구', '에이전트 상태 진단', '파일 형식 변환'],
  );

  // Right form panel
  addRect(loginPC, { name: 'right-panel', x: LEFT_W, y: 0, w: 720, h: 900, fill: '#FFFFFF' });

  const FORM_X = LEFT_W + (720 - 400) / 2; // centered in right half
  let formY = 200;

  addText(loginPC, { x: FORM_X, y: formY, text: '로그인', size: 30, style: 'Bold', color: '#0F172A' });
  addText(loginPC, { x: FORM_X, y: formY + 42, text: 'Wisecan 계정에 로그인하세요', size: 14, style: 'Regular', color: '#64748B' });
  formY += 90;

  formY += addInputField(loginPC, FORM_X, formY, 400, '이메일 *', '예) user@wisecan.co.kr');
  formY += addInputField(loginPC, FORM_X, formY, 400, '비밀번호 *', '비밀번호를 입력하세요');

  // Forgot password
  addText(loginPC, { x: FORM_X + 290, y: formY - 52, text: '비밀번호 찾기', size: 12, style: 'Regular', color: '#2563EB' });

  formY += 8;
  // 정상 상태: 에러 배너 없이 바로 버튼
  addSubmitButton(loginPC, FORM_X, formY, 400, 44, '로그인');
  formY += 60;

  addFooterLink(loginPC, FORM_X + 60, formY, '계정이 없으신가요?', '회원가입');

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 1-B: Login - Error State - PC (1440 x 900)
  // ════════════════════════════════════════════════════════════════════════
  const loginErrorPC = makeFrame('Login - Error State - PC (1440px)', 1440, 900, 1540, 0, '#FFFFFF');

  buildBrandPanel(loginErrorPC, LEFT_W, 900,
    'MCP 도구로 연결하는\n새로운 비즈니스 방식',
    '메시지 발송부터 파일 변환까지 — API 하나로 모든 MCP 도구를 연결하세요.',
    ['MCP 메시지 발송 도구', '에이전트 상태 진단', '파일 형식 변환'],
  );
  addRect(loginErrorPC, { name: 'right-panel', x: LEFT_W, y: 0, w: 720, h: 900, fill: '#FFFFFF' });

  let errFormY = 200;
  addText(loginErrorPC, { x: FORM_X, y: errFormY, text: '로그인', size: 30, style: 'Bold', color: '#0F172A' });
  addText(loginErrorPC, { x: FORM_X, y: errFormY + 42, text: 'Wisecan 계정에 로그인하세요', size: 14, style: 'Regular', color: '#64748B' });
  errFormY += 90;

  errFormY += addInputField(loginErrorPC, FORM_X, errFormY, 400, '이메일 *', '예) user@wisecan.co.kr');
  errFormY += addInputField(loginErrorPC, FORM_X, errFormY, 400, '비밀번호 *', '비밀번호를 입력하세요');
  addText(loginErrorPC, { x: FORM_X + 290, y: errFormY - 52, text: '비밀번호 찾기', size: 12, style: 'Regular', color: '#2563EB' });

  errFormY += 8;
  errFormY += addErrorBanner(loginErrorPC, FORM_X, errFormY, 400, '이메일 또는 비밀번호가 일치하지 않습니다');

  addSubmitButton(loginErrorPC, FORM_X, errFormY, 400, 44, '로그인');
  errFormY += 60;
  addFooterLink(loginErrorPC, FORM_X + 60, errFormY, '계정이 없으신가요?', '회원가입');

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 2: Login - Mobile (375 x 812) — 정상 상태
  // ════════════════════════════════════════════════════════════════════════
  const loginMobile = makeFrame('Login - Mobile (375px)', 375, 812, 3080, 0, '#FFFFFF');

  addText(loginMobile, { x: 24, y: 56, text: 'Wisecan', size: 24, style: 'Bold', color: '#0F172A' });
  addText(loginMobile, { x: 24, y: 88, text: 'MCP 도구 연결 플랫폼', size: 13, style: 'Regular', color: '#64748B' });

  let mLoginY = 156;
  addText(loginMobile, { x: 24, y: mLoginY, text: '로그인', size: 24, style: 'Bold', color: '#0F172A' });
  mLoginY += 40;

  mLoginY += addInputField(loginMobile, 24, mLoginY, 327, '이메일 *', '예) user@wisecan.co.kr');
  mLoginY += addInputField(loginMobile, 24, mLoginY, 327, '비밀번호 *', '비밀번호를 입력하세요');
  addText(loginMobile, { x: 220, y: mLoginY - 52, text: '비밀번호 찾기', size: 12, style: 'Regular', color: '#2563EB' });

  mLoginY += 8;
  // 정상 상태: 에러 배너 없이 바로 버튼
  addSubmitButton(loginMobile, 24, mLoginY, 327, 48, '로그인');
  mLoginY += 64;
  addFooterLink(loginMobile, 60, mLoginY, '계정이 없으신가요?', '회원가입');

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 2-B: Login - Error State - Mobile (375 x 812)
  // ════════════════════════════════════════════════════════════════════════
  const loginErrorMobile = makeFrame('Login - Error State - Mobile (375px)', 375, 812, 3555, 0, '#FFFFFF');

  addText(loginErrorMobile, { x: 24, y: 56, text: 'Wisecan', size: 24, style: 'Bold', color: '#0F172A' });
  addText(loginErrorMobile, { x: 24, y: 88, text: 'MCP 도구 연결 플랫폼', size: 13, style: 'Regular', color: '#64748B' });

  let mErrY = 156;
  addText(loginErrorMobile, { x: 24, y: mErrY, text: '로그인', size: 24, style: 'Bold', color: '#0F172A' });
  mErrY += 40;

  mErrY += addInputField(loginErrorMobile, 24, mErrY, 327, '이메일 *', '예) user@wisecan.co.kr');
  mErrY += addInputField(loginErrorMobile, 24, mErrY, 327, '비밀번호 *', '비밀번호를 입력하세요');
  addText(loginErrorMobile, { x: 220, y: mErrY - 52, text: '비밀번호 찾기', size: 12, style: 'Regular', color: '#2563EB' });

  mErrY += 8;
  mErrY += addErrorBanner(loginErrorMobile, 24, mErrY, 327, '이메일 또는 비밀번호가 일치하지 않습니다');

  addSubmitButton(loginErrorMobile, 24, mErrY, 327, 48, '로그인');
  mErrY += 64;
  addFooterLink(loginErrorMobile, 60, mErrY, '계정이 없으신가요?', '회원가입');

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 3: Register - PC (1440 x 960)
  // ════════════════════════════════════════════════════════════════════════
  const regPC = makeFrame('Register - PC (1440px)', 1440, 960, 3180, 0, '#FFFFFF');

  buildBrandPanel(regPC, LEFT_W, 960,
    '지금 바로 시작하세요',
    '무료로 가입하고 Wisecan MCP 도구의 모든 기능을 체험하세요.',
    ['엔터프라이즈급 보안', '즉시 사용 가능한 API', '전담 기술 지원'],
  );

  addRect(regPC, { name: 'right-panel', x: LEFT_W, y: 0, w: 720, h: 960, fill: '#FFFFFF' });

  const REG_FORM_X = LEFT_W + (720 - 400) / 2;
  let regFormY = 160;

  addText(regPC, { x: REG_FORM_X, y: regFormY, text: '회원가입', size: 30, style: 'Bold', color: '#0F172A' });
  addText(regPC, { x: REG_FORM_X, y: regFormY + 42, text: 'Wisecan 계정을 만들어보세요', size: 14, style: 'Regular', color: '#64748B' });
  regFormY += 90;

  regFormY += addInputField(regPC, REG_FORM_X, regFormY, 400, '이름 *', '예) 김위즈캔');
  regFormY += addInputField(regPC, REG_FORM_X, regFormY, 400, '이메일 *', '예) user@wisecan.co.kr');
  regFormY += addInputField(regPC, REG_FORM_X, regFormY, 400, '비밀번호 *', '8자 이상, 영문+숫자 조합',
    { hint: '영문, 숫자를 포함하여 8자 이상 입력하세요' });
  regFormY += addInputField(regPC, REG_FORM_X, regFormY, 400, '비밀번호 확인 *', '비밀번호를 다시 입력하세요',
    { error: true, hint: '비밀번호가 일치하지 않습니다' });

  regFormY += 8;
  addSubmitButton(regPC, REG_FORM_X, regFormY, 400, 44, '회원가입');
  regFormY += 60;

  addFooterLink(regPC, REG_FORM_X + 50, regFormY, '이미 계정이 있으신가요?', '로그인');
  regFormY += 32;

  addText(regPC, {
    x: REG_FORM_X, y: regFormY,
    text: '가입하면 Wisecan의 이용약관 및 개인정보처리방침에 동의하는 것으로 간주됩니다.',
    size: 11, style: 'Regular', color: '#94A3B8', w: 400,
  });

  // ════════════════════════════════════════════════════════════════════════
  // FRAME 4: Register - Mobile (375 x 900)
  // ════════════════════════════════════════════════════════════════════════
  const regMobile = makeFrame('Register - Mobile (375px)', 375, 900, 4720, 0, '#FFFFFF');

  addText(regMobile, { x: 24, y: 48, text: 'Wisecan', size: 24, style: 'Bold', color: '#0F172A' });

  let mRegY = 120;
  addText(regMobile, { x: 24, y: mRegY, text: '회원가입', size: 24, style: 'Bold', color: '#0F172A' });
  mRegY += 40;

  mRegY += addInputField(regMobile, 24, mRegY, 327, '이름 *', '예) 김위즈캔');
  mRegY += addInputField(regMobile, 24, mRegY, 327, '이메일 *', '예) user@wisecan.co.kr');
  mRegY += addInputField(regMobile, 24, mRegY, 327, '비밀번호 *', '8자 이상, 영문+숫자 조합',
    { hint: '영문, 숫자를 포함하여 8자 이상' });
  mRegY += addInputField(regMobile, 24, mRegY, 327, '비밀번호 확인 *', '비밀번호를 다시 입력하세요');

  mRegY += 8;
  addSubmitButton(regMobile, 24, mRegY, 327, 48, '회원가입');
  mRegY += 64;
  addFooterLink(regMobile, 40, mRegY, '이미 계정이 있으신가요?', '로그인');

  // ── Done ──────────────────────────────────────────────────────────────────
  figma.viewport.scrollAndZoomIntoView([loginPC]);
  figma.closePlugin('Auth Wireframe 4개 프레임 생성 완료');
})();

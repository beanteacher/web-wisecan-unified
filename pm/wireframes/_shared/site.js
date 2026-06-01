/* ── WiseCan 공유 템플릿 — _shared/site.js ──────────────────────────── */

/* ── 공개 헤더 ──────────────────────────────────────────────────────── */
var SITE_HEADER_HTML = '<header class="fixed top-0 inset-x-0 z-40 bg-white/85 backdrop-blur-md border-b border-mist">' +
  '<div class="max-w-7xl mx-auto px-8 h-16 flex items-center justify-between">' +
    '<div class="flex items-center gap-10">' +
      '<a href="__BASE__01.index.html" class="text-base font-semibold tracking-tight text-ink">WiseCan<span class="text-psblue-main">.</span></a>' +
      '<nav class="hidden md:flex items-center gap-7 text-sm">' +
        '<a class="nav-link" href="__BASE__01.index.html#channels">서비스</a>' +
        '<a class="nav-link" href="__BASE__01.index.html#pricing">가격</a>' +
        '<a class="nav-link" href="__BASE__01.index.html#docs">문서</a>' +
        '<a class="nav-link" href="#">FAQ</a>' +
      '</nav>' +
    '</div>' +
    '<div class="flex items-center gap-2 text-sm">' +
      '<a class="cta-ghost px-3.5 py-1.5 rounded-full text-xs font-medium" href="__BASE__05.login.html">로그인</a>' +
      '<a class="cta-ghost px-3.5 py-1.5 rounded-full text-xs font-medium" href="__BASE__03.signup.html">회원가입</a>' +
    '</div>' +
  '</div>' +
'</header>';

/* ── 공개 푸터 ──────────────────────────────────────────────────────── */
function SITE_FOOTER_HTML(pageId, specRef) {
  return '<footer class="bg-cloud border-t border-mist">' +
    '<div class="max-w-7xl mx-auto px-8 py-16 grid grid-cols-2 md:grid-cols-5 gap-8 text-sm">' +
      '<div class="col-span-2">' +
        '<p class="text-base font-semibold text-ink">WiseCan<span class="text-psblue-main">.</span></p>' +
        '<p class="text-slate2 mt-3 max-w-xs leading-relaxed">한국 메시지 발송을 가장 쉽게. AI 자동화와 자연스럽게 통합되는 통합 메시징 서비스.</p>' +
        '<div class="mt-6 flex items-center gap-3">' +
          '<span class="inline-block w-2 h-2 rounded-full relative" style="background:#22c55e">' +
            '<span class="absolute inset-0 rounded-full animate-ping" style="background:#22c55e; opacity:.6"></span>' +
          '</span>' +
          '<span class="text-xs text-slate2">서비스 정상 · 모든 채널 운영 중</span>' +
        '</div>' +
      '</div>' +
      '<div>' +
        '<p class="text-graphite font-medium mb-3">서비스</p>' +
        '<ul class="space-y-2 text-slate2">' +
          '<li><a class="hover:text-ink" href="__BASE__01.index.html#pricing">가격</a></li>' +
          '<li><a class="hover:text-ink" href="__BASE__01.index.html#channels">발송 채널</a></li>' +
          '<li><a class="hover:text-ink" href="__BASE__02.try.html">체험하기</a></li>' +
        '</ul>' +
      '</div>' +
      '<div>' +
        '<p class="text-graphite font-medium mb-3">개발자</p>' +
        '<ul class="space-y-2 text-slate2">' +
          '<li><a class="hover:text-ink" href="#">Python 가이드</a></li>' +
          '<li><a class="hover:text-ink" href="#">셸 가이드</a></li>' +
          '<li><a class="hover:text-ink" href="#">AI 통합 가이드</a></li>' +
        '</ul>' +
      '</div>' +
      '<div>' +
        '<p class="text-graphite font-medium mb-3">회사</p>' +
        '<ul class="space-y-2 text-slate2">' +
          '<li><a class="hover:text-ink" href="#">이용약관</a></li>' +
          '<li><a class="hover:text-ink" href="#">개인정보</a></li>' +
          '<li><a class="hover:text-ink" href="#">1:1 문의</a></li>' +
        '</ul>' +
      '</div>' +
    '</div>' +
    '<div class="border-t border-mist">' +
      '<div class="max-w-7xl mx-auto px-8 py-5 text-xs text-stone flex justify-between">' +
        '<span>© 2026 WiseCan</span>' +
        '<span>' + (pageId || '') + (pageId && specRef ? ' · ' : '') + (specRef || '') + '</span>' +
      '</div>' +
    '</div>' +
  '</footer>';
}

/* ── 회원 콘솔 사이드바 ─────────────────────────────────────────────── */
/* 사용법: <div data-member-sidebar data-active="07.dashboard.html"></div>
   → data-active 와 일치하는 data-key 의 메뉴 항목에 .active 적용 */
var MEMBER_SIDEBAR_HTML = '<aside class="w-56 bg-cloud border-r border-mist p-4 text-sm min-h-screen flex-shrink-0">' +
  '<div class="mb-6 px-2">' +
    '<p class="text-base font-semibold text-ink">WiseCan<span class="text-psblue-main">.</span></p>' +
    '<div class="mt-2 flex items-center gap-2">' +
      '<span class="text-xs text-slate2">잔액</span>' +
      '<span class="text-xs font-mono font-semibold text-graphite">₩123,450</span>' +
    '</div>' +
  '</div>' +
  '<nav class="space-y-0.5">' +
    '<a href="__BASE__07.dashboard.html" data-key="07.dashboard.html" class="side-nav-item flex items-center gap-2 px-2 py-2 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">대시보드</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">발송</p>' +
    '<a href="__BASE__08.messages-channel.html" data-key="08.messages-channel.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">SMS</a>' +
    '<a href="__BASE__08.messages-channel.html" data-key="08.messages-channel.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">LMS</a>' +
    '<a href="__BASE__08.messages-channel.html" data-key="08.messages-channel.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">MMS</a>' +
    '<a href="__BASE__08.messages-channel.html" data-key="08.messages-channel.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">카카오 알림톡</a>' +
    '<a href="__BASE__08.messages-channel.html" data-key="08.messages-channel.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">RCS</a>' +
    '<a href="__BASE__09.messages-batch.html" data-key="09.messages-batch.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">CSV 일괄</a>' +
    '<a href="__BASE__10.messages-scheduled.html" data-key="10.messages-scheduled.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">예약 발송</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">이력</p>' +
    '<a href="__BASE__11.histories.html" data-key="11.histories.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">발송 이력</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">발신번호</p>' +
    '<a href="__BASE__13.callbacks.html" data-key="13.callbacks.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">발신번호 목록</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">API Key</p>' +
    '<a href="__BASE__15.keys.html" data-key="15.keys.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">API Key</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">템플릿</p>' +
    '<a href="__BASE__16.templates-kakao.html" data-key="16.templates-kakao.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">카카오 템플릿</a>' +
    '<a href="__BASE__17.templates-rcs.html" data-key="17.templates-rcs.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">RCS 브랜드</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">결제</p>' +
    '<a href="__BASE__18.billing-charge.html" data-key="18.billing-charge.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">충전</a>' +
    '<a href="__BASE__19.billing-auto-charge.html" data-key="19.billing-auto-charge.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">자동충전</a>' +
    '<a href="__BASE__20.billing-postpaid.html" data-key="20.billing-postpaid.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">후불</a>' +
    '<a href="__BASE__21.billing-refunds.html" data-key="21.billing-refunds.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">환불</a>' +
    '<a href="__BASE__22.billing-tax.html" data-key="22.billing-tax.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">현금영수증</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">지원</p>' +
    '<a href="__BASE__23.inquiries.html" data-key="23.inquiries.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">1:1 문의</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">내 계정</p>' +
    '<a href="__BASE__24.profile.html" data-key="24.profile.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">프로필</a>' +
    '<a href="__BASE__25.withdrawal.html" data-key="25.withdrawal.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">탈퇴</a>' +
    '<a href="__BASE__26.security.html" data-key="26.security.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">2차 인증</a>' +

    '<p class="px-2 pt-4 pb-1.5 text-sm font-bold text-ink">회사</p>' +
    '<a href="__BASE__27.company-members.html" data-key="27.company-members.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">하위 계정</a>' +
    '<a href="__BASE__28.company-master-roles.html" data-key="28.company-master-roles.html" class="side-nav-item flex items-center gap-2 px-2 py-1.5 rounded-lg text-slate2 hover:text-ink hover:bg-mist text-sm">마스터 권한</a>' +
  '</nav>' +
'</aside>';

/* ── 어드민 콘솔 사이드바 (light + 대메뉴 토글) ───────────────────────── */
/* 사용법: <div data-admin-sidebar data-active="08.admin-abuse.html"></div>
   → data-active 의 파일명과 일치하는 소메뉴 <a> 에 .active,
     그 부모 <details.aside-group> 에 .is-active + open 적용 */
var ADMIN_SIDEBAR_HTML = '<aside class="admin-aside w-56 flex-shrink-0 p-3">' +
  '<div data-admin-sidebar-inner>' +
    '<details class="aside-group">' +
      '<summary>운영 현황</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="02.admin-dashboard.html" href="02.admin-dashboard.html">운영 대시보드</a>' +
      '</div>' +
    '</details>' +
    '<details class="aside-group">' +
      '<summary>심사</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="03.admin-review-business.html" href="03.admin-review-business.html">사업자 가입</a>' +
        '<a class="aside-link" data-key="04.admin-review-callback.html" href="04.admin-review-callback.html">발신번호</a>' +
        '<a class="aside-link" data-key="05.admin-keys-review.html" href="05.admin-keys-review.html">키 발급·전환</a>' +
      '</div>' +
    '</details>' +
    '<details class="aside-group">' +
      '<summary>회원 관리</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="06.admin-members.html" href="06.admin-members.html">회원 통제</a>' +
        '<a class="aside-link" data-key="07.admin-callbacks.html" href="07.admin-callbacks.html">발신번호 통합관리</a>' +
      '</div>' +
    '</details>' +
    '<details class="aside-group">' +
      '<summary>발송 운영</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="08.admin-abuse.html" href="08.admin-abuse.html">이상 패턴·차단</a>' +
        '<a class="aside-link" data-key="09.admin-routing.html" href="09.admin-routing.html">카카오·RCS 라우팅</a>' +
      '</div>' +
    '</details>' +
    '<details class="aside-group">' +
      '<summary>재무</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="10.admin-finance.html" href="10.admin-finance.html">환불·캐시·후불</a>' +
      '</div>' +
    '</details>' +
    '<details class="aside-group">' +
      '<summary>도구</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="11.admin-tools.html" href="11.admin-tools.html">SDK / CLI / MCP</a>' +
      '</div>' +
    '</details>' +
    '<details class="aside-group">' +
      '<summary>CS</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="12.admin-cs.html" href="12.admin-cs.html">문의·챗봇·FAQ·공지</a>' +
      '</div>' +
    '</details>' +
    '<details class="aside-group">' +
      '<summary>시스템</summary>' +
      '<div class="aside-group-items">' +
        '<a class="aside-link" data-key="13.admin-operators.html" href="13.admin-operators.html">운영자 계정</a>' +
        '<a class="aside-link" data-key="14.admin-policies.html" href="14.admin-policies.html">테스트 한도 정책</a>' +
        '<a class="aside-link" data-key="15.admin-system.html" href="15.admin-system.html">인코딩·약관·감사 로그</a>' +
        '<a class="aside-link" data-key="16.admin-audit.html" href="16.admin-audit.html">내부 감사</a>' +
      '</div>' +
    '</details>' +
  '</div>' +
'</aside>';

/* ── DOM 주입 ────────────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', function() {
  var sel = function(s) { return document.querySelector(s); };

  // 페이지 위치별 BASE — admin/ 폴더 안이면 한 단계 위, 아니면 같은 폴더
  var BASE = '';
  try {
    if (location.pathname.indexOf('/admin/') !== -1) BASE = '../';
  } catch (e) {}
  var withBase = function(html) { return html.split('__BASE__').join(BASE); };

  var h = sel('[data-site-header]');
  if (h) { h.outerHTML = withBase(SITE_HEADER_HTML); }

  var ms = sel('[data-member-sidebar]');
  if (ms) {
    var memberKey = ms.getAttribute('data-active') || '';
    var tmpM = document.createElement('div');
    tmpM.innerHTML = withBase(MEMBER_SIDEBAR_HTML);
    var newMs = tmpM.firstChild;
    if (memberKey) {
      // 같은 data-key 가 여러 개 있어도 첫 번째 항목만 활성 표시
      // (예: 메시지 채널 SMS/LMS/MMS/카카오/RCS 가 모두 08.messages-channel.html 인 경우 SMS 만 강조)
      var memberLinks = newMs.querySelectorAll('a.side-nav-item[data-key="' + memberKey + '"]');
      if (memberLinks.length > 0) {
        memberLinks[0].classList.add('active');
      }
    }
    ms.parentNode.replaceChild(newMs, ms);
  }

  var as = sel('[data-admin-sidebar]');
  if (as) {
    var activeKey = as.getAttribute('data-active') || '';
    var tmp = document.createElement('div');
    tmp.innerHTML = ADMIN_SIDEBAR_HTML;
    var newAside = tmp.firstChild;
    if (activeKey) {
      var activeLink = newAside.querySelector('a.aside-link[data-key="' + activeKey + '"]');
      if (activeLink) {
        activeLink.classList.add('active');
        var parentDetails = activeLink.closest('details.aside-group');
        if (parentDetails) {
          parentDetails.classList.add('is-active');
          parentDetails.setAttribute('open', '');
        }
      }
    }
    as.parentNode.replaceChild(newAside, as);
  }

  var f = sel('[data-site-footer]');
  if (f) {
    var pageId  = f.getAttribute('data-page-id')  || '';
    var specRef = f.getAttribute('data-spec-ref') || '';
    f.outerHTML = withBase(SITE_FOOTER_HTML(pageId, specRef));
  }
});

/**
 * wsc docs / wsc snippet — 문서 및 코드 스니펫 (무인증, RQ-CLI-401~405)
 *
 *   wsc docs [send|auth|history|key|callback]
 *   wsc snippet sms|lms|mms|kakao|rcs|bulk
 */

'use strict';

const DOCS = {
  send: `
[발송 명령 가이드]

단건 발송:
  wsc send sms   -c <발신번호> -d <수신번호> -t <본문>
  wsc send lms   -c <발신번호> -d <수신번호> -t <본문> -s <제목>
  wsc send mms   -c <발신번호> -d <수신번호> -t <본문> -s <제목>
  wsc send kakao -c <발신번호> -d <수신번호> -t <본문> --sender-key <키> --template-code <코드>
  wsc send rcs   -c <발신번호> -d <수신번호> -t <본문>

일괄 발송:
  wsc send bulk -c <발신번호> --destaddrs <번호1,번호2,...> -t <본문> --channel SMS

예약:   --schedule 2026-07-01T09:00:00
광고:   --ad
포맷:   -f json|table|csv
`,
  auth: `
[인증 명령 가이드]

  wsc auth login  --key <API_KEY> [--url <서버주소>]
  wsc auth status
  wsc auth logout

환경변수:
  WSC_API_KEY=<키>    wsc history list
  WSC_BASE_URL=<URL>  wsc send sms ...
`,
  history: `
[이력 조회 가이드]

  wsc history list
  wsc history list --channel SMS --from 2026-07-01 --to 2026-07-31
  wsc history list --page 1 --size 50
  wsc history get <send_id>
  wsc history list -f csv > history.csv
`,
  key: `
[API Key 관리 가이드]

  wsc key list
  wsc key create --name "prod-key" --type PRODUCTION
  wsc key revoke <id>
  wsc key rotate <id>
  wsc key scopes
`,
  callback: `
[발신번호 관리 가이드]

  wsc callback list
  wsc callback get <id>
  wsc callback register -n 01012345678 --type SELF_MOBILE --purpose "고객 안내"
  wsc callback delete <id>

등록 유형:
  SELF_MOBILE    본인 명의 휴대폰 (즉시 승인)
  SELF_LANDLINE  본인 명의 일반 유선
  EMPLOYEE       임직원 명의 (서류 심사)
  CORP_REP       법인 대표번호 (서류 심사)
`,
};

const SNIPPETS = {
  sms: `wsc send sms \\
  --callback 01012345678 \\
  --destaddr 01098765432 \\
  --text "안녕하세요. 발송 테스트입니다."
`,
  lms: `wsc send lms \\
  --callback 01012345678 \\
  --destaddr 01098765432 \\
  --subject "공지사항" \\
  --text "장문 메시지 내용입니다."
`,
  mms: `wsc send mms \\
  --callback 01012345678 \\
  --destaddr 01098765432 \\
  --subject "이미지 발송" \\
  --text "MMS 메시지 본문입니다."
`,
  kakao: `wsc send kakao \\
  --callback 01012345678 \\
  --destaddr 01098765432 \\
  --sender-key YOUR_SENDER_KEY \\
  --template-code YOUR_TEMPLATE_CODE \\
  --text "#{이름}님 주문이 완료되었습니다."
`,
  rcs: `wsc send rcs \\
  --callback 01012345678 \\
  --destaddr 01098765432 \\
  --subject "RCS 제목" \\
  --text "RCS 메시지 본문입니다."
`,
  bulk: `wsc send bulk \\
  --callback 01012345678 \\
  --destaddrs "01011110000,01022220000,01033330000" \\
  --channel SMS \\
  --text "대량 발송 메시지입니다."
`,
};

function docsCmd(topic, _flags) {
  if (!topic) {
    process.stdout.write(Object.values(DOCS).join('\n') + '\n');
    return;
  }
  const content = DOCS[topic.toLowerCase()];
  if (!content) {
    process.stderr.write('알 수 없는 주제: ' + topic + '\n');
    process.stderr.write('사용 가능: ' + Object.keys(DOCS).join(', ') + '\n');
    process.exit(4);
  }
  process.stdout.write(content + '\n');
}

function snippetCmd(channel, _flags) {
  if (!channel) {
    process.stderr.write('채널을 지정하세요. (sms|lms|mms|kakao|rcs|bulk)\n');
    process.exit(4);
  }
  const content = SNIPPETS[channel.toLowerCase()];
  if (!content) {
    process.stderr.write('알 수 없는 채널: ' + channel + '\n');
    process.stderr.write('사용 가능: ' + Object.keys(SNIPPETS).join(', ') + '\n');
    process.exit(4);
  }
  process.stdout.write(content + '\n');
}

module.exports = { docsCmd, snippetCmd };

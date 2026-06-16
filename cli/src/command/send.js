/**
 * wsc send — 발송 명령 (RQ-CLI-101~109)
 *
 *   wsc send sms   -c <번호> -d <번호> -t <본문> [--ad] [--schedule <ISO>] [-f json|table|csv]
 *   wsc send lms   ... [-s <제목>]
 *   wsc send mms   ... [-s <제목>]
 *   wsc send kakao ... --sender-key <키> --template-code <코드>
 *   wsc send rcs   ... [-s <제목>]
 *   wsc send bulk  -c <번호> --destaddrs <번호,...> -t <본문> --channel <채널> [옵션]
 */

'use strict';

const client = require('../api/client');
const { print, fatal } = require('../util/output');
const { getFlag, hasFlag } = require('../util/args');
const store = require('../config/store');
const { checkForUpdate } = require('../util/updater');

function requireAuth() {
  if (!store.getApiKey()) {
    fatal('API Key 가 등록되지 않았습니다. wsc auth login --key <KEY> 를 먼저 실행하세요.', 1);
  }
}

async function execSingle(body, format) {
  requireAuth();
  let res;
  try {
    res = await client.post('/dispatch/send', body);
  } catch (err) {
    fatal('네트워크 오류: ' + err.message, 5);
  }
  const code = client.exitCodeFromStatus(res.status);
  if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
  print(res.body && res.body.data ? res.body.data : res.body, format);
  checkForUpdate().catch(() => {});
}

async function sendCmd(sub, _rest, flags) {
  const format = getFlag(flags, ['format', 'f'], 'table');

  switch (sub) {
    case 'sms': {
      const callback = getFlag(flags, ['callback', 'c'], null);
      const destaddr = getFlag(flags, ['destaddr', 'd'], null);
      const text     = getFlag(flags, ['text', 't'], null);
      if (!callback || !destaddr || !text) fatal('-c, -d, -t 는 필수입니다.', 4);
      await execSingle({
        callbackNumber: callback,
        recipientNumber: destaddr,
        channel: 'SMS',
        messageBody: text,
        isAdvertisement: hasFlag(flags, ['ad']),
        scheduledAt: getFlag(flags, ['schedule'], null),
      }, format);
      break;
    }
    case 'lms':
    case 'mms':
    case 'rcs': {
      const callback = getFlag(flags, ['callback', 'c'], null);
      const destaddr = getFlag(flags, ['destaddr', 'd'], null);
      const text     = getFlag(flags, ['text', 't'], null);
      if (!callback || !destaddr || !text) fatal('-c, -d, -t 는 필수입니다.', 4);
      await execSingle({
        callbackNumber: callback,
        recipientNumber: destaddr,
        channel: sub.toUpperCase(),
        subject: getFlag(flags, ['subject', 's'], null),
        messageBody: text,
        isAdvertisement: hasFlag(flags, ['ad']),
        scheduledAt: getFlag(flags, ['schedule'], null),
      }, format);
      break;
    }
    case 'kakao': {
      const callback     = getFlag(flags, ['callback', 'c'], null);
      const destaddr     = getFlag(flags, ['destaddr', 'd'], null);
      const text         = getFlag(flags, ['text', 't'], null);
      const senderKey    = getFlag(flags, ['sender-key'], null);
      const templateCode = getFlag(flags, ['template-code'], null);
      if (!callback || !destaddr || !text || !senderKey || !templateCode) {
        fatal('-c, -d, -t, --sender-key, --template-code 는 필수입니다.', 4);
      }
      await execSingle({
        callbackNumber: callback,
        recipientNumber: destaddr,
        channel: 'KAKAO',
        messageBody: text,
        senderKey,
        templateCode,
        isAdvertisement: false,
        scheduledAt: getFlag(flags, ['schedule'], null),
      }, format);
      break;
    }
    case 'bulk': {
      requireAuth();
      const callback  = getFlag(flags, ['callback', 'c'], null);
      const destaddrs = getFlag(flags, ['destaddrs'], null);
      const text      = getFlag(flags, ['text', 't'], null);
      const channel   = getFlag(flags, ['channel'], null);
      if (!callback || !destaddrs || !text || !channel) {
        fatal('-c, --destaddrs, -t, --channel 는 필수입니다.', 4);
      }
      const recipientNumbers = destaddrs.split(',').map((s) => s.trim()).filter(Boolean);
      if (recipientNumbers.length === 0) fatal('수신번호 목록이 비어 있습니다.', 4);

      let res;
      try {
        res = await client.post('/dispatch/send/bulk', {
          callbackNumber: callback,
          recipientNumbers,
          channel: channel.toUpperCase(),
          subject: getFlag(flags, ['subject', 's'], null),
          messageBody: text,
          isAdvertisement: hasFlag(flags, ['ad']),
          scheduledAt: getFlag(flags, ['schedule'], null),
        });
      } catch (err) {
        fatal('네트워크 오류: ' + err.message, 5);
      }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      print(res.body && res.body.data ? res.body.data : res.body, format);
      checkForUpdate().catch(() => {});
      break;
    }
    default:
      process.stderr.write('알 수 없는 send 하위 명령: ' + sub + '\n');
      process.stderr.write('사용 가능: sms, lms, mms, kakao, rcs, bulk\n');
      process.exit(4);
  }
}

module.exports = sendCmd;

/**
 * wsc history — 발송 이력 명령 (RQ-CLI 이력 조회)
 *
 *   wsc history list [--from <날짜>] [--to <날짜>] [--channel <채널>]
 *                    [--callback <번호>] [--page N] [--size N] [-f 형식]
 *   wsc history get  <send_id> [-f 형식]
 */

'use strict';

const client = require('../api/client');
const { print, fatal } = require('../util/output');
const { getFlag } = require('../util/args');
const store = require('../config/store');
const { checkForUpdate } = require('../util/updater');

function requireAuth() {
  if (!store.getApiKey()) {
    fatal('API Key 가 등록되지 않았습니다. wsc auth login --key <KEY> 를 먼저 실행하세요.', 1);
  }
}

async function historyCmd(sub, rest, flags) {
  const format = getFlag(flags, ['format', 'f'], 'table');

  switch (sub) {
    case 'list': {
      requireAuth();
      const params = new URLSearchParams();
      const from     = getFlag(flags, ['from'], null);
      const to       = getFlag(flags, ['to'], null);
      const channel  = getFlag(flags, ['channel'], null);
      const callback = getFlag(flags, ['callback'], null);
      const page     = getFlag(flags, ['page'], '0');
      const size     = getFlag(flags, ['size'], '20');
      if (from)     params.set('from', from);
      if (to)       params.set('to', to);
      if (channel)  params.set('channel', channel.toUpperCase());
      if (callback) params.set('callbackNumber', callback);
      params.set('page', page);
      params.set('size', size);

      let res;
      try {
        res = await client.get('/dispatch/send?' + params.toString());
      } catch (err) {
        fatal('네트워크 오류: ' + err.message, 5);
      }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);

      const body = res.body;
      const rows = (body && body.data && Array.isArray(body.data.content))
        ? body.data.content
        : (Array.isArray(body) ? body : [body]);
      print(rows, format);
      checkForUpdate().catch(() => {});
      break;
    }
    case 'get': {
      requireAuth();
      const sendId = rest[0] || getFlag(flags, ['id'], null);
      if (!sendId) fatal('send_id 가 필요합니다. (wsc history get <send_id>)', 4);

      let res;
      try {
        res = await client.get('/dispatch/send/' + sendId);
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
      process.stderr.write('알 수 없는 history 하위 명령: ' + sub + '\n');
      process.stderr.write('사용 가능: list, get\n');
      process.exit(4);
  }
}

module.exports = historyCmd;

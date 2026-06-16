/**
 * wsc callback — 발신번호 관리
 *
 *   wsc callback list
 *   wsc callback get <id>
 *   wsc callback register -n <번호> --type <타입> [--purpose <설명>]
 *   wsc callback delete <id>
 */

'use strict';

const client = require('../api/client');
const { print, fatal } = require('../util/output');
const { getFlag } = require('../util/args');
const store = require('../config/store');

function requireAuth() {
  if (!store.getApiKey()) {
    fatal('API Key 가 등록되지 않았습니다. wsc auth login --key <KEY> 를 먼저 실행하세요.', 1);
  }
}

async function callbackCmd(sub, rest, flags) {
  const format = getFlag(flags, ['format', 'f'], 'table');

  switch (sub) {
    case 'list': {
      requireAuth();
      let res;
      try { res = await client.get('/api/v1/callbacks'); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      const data = res.body && res.body.data ? res.body.data : res.body;
      print(Array.isArray(data) ? data : [data], format);
      break;
    }
    case 'get': {
      requireAuth();
      const id = rest[0] || getFlag(flags, ['id'], null);
      if (!id) fatal('id 가 필요합니다. (wsc callback get <id>)', 4);
      let res;
      try { res = await client.get('/api/v1/callbacks/' + id); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      print(res.body && res.body.data ? res.body.data : res.body, format);
      break;
    }
    case 'register': {
      requireAuth();
      const number = getFlag(flags, ['number', 'n'], null);
      const type   = getFlag(flags, ['type'], null);
      if (!number || !type) fatal('-n <번호> 와 --type <타입> 은 필수입니다.', 4);
      const body = {
        phoneNumber: number,
        registerType: type.toUpperCase(),
        purposeDescription: getFlag(flags, ['purpose'], '') || '',
      };
      let res;
      try { res = await client.post('/api/v1/callbacks', body); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      print(res.body && res.body.data ? res.body.data : res.body, format);
      break;
    }
    case 'delete': {
      requireAuth();
      const id = rest[0] || getFlag(flags, ['id'], null);
      if (!id) fatal('id 가 필요합니다. (wsc callback delete <id>)', 4);
      let res;
      try { res = await client.del('/api/v1/callbacks/' + id); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      process.stdout.write('발신번호 ' + id + ' 삭제 완료.\n');
      break;
    }
    default:
      process.stderr.write('알 수 없는 callback 하위 명령: ' + sub + '\n');
      process.stderr.write('사용 가능: list, get, register, delete\n');
      process.exit(4);
  }
}

module.exports = callbackCmd;

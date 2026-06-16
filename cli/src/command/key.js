/**
 * wsc key — API Key 관리 (RQ-CLI-004~007)
 *
 *   wsc key list
 *   wsc key create --name <이름> [--type TEST|PRODUCTION] [--scopes s1,s2] [--daily-limit N] [--callbacks 번호,...]
 *   wsc key revoke <id>
 *   wsc key rotate <id>
 *   wsc key scopes
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

async function keyCmd(sub, rest, flags) {
  const format = getFlag(flags, ['format', 'f'], 'table');

  switch (sub) {
    case 'list': {
      requireAuth();
      let res;
      try { res = await client.get('/api/v1/api-keys'); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      const data = res.body && res.body.data ? res.body.data : res.body;
      print(Array.isArray(data) ? data : [data], format);
      break;
    }
    case 'create': {
      requireAuth();
      const name = getFlag(flags, ['name', 'n'], null);
      if (!name) fatal('--name <이름> 은 필수입니다.', 4);
      const body = { keyName: name, keyType: (getFlag(flags, ['type'], 'TEST')).toUpperCase() };
      const scopes = getFlag(flags, ['scopes'], null);
      if (scopes) body.scopes = scopes.split(',').map((s) => s.trim());
      const dailyLimit = getFlag(flags, ['daily-limit'], null);
      if (dailyLimit) body.dailyLimit = parseInt(dailyLimit, 10);
      const callbacks = getFlag(flags, ['callbacks'], null);
      if (callbacks) body.allowedCallbacks = callbacks.split(',').map((s) => s.trim());

      let res;
      try { res = await client.post('/api/v1/api-keys', body); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      print(res.body && res.body.data ? res.body.data : res.body, format);
      break;
    }
    case 'revoke': {
      requireAuth();
      const id = rest[0] || getFlag(flags, ['id'], null);
      if (!id) fatal('key id 가 필요합니다. (wsc key revoke <id>)', 4);
      let res;
      try { res = await client.patch('/api/v1/api-keys/' + id + '/revoke', {}); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      process.stdout.write('키 ' + id + ' 폐기 완료.\n');
      break;
    }
    case 'rotate': {
      requireAuth();
      const id = rest[0] || getFlag(flags, ['id'], null);
      if (!id) fatal('key id 가 필요합니다. (wsc key rotate <id>)', 4);
      let res;
      try { res = await client.post('/api/v1/api-keys/' + id + '/rotate', {}); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      print(res.body && res.body.data ? res.body.data : res.body, format);
      break;
    }
    case 'scopes': {
      let res;
      try { res = await client.get('/api/v1/api-keys/scopes/catalog'); }
      catch (err) { fatal('네트워크 오류: ' + err.message, 5); }
      const code = client.exitCodeFromStatus(res.status);
      if (code !== 0) fatal((res.body && res.body.message) || JSON.stringify(res.body), code);
      const data = res.body && res.body.data ? res.body.data : res.body;
      print(data && data.scopes ? data.scopes : data, format);
      break;
    }
    default:
      process.stderr.write('알 수 없는 key 하위 명령: ' + sub + '\n');
      process.stderr.write('사용 가능: list, create, revoke, rotate, scopes\n');
      process.exit(4);
  }
}

module.exports = keyCmd;

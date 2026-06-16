/**
 * wsc auth — 인증 명령 (RQ-CLI-001~003)
 *
 *   wsc auth login  --key <API_KEY> [--url <BASE_URL>]
 *   wsc auth logout
 *   wsc auth status
 */

'use strict';

const store = require('../config/store');
const { fatal } = require('../util/output');
const { getFlag } = require('../util/args');

/**
 * @param {string} sub  — login | logout | status
 * @param {string[]} _rest
 * @param {Map<string,string|true>} flags
 */
async function authCmd(sub, _rest, flags) {
  switch (sub) {
    case 'login': {
      const key = getFlag(flags, ['key', 'k'], null);
      if (!key) fatal('--key <API_KEY> 는 필수입니다.', 4);
      const url = getFlag(flags, ['url', 'u'], null);
      store.saveApiKey(key);
      if (url) store.saveBaseUrl(url);
      process.stdout.write('로그인 완료. 설정 파일: ' + store.getConfigPath() + '\n');
      break;
    }
    case 'logout':
      store.clear();
      process.stdout.write('로그아웃 완료.\n');
      break;

    case 'status': {
      const key = store.getApiKey();
      const url = store.getBaseUrl();
      if (!key) {
        process.stdout.write('로그인 안 됨 (wsc auth login --key <API_KEY> 로 등록)\n');
        process.exit(1);
      }
      const masked = key.length > 8
        ? key.slice(0, 4) + '****' + key.slice(-4)
        : '****';
      process.stdout.write('로그인 상태\n');
      process.stdout.write('  API Key : ' + masked + '\n');
      process.stdout.write('  서버    : ' + url + '\n');
      process.stdout.write('  설정    : ' + store.getConfigPath() + '\n');
      break;
    }
    default:
      process.stderr.write('알 수 없는 auth 하위 명령: ' + sub + '\n');
      process.stderr.write('사용 가능: login, logout, status\n');
      process.exit(4);
  }
}

module.exports = authCmd;

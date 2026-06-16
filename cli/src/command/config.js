/**
 * wsc config — CLI 클라이언트 설정 관리
 *
 *   wsc config set --url <BASE_URL>
 *   wsc config get
 *   wsc config path
 */

'use strict';

const store = require('../config/store');
const { getFlag } = require('../util/args');

async function configCmd(sub, _rest, flags) {
  switch (sub) {
    case 'set': {
      const url = getFlag(flags, ['url', 'u'], null);
      if (!url) {
        process.stderr.write('변경할 항목을 지정하세요. (--url <BASE_URL>)\n');
        process.exit(4);
      }
      store.saveBaseUrl(url);
      process.stdout.write('서버 주소 설정: ' + url + '\n');
      break;
    }
    case 'get': {
      const baseUrl = store.getBaseUrl();
      const apiKey  = store.getApiKey();
      const masked  = apiKey
        ? (apiKey.length > 8 ? apiKey.slice(0, 4) + '****' + apiKey.slice(-4) : '****')
        : '(미설정)';
      process.stdout.write('서버 주소  : ' + baseUrl + '\n');
      process.stdout.write('API Key    : ' + masked + '\n');
      process.stdout.write('설정 파일  : ' + store.getConfigPath() + '\n');
      break;
    }
    case 'path':
      process.stdout.write(store.getConfigPath() + '\n');
      break;
    default:
      process.stderr.write('알 수 없는 config 하위 명령: ' + sub + '\n');
      process.stderr.write('사용 가능: set, get, path\n');
      process.exit(4);
  }
}

module.exports = configCmd;

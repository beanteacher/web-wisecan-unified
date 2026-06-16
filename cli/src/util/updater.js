/**
 * 자동 업데이트 메커니즘 — GitHub Releases API 에서 최신 버전을 확인하고
 * 현재 버전과 비교하여 업그레이드 안내를 출력한다.
 *
 * RQ-CLI-301~303
 *
 * 구현 방식:
 *   1) https://api.github.com/repos/beanteacher/web-wisecan-unified/releases/latest 조회
 *   2) tag_name 과 package.json version 비교 (semver 단순 문자열 비교)
 *   3) 업데이트 있으면 stderr 에 안내 출력 (발송·조회 명령 실행 후 비동기)
 *   4) 환경변수 WSC_NO_UPDATE_CHECK=1 이면 건너뜀
 */

'use strict';

const https = require('https');
const pkg = require('../../package.json');

const RELEASES_API = 'https://api.github.com/repos/beanteacher/web-wisecan-unified/releases/latest';
const INSTALL_HINT = 'https://github.com/beanteacher/web-wisecan-unified/releases/latest';

/**
 * 버전 문자열 a 가 b 보다 최신인지 판단 (semver major.minor.patch).
 * @param {string} a
 * @param {string} b
 * @returns {boolean}
 */
function isNewer(a, b) {
  const parse = (v) => (v || '').replace(/^v/, '').split('.').map(Number);
  const [aMaj, aMin, aPat] = parse(a);
  const [bMaj, bMin, bPat] = parse(b);
  if (aMaj !== bMaj) return aMaj > bMaj;
  if (aMin !== bMin) return aMin > bMin;
  return aPat > bPat;
}

/**
 * 최신 릴리즈 tag_name 을 GitHub API 로 조회. 실패 시 null 반환.
 * @returns {Promise<string|null>}
 */
function fetchLatestVersion() {
  return new Promise((resolve) => {
    const req = https.request(
      RELEASES_API,
      {
        headers: {
          'User-Agent': 'wsc-cli/' + pkg.version,
          'Accept': 'application/vnd.github+json',
        },
        timeout: 3000,
      },
      (res) => {
        let data = '';
        res.on('data', (c) => { data += c; });
        res.on('end', () => {
          try {
            const json = JSON.parse(data);
            resolve(json.tag_name || null);
          } catch (_) {
            resolve(null);
          }
        });
      }
    );
    req.on('error', () => resolve(null));
    req.on('timeout', () => { req.destroy(); resolve(null); });
    req.end();
  });
}

/**
 * 백그라운드에서 업데이트 확인 후 필요시 stderr 에 안내 출력.
 * 명령 실행 완료 후 비동기로 호출하여 UX 를 방해하지 않는다.
 */
async function checkForUpdate() {
  if (process.env.WSC_NO_UPDATE_CHECK === '1') return;

  try {
    const latest = await fetchLatestVersion();
    if (latest && isNewer(latest, pkg.version)) {
      process.stderr.write(
        '\n──────────────────────────────────────────\n' +
        '  wsc 업데이트가 있습니다: v' + pkg.version + ' → ' + latest + '\n' +
        '  다운로드: ' + INSTALL_HINT + '\n' +
        '──────────────────────────────────────────\n'
      );
    }
  } catch (_) {
    // 네트워크 오류 시 조용히 무시
  }
}

module.exports = { checkForUpdate, isNewer };

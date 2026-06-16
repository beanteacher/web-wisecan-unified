/**
 * 경량 CLI 인수 파서 — Node.js 내장 모듈만 사용 (외부 패키지 불필요).
 *
 * 지원 형식:
 *   --flag            boolean true
 *   --key value
 *   --key=value
 *   positional        플래그 아닌 값
 *
 * 반환: { flags: Map<string,string|true>, positional: string[] }
 */

'use strict';

/**
 * process.argv 슬라이스를 파싱한다.
 * @param {string[]} argv  — process.argv.slice(2) 등
 * @returns {{ flags: Map<string, string|true>, positional: string[] }}
 */
function parseArgs(argv) {
  const flags = new Map();
  const positional = [];
  let i = 0;

  while (i < argv.length) {
    const arg = argv[i];

    if (arg.startsWith('--')) {
      const eqIdx = arg.indexOf('=');
      if (eqIdx !== -1) {
        // --key=value
        const key = arg.slice(2, eqIdx);
        const val = arg.slice(eqIdx + 1);
        flags.set(key, val);
      } else {
        const key = arg.slice(2);
        const next = argv[i + 1];
        if (next !== undefined && !next.startsWith('-')) {
          flags.set(key, next);
          i++;
        } else {
          flags.set(key, true);
        }
      }
    } else if (arg.startsWith('-') && arg.length === 2) {
      // -k value (단축 플래그)
      const key = arg.slice(1);
      const next = argv[i + 1];
      if (next !== undefined && !next.startsWith('-')) {
        flags.set(key, next);
        i++;
      } else {
        flags.set(key, true);
      }
    } else {
      positional.push(arg);
    }

    i++;
  }

  return { flags, positional };
}

/**
 * 플래그 값을 문자열로 가져온다. 없으면 defaultValue 반환.
 * @param {Map} flags
 * @param {string[]} names  — ['key', 'k'] 등 긴 이름과 단축 이름 순서
 * @param {string|null} defaultValue
 * @returns {string|null}
 */
function getFlag(flags, names, defaultValue) {
  for (const name of names) {
    if (flags.has(name)) {
      const v = flags.get(name);
      return v === true ? null : String(v);
    }
  }
  return defaultValue !== undefined ? defaultValue : null;
}

/**
 * boolean 플래그 존재 여부 확인.
 * @param {Map} flags
 * @param {string[]} names
 * @returns {boolean}
 */
function hasFlag(flags, names) {
  return names.some((n) => flags.has(n));
}

module.exports = { parseArgs, getFlag, hasFlag };

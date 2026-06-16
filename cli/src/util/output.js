/**
 * 출력 포맷터 — JSON / 표 / CSV 세 가지 형식 지원 (02 §7.2).
 * RQ-CLI-201~213
 */

'use strict';

/**
 * 단일 객체 또는 배열을 지정 포맷으로 stdout 출력.
 * @param {object|object[]} data
 * @param {'json'|'table'|'csv'} format
 */
function print(data, format) {
  const fmt = (format || 'table').toLowerCase();

  if (fmt === 'json') {
    process.stdout.write(JSON.stringify(data, null, 2) + '\n');
    return;
  }

  const rows = Array.isArray(data) ? data : [data];
  if (rows.length === 0) {
    process.stdout.write('(결과 없음)\n');
    return;
  }

  if (fmt === 'csv') {
    printCsv(rows);
  } else {
    printTable(rows);
  }
}

/**
 * 배열을 표 형식으로 출력.
 * @param {object[]} rows
 */
function printTable(rows) {
  const keys = Object.keys(rows[0]);
  const widths = keys.map((k) => {
    const maxVal = Math.max(...rows.map((r) => String(r[k] ?? '').length));
    return Math.max(k.length, maxVal);
  });

  const sep = '+' + widths.map((w) => '-'.repeat(w + 2)).join('+') + '+';
  const header = '|' + keys.map((k, i) => ' ' + k.padEnd(widths[i]) + ' ').join('|') + '|';

  process.stdout.write(sep + '\n');
  process.stdout.write(header + '\n');
  process.stdout.write(sep + '\n');

  for (const row of rows) {
    const line = '|' + keys.map((k, i) => ' ' + String(row[k] ?? '').padEnd(widths[i]) + ' ').join('|') + '|';
    process.stdout.write(line + '\n');
  }
  process.stdout.write(sep + '\n');
}

/**
 * 배열을 CSV 형식으로 출력.
 * @param {object[]} rows
 */
function printCsv(rows) {
  const keys = Object.keys(rows[0]);
  process.stdout.write(keys.join(',') + '\n');
  for (const row of rows) {
    const line = keys.map((k) => {
      const val = String(row[k] ?? '');
      return val.includes(',') || val.includes('"') || val.includes('\n')
        ? '"' + val.replace(/"/g, '""') + '"'
        : val;
    }).join(',');
    process.stdout.write(line + '\n');
  }
}

/**
 * 에러 메시지를 stderr 에 출력하고 지정 코드로 종료.
 * @param {string} message
 * @param {number} [code=1]
 */
function fatal(message, code) {
  process.stderr.write('오류: ' + message + '\n');
  process.exit(code != null ? code : 1);
}

/**
 * 경고 메시지를 stderr 에 출력 (종료하지 않음).
 * @param {string} message
 */
function warn(message) {
  process.stderr.write('경고: ' + message + '\n');
}

module.exports = { print, fatal, warn };

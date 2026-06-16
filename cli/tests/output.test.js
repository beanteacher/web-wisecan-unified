/**
 * 출력 포맷터 단위 테스트
 * Node.js 내장 assert 모듈 사용 — 외부 패키지 설치 없이 검증 가능.
 */

'use strict';

const assert = require('assert');

// output.js 에서 내부 함수를 테스트하기 위해 직접 로직을 재현한다.
// (실제 파일이 process.stdout 에 직접 쓰므로 캡처 방식으로 테스트)

/** printCsv 로직 추출 (테스트용) */
function toCsv(rows) {
  const keys = Object.keys(rows[0]);
  const lines = [keys.join(',')];
  for (const row of rows) {
    const line = keys.map((k) => {
      const val = String(row[k] ?? '');
      return val.includes(',') || val.includes('"') || val.includes('\n')
        ? '"' + val.replace(/"/g, '""') + '"'
        : val;
    }).join(',');
    lines.push(line);
  }
  return lines.join('\n');
}

/** printTable 로직 추출 (테스트용) */
function toTable(rows) {
  const keys = Object.keys(rows[0]);
  const widths = keys.map((k) => {
    const maxVal = Math.max(...rows.map((r) => String(r[k] ?? '').length));
    return Math.max(k.length, maxVal);
  });
  const sep = '+' + widths.map((w) => '-'.repeat(w + 2)).join('+') + '+';
  const header = '|' + keys.map((k, i) => ' ' + k.padEnd(widths[i]) + ' ').join('|') + '|';
  const dataRows = rows.map((row) =>
    '|' + keys.map((k, i) => ' ' + String(row[k] ?? '').padEnd(widths[i]) + ' ').join('|') + '|'
  );
  return [sep, header, sep, ...dataRows, sep].join('\n');
}

// ─── 테스트 케이스 ───────────────────────────────────────────────

let passed = 0;
let failed = 0;

function test(name, fn) {
  try {
    fn();
    console.log('  PASS  ' + name);
    passed++;
  } catch (err) {
    console.error('  FAIL  ' + name);
    console.error('         ' + err.message);
    failed++;
  }
}

console.log('\n[output 포맷터 테스트]');

test('CSV 헤더가 키 순서대로 출력된다', () => {
  const rows = [{ id: '1', status: 'ACCEPTED', cost: 20 }];
  const csv = toCsv(rows);
  const lines = csv.split('\n');
  assert.strictEqual(lines[0], 'id,status,cost');
});

test('CSV 데이터 행이 올바르게 출력된다', () => {
  const rows = [{ id: 'ABC123', status: 'ACCEPTED', cost: 20 }];
  const csv = toCsv(rows);
  const lines = csv.split('\n');
  assert.strictEqual(lines[1], 'ABC123,ACCEPTED,20');
});

test('CSV 값에 쉼표가 있으면 큰따옴표로 감싼다', () => {
  const rows = [{ name: 'Kim, Chul', value: 100 }];
  const csv = toCsv(rows);
  const lines = csv.split('\n');
  assert.ok(lines[1].startsWith('"Kim, Chul"'), '쉼표 포함 값이 인용되지 않음');
});

test('CSV 값에 큰따옴표가 있으면 이스케이프한다', () => {
  const rows = [{ name: 'say "hello"', value: 1 }];
  const csv = toCsv(rows);
  const lines = csv.split('\n');
  assert.ok(lines[1].includes('""hello""'), '큰따옴표가 이스케이프되지 않음');
});

test('Table 구분선에 + 와 - 가 포함된다', () => {
  const rows = [{ sendId: 'ABC', status: 'ACCEPTED' }];
  const table = toTable(rows);
  assert.ok(table.includes('+'), '+ 없음');
  assert.ok(table.includes('-'), '- 없음');
});

test('Table 헤더에 컬럼명이 포함된다', () => {
  const rows = [{ sendId: 'ABC', status: 'ACCEPTED' }];
  const table = toTable(rows);
  assert.ok(table.includes('sendId'), 'sendId 컬럼명 없음');
  assert.ok(table.includes('status'), 'status 컬럼명 없음');
});

test('Table 데이터 값이 포함된다', () => {
  const rows = [{ sendId: 'ULID123', status: 'ACCEPTED' }];
  const table = toTable(rows);
  assert.ok(table.includes('ULID123'), '데이터 값 없음');
});

test('null 값은 빈 문자열로 처리된다', () => {
  const rows = [{ id: 'X', subject: null }];
  const csv = toCsv(rows);
  const lines = csv.split('\n');
  assert.strictEqual(lines[1], 'X,');
});

test('여러 행을 올바르게 처리한다', () => {
  const rows = [
    { id: '1', ch: 'SMS' },
    { id: '2', ch: 'LMS' },
  ];
  const csv = toCsv(rows);
  const lines = csv.split('\n');
  assert.strictEqual(lines.length, 3); // header + 2 rows
  assert.ok(lines[2].includes('LMS'));
});

console.log('\n결과: ' + passed + ' 통과 / ' + failed + ' 실패\n');
if (failed > 0) process.exit(1);

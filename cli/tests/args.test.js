/**
 * args 파서 단위 테스트
 */
'use strict';

const assert = require('assert');
const { parseArgs, getFlag, hasFlag } = require('../src/util/args');

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

console.log('\n[args 파서 테스트]');

test('--key value 형식 파싱', () => {
  const { flags } = parseArgs(['--key', 'myvalue']);
  assert.strictEqual(flags.get('key'), 'myvalue');
});

test('--key=value 형식 파싱', () => {
  const { flags } = parseArgs(['--key=myvalue']);
  assert.strictEqual(flags.get('key'), 'myvalue');
});

test('--flag 불리언 플래그 파싱', () => {
  const { flags } = parseArgs(['--ad']);
  assert.strictEqual(flags.get('ad'), true);
});

test('-k value 단축 플래그 파싱', () => {
  const { flags } = parseArgs(['-k', 'mykey']);
  assert.strictEqual(flags.get('k'), 'mykey');
});

test('positional 인수 파싱', () => {
  const { positional } = parseArgs(['send', 'sms']);
  assert.deepStrictEqual(positional, ['send', 'sms']);
});

test('플래그와 positional 혼합', () => {
  const { flags, positional } = parseArgs(['history', 'get', 'ULID123', '--format', 'json']);
  assert.strictEqual(positional[0], 'history');
  assert.strictEqual(positional[1], 'get');
  assert.strictEqual(positional[2], 'ULID123');
  assert.strictEqual(flags.get('format'), 'json');
});

test('getFlag: 긴 이름으로 값 반환', () => {
  const { flags } = parseArgs(['--format', 'csv']);
  assert.strictEqual(getFlag(flags, ['format', 'f'], 'table'), 'csv');
});

test('getFlag: 단축 이름으로 값 반환', () => {
  const { flags } = parseArgs(['-f', 'json']);
  assert.strictEqual(getFlag(flags, ['format', 'f'], 'table'), 'json');
});

test('getFlag: 없으면 기본값 반환', () => {
  const { flags } = parseArgs([]);
  assert.strictEqual(getFlag(flags, ['format', 'f'], 'table'), 'table');
});

test('hasFlag: 플래그 존재 확인', () => {
  const { flags } = parseArgs(['--ad']);
  assert.strictEqual(hasFlag(flags, ['ad']), true);
});

test('hasFlag: 없으면 false', () => {
  const { flags } = parseArgs([]);
  assert.strictEqual(hasFlag(flags, ['ad']), false);
});

test('빈 argv 파싱', () => {
  const { flags, positional } = parseArgs([]);
  assert.strictEqual(flags.size, 0);
  assert.strictEqual(positional.length, 0);
});

test('다음 토큰이 플래그면 boolean 처리', () => {
  const { flags } = parseArgs(['--verbose', '--format', 'json']);
  assert.strictEqual(flags.get('verbose'), true);
  assert.strictEqual(flags.get('format'), 'json');
});

console.log('\n결과: ' + passed + ' 통과 / ' + failed + ' 실패\n');
if (failed > 0) process.exit(1);

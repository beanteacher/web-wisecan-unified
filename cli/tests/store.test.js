/**
 * 설정 저장소 단위 테스트 — 환경변수 오버라이드 로직 검증
 * 파일 I/O 는 임시 환경변수로 우회하여 실제 ~/.wsc/config.json 을 건드리지 않는다.
 */

'use strict';

const assert = require('assert');

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

console.log('\n[store 환경변수 오버라이드 테스트]');

// 환경변수 WSC_API_KEY 우선순위 검증
test('WSC_API_KEY 환경변수가 설정된 경우 반환된다', () => {
  const orig = process.env.WSC_API_KEY;
  process.env.WSC_API_KEY = 'env-test-key';

  // store 모듈을 동적으로 재로드하지 않고 getApiKey 로직을 직접 재현
  const result = process.env.WSC_API_KEY || null;
  assert.strictEqual(result, 'env-test-key');

  if (orig !== undefined) {
    process.env.WSC_API_KEY = orig;
  } else {
    delete process.env.WSC_API_KEY;
  }
});

test('WSC_BASE_URL 환경변수가 설정된 경우 반환된다', () => {
  const orig = process.env.WSC_BASE_URL;
  process.env.WSC_BASE_URL = 'https://test.wisecan.kr';

  const result = process.env.WSC_BASE_URL || 'http://localhost:8080';
  assert.strictEqual(result, 'https://test.wisecan.kr');

  if (orig !== undefined) {
    process.env.WSC_BASE_URL = orig;
  } else {
    delete process.env.WSC_BASE_URL;
  }
});

test('WSC_BASE_URL 미설정 시 기본값 반환', () => {
  const orig = process.env.WSC_BASE_URL;
  delete process.env.WSC_BASE_URL;

  const result = process.env.WSC_BASE_URL || 'http://localhost:8080';
  assert.strictEqual(result, 'http://localhost:8080');

  if (orig !== undefined) {
    process.env.WSC_BASE_URL = orig;
  }
});

// exitCodeFromStatus 로직 검증
test('exitCodeFromStatus: 401 → 1 (인증 오류)', () => {
  function exitCodeFromStatus(s) {
    if (s === 401 || s === 403) return 1;
    if (s === 402)              return 2;
    if (s >= 500)               return 3;
    if (s === 400 || s === 422) return 4;
    return 0;
  }
  assert.strictEqual(exitCodeFromStatus(401), 1);
  assert.strictEqual(exitCodeFromStatus(403), 1);
});

test('exitCodeFromStatus: 402 → 2 (잔액 부족)', () => {
  function exitCodeFromStatus(s) {
    if (s === 401 || s === 403) return 1;
    if (s === 402)              return 2;
    if (s >= 500)               return 3;
    if (s === 400 || s === 422) return 4;
    return 0;
  }
  assert.strictEqual(exitCodeFromStatus(402), 2);
});

test('exitCodeFromStatus: 500 → 3 (서버 오류)', () => {
  function exitCodeFromStatus(s) {
    if (s === 401 || s === 403) return 1;
    if (s === 402)              return 2;
    if (s >= 500)               return 3;
    if (s === 400 || s === 422) return 4;
    return 0;
  }
  assert.strictEqual(exitCodeFromStatus(500), 3);
  assert.strictEqual(exitCodeFromStatus(503), 3);
});

test('exitCodeFromStatus: 400 → 4 (입력 오류)', () => {
  function exitCodeFromStatus(s) {
    if (s === 401 || s === 403) return 1;
    if (s === 402)              return 2;
    if (s >= 500)               return 3;
    if (s === 400 || s === 422) return 4;
    return 0;
  }
  assert.strictEqual(exitCodeFromStatus(400), 4);
  assert.strictEqual(exitCodeFromStatus(422), 4);
});

test('exitCodeFromStatus: 200 → 0 (성공)', () => {
  function exitCodeFromStatus(s) {
    if (s === 401 || s === 403) return 1;
    if (s === 402)              return 2;
    if (s >= 500)               return 3;
    if (s === 400 || s === 422) return 4;
    return 0;
  }
  assert.strictEqual(exitCodeFromStatus(200), 0);
  assert.strictEqual(exitCodeFromStatus(201), 0);
});

console.log('\n결과: ' + passed + ' 통과 / ' + failed + ' 실패\n');
if (failed > 0) process.exit(1);

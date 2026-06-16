/**
 * 자동 업데이터 단위 테스트 — isNewer() 순수 함수 검증
 */

'use strict';

const assert = require('assert');
const { isNewer } = require('../src/util/updater');

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

console.log('\n[updater.isNewer 테스트]');

test('major 가 크면 newer', () => {
  assert.strictEqual(isNewer('2.0.0', '1.9.9'), true);
});

test('major 가 같고 minor 가 크면 newer', () => {
  assert.strictEqual(isNewer('1.2.0', '1.1.9'), true);
});

test('major·minor 같고 patch 가 크면 newer', () => {
  assert.strictEqual(isNewer('1.0.2', '1.0.1'), true);
});

test('완전히 같으면 false', () => {
  assert.strictEqual(isNewer('1.0.0', '1.0.0'), false);
});

test('낮은 버전이면 false', () => {
  assert.strictEqual(isNewer('1.0.0', '1.0.1'), false);
});

test('v 접두사 있어도 정상 비교', () => {
  assert.strictEqual(isNewer('v2.0.0', 'v1.9.9'), true);
});

test('v 접두사 혼합도 정상 비교', () => {
  assert.strictEqual(isNewer('v1.1.0', '1.0.9'), true);
});

test('patch 가 낮으면 false', () => {
  assert.strictEqual(isNewer('1.2.3', '1.2.4'), false);
});

console.log('\n결과: ' + passed + ' 통과 / ' + failed + ' 실패\n');
if (failed > 0) process.exit(1);

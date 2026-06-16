/**
 * wsc CLI 전체 테스트 러너 — 외부 패키지 없이 node 내장만 사용
 */
'use strict';

const { execFileSync } = require('child_process');
const path = require('path');

const tests = [
  'output.test.js',
  'updater.test.js',
  'store.test.js',
  'args.test.js',
];

let totalPass = 0;
let totalFail = 0;

for (const t of tests) {
  try {
    const out = execFileSync(process.execPath, [path.join(__dirname, t)], { encoding: 'utf8' });
    process.stdout.write(out);
    const m = out.match(/결과: (\d+) 통과 \/ (\d+) 실패/);
    if (m) { totalPass += parseInt(m[1]); totalFail += parseInt(m[2]); }
  } catch (err) {
    process.stdout.write(err.stdout || '');
    process.stderr.write(err.stderr || '');
    totalFail++;
  }
}

process.stdout.write('\n══════════════════════════════════════\n');
process.stdout.write('전체: ' + totalPass + ' 통과 / ' + totalFail + ' 실패\n');
process.stdout.write('══════════════════════════════════════\n');
if (totalFail > 0) process.exit(1);

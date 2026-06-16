#!/usr/bin/env node
/**
 * wsc — WiseCan CLI 진입점 (Node.js 내장 모듈만 사용)
 *
 * 명령 14종:
 *   auth login / logout / status                              (3종, RQ-CLI-001~003)
 *   send sms / lms / mms / kakao / rcs / bulk                (6종, RQ-CLI-101~109)
 *   history list / get                                        (2종, RQ-CLI-101~109)
 *   key list / create / revoke / rotate / scopes             (5종, RQ-CLI-004~007)
 *   callback list / get / register / delete                  (4종)
 *   config set / get / path                                  (3종)
 *   docs [주제]                                               (1종, RQ-CLI-401~405)
 *   snippet <채널>                                            (1종, RQ-CLI-401~405)
 *
 * 종료 코드 (02 §7.2):
 *   0 성공 / 1 인증 오류 / 2 잔액 부족 / 3 발송 오류 / 4 입력 오류 / 5 네트워크 오류
 */

'use strict';

const pkg = require('../package.json');
const { parseArgs, getFlag, hasFlag } = require('./util/args');

const authCmd     = require('./command/auth');
const sendCmd     = require('./command/send');
const historyCmd  = require('./command/history');
const keyCmd      = require('./command/key');
const callbackCmd = require('./command/callback');
const configCmd   = require('./command/config');
const { docsCmd, snippetCmd } = require('./command/docs');

const argv = process.argv.slice(2);
const { flags, positional } = parseArgs(argv);

const command   = positional[0];
const subcommand = positional[1];
const rest      = positional.slice(2);

if (!command || hasFlag(flags, ['help', 'h'])) {
  printHelp();
  process.exit(0);
}

if (hasFlag(flags, ['version', 'v'])) {
  process.stdout.write(pkg.version + '\n');
  process.exit(0);
}

(async () => {
  switch (command) {
    case 'auth':     await authCmd(subcommand, rest, flags); break;
    case 'send':     await sendCmd(subcommand, rest, flags); break;
    case 'history':  await historyCmd(subcommand, rest, flags); break;
    case 'key':      await keyCmd(subcommand, rest, flags); break;
    case 'callback': await callbackCmd(subcommand, rest, flags); break;
    case 'config':   await configCmd(subcommand, rest, flags); break;
    case 'docs':     docsCmd(subcommand, flags); break;
    case 'snippet':  snippetCmd(subcommand, flags); break;
    default:
      process.stderr.write('알 수 없는 명령: ' + command + '\n');
      printHelp();
      process.exit(4);
  }
})().catch((err) => {
  process.stderr.write('치명적 오류: ' + err.message + '\n');
  process.exit(1);
});

function printHelp() {
  process.stdout.write([
    'wsc v' + pkg.version + ' — WiseCan CLI',
    '',
    '사용법: wsc <명령> <하위명령> [옵션]',
    '',
    '명령:',
    '  auth     login / logout / status',
    '  send     sms / lms / mms / kakao / rcs / bulk',
    '  history  list / get <send_id>',
    '  key      list / create / revoke / rotate / scopes',
    '  callback list / get / register / delete',
    '  config   set / get / path',
    '  docs     [send|auth|history|key|callback]',
    '  snippet  sms|lms|mms|kakao|rcs|bulk',
    '',
    '옵션:',
    '  -h, --help     도움말 출력',
    '  -v, --version  버전 출력',
    '',
    '예시:',
    '  wsc auth login --key sk_live_xxxx',
    '  wsc send sms -c 01012345678 -d 01098765432 -t "안녕하세요!"',
    '  wsc history list --channel SMS -f json',
    '  wsc docs send',
    '',
  ].join('\n'));
}

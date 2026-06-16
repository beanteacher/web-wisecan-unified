# wsc — WiseCan CLI

WiseCan 통합 메시징 서비스의 CLI 클라이언트.
발송·이력·API Key·발신번호·설정을 셸에서 직접 조작할 수 있다.

## 설치

### 바이너리 직접 다운로드 (권장)

[Releases 페이지](https://github.com/beanteacher/web-wisecan-unified/releases/latest)에서 OS에 맞는 파일을 받는다.

| OS | 파일 |
|----|------|
| Windows | `wsc-win.exe` |
| macOS | `wsc-macos` |
| Linux | `wsc-linux` |

```bash
# macOS / Linux
chmod +x wsc-macos   # 또는 wsc-linux
sudo mv wsc-macos /usr/local/bin/wsc
```

### Node.js 로 직접 실행

```bash
cd cli
npm install
node src/index.js --help
# 또는 PATH에 추가
npm link
```

## 빠른 시작

```bash
# 1. API Key 등록
wsc auth login --key sk_live_xxxxxxxxxxxx

# 2. SMS 발송
wsc send sms --callback 01012345678 --destaddr 01098765432 --text "안녕하세요!"

# 3. 이력 확인
wsc history list
```

## 명령 목록 (14종)

### auth — 인증

```bash
wsc auth login  --key <API_KEY> [--url <서버주소>]
wsc auth logout
wsc auth status
```

### send — 발송

```bash
wsc send sms   -c <발신번호> -d <수신번호> -t <본문>
wsc send lms   -c <발신번호> -d <수신번호> -t <본문> [-s <제목>]
wsc send mms   -c <발신번호> -d <수신번호> -t <본문> [-s <제목>]
wsc send kakao -c <발신번호> -d <수신번호> -t <본문> --sender-key <키> --template-code <코드>
wsc send rcs   -c <발신번호> -d <수신번호> -t <본문> [-s <제목>]
wsc send bulk  -c <발신번호> --destaddrs <번호1,번호2,...> -t <본문> --channel SMS
```

공통 옵션:
- `--ad` — 광고성 메시지
- `--schedule <ISO8601>` — 예약 발송 (예: `2026-07-01T09:00:00`)
- `-f json|table|csv` — 출력 형식 (기본 `table`)

### history — 이력 조회

```bash
wsc history list [--from YYYY-MM-DD] [--to YYYY-MM-DD] [--channel SMS] [--page 0] [--size 20]
wsc history get <send_id>
```

### key — API Key 관리

```bash
wsc key list
wsc key create --name <이름> [--type TEST|PRODUCTION] [--scopes send,history:read] [--daily-limit 1000]
wsc key revoke <id>
wsc key rotate <id>
wsc key scopes
```

### callback — 발신번호 관리

```bash
wsc callback list
wsc callback get <id>
wsc callback register -n <번호> --type SELF_MOBILE|SELF_LANDLINE|EMPLOYEE|CORP_REP [--purpose <설명>]
wsc callback delete <id>
```

### config — 설정

```bash
wsc config set --url https://api.wisecan.kr
wsc config get
wsc config path
```

### docs / snippet — 문서 (무인증)

```bash
wsc docs [send|auth|history|key|callback]
wsc snippet sms|lms|mms|kakao|rcs|bulk
```

## 종료 코드

| 코드 | 의미 |
|------|------|
| 0 | 성공 |
| 1 | 인증 오류 (401/403) |
| 2 | 잔액 부족 (402) |
| 3 | 발송/서버 오류 (5xx) |
| 4 | 입력 오류 (400/422) |
| 5 | 네트워크 오류 |

## 환경 변수

| 변수 | 설명 |
|------|------|
| `WSC_API_KEY` | API Key (설정 파일보다 우선) |
| `WSC_BASE_URL` | 서버 주소 (기본 `http://localhost:8080`) |
| `WSC_NO_UPDATE_CHECK=1` | 자동 업데이트 확인 비활성화 |

## 빌드 (멀티 플랫폼)

```bash
cd cli
npm install

# 전체 3 OS
npm run build:all

# 개별
npm run build:win    # dist/wsc-win.exe
npm run build:mac    # dist/wsc-macos
npm run build:linux  # dist/wsc-linux
```

## 테스트

```bash
cd cli
node tests/output.test.js
node tests/updater.test.js
node tests/store.test.js
```

## 자동 업데이트

명령 실행 후 백그라운드에서 GitHub Releases 최신 버전을 확인한다.
새 버전이 있으면 stderr 에 다운로드 링크를 출력한다.
`WSC_NO_UPDATE_CHECK=1` 로 비활성화할 수 있다.

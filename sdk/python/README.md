# wisecan-sdk

WiseCan 통합 메시징 서비스 Python SDK.

SMS, LMS, MMS, 카카오 알림톡, RCS 5채널 발송과 발송 이력 조회를 제공합니다.

## 요구사항

- Python 3.9 이상
- httpx 0.27 이상

## 설치

```bash
pip install wisecan-sdk
```

## 빠른 시작

```python
from wisecan import WiseCan

client = WiseCan(api_key="wsc_live_YOUR_API_KEY")

# SMS 발송
result = client.send_sms(
    callback_number="01012345678",
    recipient_numbers=["01098765432"],
    message_body="안녕하세요, WiseCan입니다.",
)
print(result.send_id)      # ULID 26자
print(result.status)       # SendRequestStatus.QUEUED
print(result.total_cost)   # 차감 금액(원)
```

## 환경 분기

API Key 접두사로 자동 라우팅됩니다.

| 접두사 | 환경 | 엔드포인트 |
|--------|------|-----------|
| `wsc_live_` | 운영 | `https://api.wisecan.io` |
| `wsc_test_` | 테스트 | `https://api-test.wisecan.io` |

```python
# 테스트 환경 (자동)
client = WiseCan(api_key="wsc_test_YOUR_TEST_KEY")

# 직접 URL 지정 (로컬 개발용)
client = WiseCan(api_key="wsc_live_key", base_url="http://localhost:8080")
```

## 채널별 발송

### SMS

```python
result = client.send_sms(
    callback_number="01012345678",
    recipient_numbers=["01098765432", "01011112222"],
    message_body="단문 메시지 (90바이트 이하 권장)",
    is_advertisement=False,
)
```

### LMS

```python
result = client.send_lms(
    callback_number="01012345678",
    recipient_numbers=["01098765432"],
    message_body="장문 메시지 내용입니다. 2,000바이트까지 입력 가능합니다.",
    subject="공지사항",
)
```

### MMS

```python
result = client.send_mms(
    callback_number="01012345678",
    recipient_numbers=["01098765432"],
    message_body="멀티미디어 메시지 본문",
    subject="이벤트 안내",
)
```

### 카카오 알림톡

```python
result = client.send_kakao(
    callback_number="01012345678",
    recipient_numbers=["01098765432"],
    message_body="주문이 접수되었습니다.",
    sender_key="발신프로필키",
    template_code="ORDER_CONFIRM",
)
```

### RCS

```python
result = client.send_rcs(
    callback_number="01012345678",
    recipient_numbers=["01098765432"],
    message_body="RCS 메시지 내용",
    subject="제목",
)
```

### 예약 발송

모든 채널 메서드에 `scheduled_at` 을 지정하면 예약 발송으로 처리됩니다.

```python
from datetime import datetime, timedelta

result = client.send_sms(
    callback_number="01012345678",
    recipient_numbers=["01098765432"],
    message_body="내일 오전 9시에 발송됩니다.",
    scheduled_at=datetime.now() + timedelta(days=1),
)
```

## 발송 이력 조회

```python
from wisecan import SendChannel

# 전체 이력 (페이지네이션)
page = client.get_send_history(page=0, size=20)
print(page.total_elements)
for item in page.items:
    print(item.send_id, item.channel, item.status)

# 채널 필터
page = client.get_send_history(channel=SendChannel.SMS)

# 단건 상세
detail = client.get_send_detail("01J3ZRAA1AAAAAAAAAAAAAAAA")
```

## 예외 처리

모든 예외는 `WiseCanError` 를 상속합니다.

```python
from wisecan.exceptions import (
    WiseCanError,
    AuthenticationError,
    ValidationError,
    RateLimitError,
    InsufficientBalanceError,
    SendError,
)

try:
    result = client.send_sms(
        callback_number="01012345678",
        recipient_numbers=["01098765432"],
        message_body="테스트",
    )
except AuthenticationError:
    print("API Key가 유효하지 않습니다.")
except InsufficientBalanceError:
    print("잔액이 부족합니다.")
except ValidationError as e:
    print(f"요청 오류: {e.message}")
    print(f"필드 오류: {e.field_errors}")
except RateLimitError as e:
    print(f"요청 빈도 초과. {e.retry_after}초 후 재시도하세요.")
except SendError:
    print("서버 오류가 발생했습니다.")
except WiseCanError as e:
    print(f"기타 오류: {e.message} (HTTP {e.status_code})")
```

| 예외 | HTTP 상태 | 설명 |
|------|-----------|------|
| `AuthenticationError` | 401 | API Key 없음 또는 유효하지 않음 |
| `PermissionError` | 403 | 스코프 부족 |
| `ValidationError` | 400 | 요청 파라미터 오류 |
| `InsufficientBalanceError` | 402 | 잔액 부족 |
| `RateLimitError` | 429 | 요청 빈도 초과 |
| `SendError` | 5xx | 서버 오류 |

## 컨텍스트 매니저

```python
with WiseCan(api_key="wsc_live_key") as client:
    result = client.send_sms(
        callback_number="01012345678",
        recipient_numbers=["01098765432"],
        message_body="컨텍스트 매니저 예시",
    )
# with 블록 종료 시 커넥션 풀 자동 반환
```

## 개발

```bash
# 의존성 설치
pip install -e ".[dev]"

# 테스트 실행
pytest

# 특정 테스트
pytest tests/test_client.py -v
```

## 라이선스

MIT

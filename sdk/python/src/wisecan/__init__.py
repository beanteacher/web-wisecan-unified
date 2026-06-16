"""WiseCan Python SDK.

사용 예시::

    from wisecan import WiseCan

    client = WiseCan(api_key="wsc_live_xxxx")
    result = client.send_sms(
        callback_number="01012345678",
        recipient_numbers=["01098765432"],
        message_body="안녕하세요, WiseCan입니다.",
    )
    print(result.send_id)
"""

from .client import WiseCan
from .exceptions import (
    WiseCanError,
    AuthenticationError,
    ValidationError,
    RateLimitError,
    InsufficientBalanceError,
    SendError,
)
from .models import (
    SendChannel,
    SendRequestStatus,
    SendResult,
    SendHistoryItem,
    SendHistoryPage,
)

__all__ = [
    "WiseCan",
    # 예외
    "WiseCanError",
    "AuthenticationError",
    "ValidationError",
    "RateLimitError",
    "InsufficientBalanceError",
    "SendError",
    # 모델
    "SendChannel",
    "SendRequestStatus",
    "SendResult",
    "SendHistoryItem",
    "SendHistoryPage",
]

__version__ = "0.1.0"

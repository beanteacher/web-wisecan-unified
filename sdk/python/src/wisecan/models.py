"""WiseCan SDK 데이터 모델.

백엔드 SendChannel, SendRequestStatus enum 및 응답 DTO 와 1:1 대응한다.
(backend/src/main/java/com/wisecan/unified/domain/dispatch/ 참조)
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional


class SendChannel(str, Enum):
    """발송 채널 5종.

    백엔드 SendChannel enum 과 값이 일치한다.
    """

    SMS = "SMS"
    LMS = "LMS"
    MMS = "MMS"
    KAKAO = "KAKAO"
    RCS = "RCS"


class SendRequestStatus(str, Enum):
    """발송 요청 내부 상태.

    백엔드 SendRequestStatus enum 과 값이 일치한다.
    """

    PENDING = "PENDING"
    QUEUED = "QUEUED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


@dataclass(frozen=True)
class SendResult:
    """발송 적재 응답.

    백엔드 SendRequestDto.AcceptResponse 에 대응한다.

    Attributes:
        send_id: 발송 요청 단일 식별자 (ULID 26자).
        status: 적재 상태 (PENDING / QUEUED 등).
        recipient_count: 실제 발송 대상 수신자 수.
        total_cost: 총 차감 금액(원).
        scheduled_at: 예약 발송 일시. 즉시 발송이면 None.
    """

    send_id: str
    status: SendRequestStatus
    recipient_count: int
    total_cost: int
    scheduled_at: Optional[datetime] = None

    @classmethod
    def _from_dict(cls, data: dict) -> SendResult:
        return cls(
            send_id=data["sendId"],
            status=SendRequestStatus(data["status"]),
            recipient_count=data["recipientCount"],
            total_cost=data["totalCost"],
            scheduled_at=_parse_datetime(data.get("scheduledAt")),
        )


@dataclass(frozen=True)
class SendHistoryItem:
    """발송 이력 단건.

    백엔드 SendRequestDto.DetailResponse 에 대응한다.

    Attributes:
        send_id: 발송 요청 식별자 (ULID 26자).
        channel: 발송 채널.
        callback_number: 발신번호.
        recipient_count: 수신자 수.
        subject: 메시지 제목 (LMS/MMS/카카오). SMS 이면 None.
        status: 발송 상태.
        unit_cost: 건당 단가(원).
        total_cost: 총 차감 금액(원).
        requested_at: 요청 시각.
        created_at: 서버 적재 시각.
    """

    send_id: str
    channel: SendChannel
    callback_number: str
    recipient_count: int
    status: SendRequestStatus
    unit_cost: int
    total_cost: int
    requested_at: datetime
    created_at: datetime
    subject: Optional[str] = None

    @classmethod
    def _from_dict(cls, data: dict) -> SendHistoryItem:
        return cls(
            send_id=data["sendId"],
            channel=SendChannel(data["channel"]),
            callback_number=data["callbackNumber"],
            recipient_count=data["recipientCount"],
            status=SendRequestStatus(data["status"]),
            unit_cost=data["unitCost"],
            total_cost=data["totalCost"],
            requested_at=_parse_datetime(data["requestedAt"]),
            created_at=_parse_datetime(data["createdAt"]),
            subject=data.get("subject"),
        )


@dataclass
class SendHistoryPage:
    """발송 이력 페이지 응답.

    Attributes:
        items: 현재 페이지 발송 이력 목록.
        total_elements: 전체 건수.
        total_pages: 전체 페이지 수.
        page: 현재 페이지 번호 (0-based).
        size: 페이지 크기.
    """

    items: list[SendHistoryItem] = field(default_factory=list)
    total_elements: int = 0
    total_pages: int = 0
    page: int = 0
    size: int = 20

    @classmethod
    def _from_dict(cls, data: dict) -> SendHistoryPage:
        content = data.get("content", [])
        return cls(
            items=[SendHistoryItem._from_dict(item) for item in content],
            total_elements=data.get("totalElements", 0),
            total_pages=data.get("totalPages", 0),
            page=data.get("number", 0),
            size=data.get("size", 20),
        )


def _parse_datetime(value: Optional[str]) -> Optional[datetime]:
    """ISO 8601 문자열을 datetime 으로 변환한다. None 이면 None 반환."""
    if value is None:
        return None
    # 백엔드가 반환하는 LocalDateTime 형식: "2026-06-16T10:30:00"
    return datetime.fromisoformat(value)

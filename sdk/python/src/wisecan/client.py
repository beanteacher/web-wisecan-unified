"""WiseCan SDK 메인 클라이언트.

5채널(SMS/LMS/MMS/카카오/RCS) 발송 메서드와 발송 이력 조회를 제공한다.
API Key 인증은 HttpClient 가 처리하며, 이 클래스는 비즈니스 메서드만 노출한다.

사용 예시::

    from wisecan import WiseCan

    client = WiseCan(api_key="wsc_live_xxxx")

    # SMS 발송
    result = client.send_sms(
        callback_number="01012345678",
        recipient_numbers=["01098765432"],
        message_body="안녕하세요!",
    )

    # 카카오 알림톡 발송
    result = client.send_kakao(
        callback_number="01012345678",
        recipient_numbers=["01098765432"],
        message_body="주문이 접수되었습니다.",
        sender_key="abc123",
        template_code="ORDER_CONFIRM",
    )

    # 발송 이력 조회
    page = client.get_send_history(page=0, size=20)
"""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from .http import HttpClient
from .models import SendChannel, SendHistoryPage, SendResult

# SDK 가 사용하는 API 경로 (백엔드 WebSendController 기준)
_SEND_PATH = "/console/send"
_HISTORY_PATH = "/console/send/history"


class WiseCan:
    """WiseCan 통합 메시징 SDK 클라이언트.

    Args:
        api_key: 발급받은 API Key.
            ``wsc_live_`` 접두사면 운영 환경, ``wsc_test_`` 접두사면 테스트 환경으로
            자동 라우팅된다.
        base_url: API 기본 URL 직접 지정 (테스트·개발용). None 이면 api_key 로 결정.
        timeout: HTTP 요청 타임아웃(초). 기본 30초.

    Raises:
        ValueError: api_key 가 빈 문자열이면 즉시 오류를 발생시킨다.
    """

    def __init__(
        self,
        api_key: str,
        *,
        base_url: Optional[str] = None,
        timeout: float = 30.0,
    ) -> None:
        if not api_key:
            raise ValueError("api_key 는 필수입니다.")
        self._http = HttpClient(api_key, base_url=base_url, timeout=timeout)

    # ── SMS ────────────────────────────────────────────────────────────

    def send_sms(
        self,
        *,
        callback_number: str,
        recipient_numbers: list[str],
        message_body: str,
        is_advertisement: bool = False,
        scheduled_at: Optional[datetime] = None,
    ) -> SendResult:
        """SMS를 발송한다. 본문 90바이트 초과 시 서버에서 LMS로 자동 전환된다.

        Args:
            callback_number: 발신번호 (등록된 번호만 허용).
            recipient_numbers: 수신번호 목록 (최대 1,000개).
            message_body: 메시지 본문 (90바이트 이하 권장).
            is_advertisement: 광고성 메시지 여부.
            scheduled_at: 예약 발송 일시. None 이면 즉시 발송.

        Returns:
            SendResult — send_id, status, recipient_count, total_cost 포함.
        """
        return self._send(
            channel=SendChannel.SMS,
            callback_number=callback_number,
            recipient_numbers=recipient_numbers,
            message_body=message_body,
            is_advertisement=is_advertisement,
            scheduled_at=scheduled_at,
        )

    # ── LMS ────────────────────────────────────────────────────────────

    def send_lms(
        self,
        *,
        callback_number: str,
        recipient_numbers: list[str],
        message_body: str,
        subject: Optional[str] = None,
        is_advertisement: bool = False,
        scheduled_at: Optional[datetime] = None,
    ) -> SendResult:
        """LMS(장문 문자)를 발송한다.

        Args:
            callback_number: 발신번호.
            recipient_numbers: 수신번호 목록 (최대 1,000개).
            message_body: 메시지 본문 (2,000바이트 이하).
            subject: 메시지 제목 (최대 120자).
            is_advertisement: 광고성 메시지 여부.
            scheduled_at: 예약 발송 일시.

        Returns:
            SendResult.
        """
        return self._send(
            channel=SendChannel.LMS,
            callback_number=callback_number,
            recipient_numbers=recipient_numbers,
            message_body=message_body,
            subject=subject,
            is_advertisement=is_advertisement,
            scheduled_at=scheduled_at,
        )

    # ── MMS ────────────────────────────────────────────────────────────

    def send_mms(
        self,
        *,
        callback_number: str,
        recipient_numbers: list[str],
        message_body: str,
        subject: Optional[str] = None,
        is_advertisement: bool = False,
        scheduled_at: Optional[datetime] = None,
    ) -> SendResult:
        """MMS(멀티미디어 문자)를 발송한다.

        Args:
            callback_number: 발신번호.
            recipient_numbers: 수신번호 목록 (최대 1,000개).
            message_body: 메시지 본문.
            subject: 메시지 제목 (최대 120자).
            is_advertisement: 광고성 메시지 여부.
            scheduled_at: 예약 발송 일시.

        Returns:
            SendResult.
        """
        return self._send(
            channel=SendChannel.MMS,
            callback_number=callback_number,
            recipient_numbers=recipient_numbers,
            message_body=message_body,
            subject=subject,
            is_advertisement=is_advertisement,
            scheduled_at=scheduled_at,
        )

    # ── 카카오 알림톡 ─────────────────────────────────────────────────

    def send_kakao(
        self,
        *,
        callback_number: str,
        recipient_numbers: list[str],
        message_body: str,
        sender_key: str,
        template_code: str,
        subject: Optional[str] = None,
        is_advertisement: bool = False,
        scheduled_at: Optional[datetime] = None,
    ) -> SendResult:
        """카카오 알림톡을 발송한다.

        Args:
            callback_number: 발신번호.
            recipient_numbers: 수신번호 목록 (최대 1,000개).
            message_body: 메시지 본문.
            sender_key: 카카오 발신프로필 키 (필수).
            template_code: 카카오 템플릿 코드 (필수).
            subject: 메시지 제목 (최대 120자).
            is_advertisement: 광고성 메시지 여부.
            scheduled_at: 예약 발송 일시.

        Returns:
            SendResult.

        Raises:
            ValueError: sender_key 또는 template_code 가 비어 있으면 오류.
        """
        if not sender_key:
            raise ValueError("카카오 발송에는 sender_key 가 필수입니다.")
        if not template_code:
            raise ValueError("카카오 발송에는 template_code 가 필수입니다.")
        return self._send(
            channel=SendChannel.KAKAO,
            callback_number=callback_number,
            recipient_numbers=recipient_numbers,
            message_body=message_body,
            subject=subject,
            sender_key=sender_key,
            template_code=template_code,
            is_advertisement=is_advertisement,
            scheduled_at=scheduled_at,
        )

    # ── RCS ────────────────────────────────────────────────────────────

    def send_rcs(
        self,
        *,
        callback_number: str,
        recipient_numbers: list[str],
        message_body: str,
        subject: Optional[str] = None,
        is_advertisement: bool = False,
        scheduled_at: Optional[datetime] = None,
    ) -> SendResult:
        """RCS 메시지를 발송한다.

        Args:
            callback_number: 발신번호.
            recipient_numbers: 수신번호 목록 (최대 1,000개).
            message_body: 메시지 본문.
            subject: 메시지 제목 (최대 120자).
            is_advertisement: 광고성 메시지 여부.
            scheduled_at: 예약 발송 일시.

        Returns:
            SendResult.
        """
        return self._send(
            channel=SendChannel.RCS,
            callback_number=callback_number,
            recipient_numbers=recipient_numbers,
            message_body=message_body,
            subject=subject,
            is_advertisement=is_advertisement,
            scheduled_at=scheduled_at,
        )

    # ── 발송 이력 조회 ─────────────────────────────────────────────────

    def get_send_history(
        self,
        *,
        page: int = 0,
        size: int = 20,
        channel: Optional[SendChannel] = None,
        status: Optional[str] = None,
    ) -> SendHistoryPage:
        """발송 이력을 페이지 단위로 조회한다.

        Args:
            page: 페이지 번호 (0-based).
            size: 페이지 크기 (최대 100).
            channel: 채널 필터. None 이면 전체 채널.
            status: 상태 필터 (예: ``"QUEUED"``). None 이면 전체 상태.

        Returns:
            SendHistoryPage — items, total_elements, total_pages 포함.
        """
        params: dict = {"page": page, "size": size}
        if channel is not None:
            params["channel"] = channel.value
        if status is not None:
            params["status"] = status

        data = self._http.get(_HISTORY_PATH, params=params)
        return SendHistoryPage._from_dict(data)

    def get_send_detail(self, send_id: str) -> dict:
        """발송 단건 상세를 조회한다.

        Args:
            send_id: 발송 요청 식별자 (ULID 26자).

        Returns:
            SendRequestDto.DetailResponse 에 대응하는 딕셔너리.
        """
        return self._http.get(f"{_HISTORY_PATH}/{send_id}")

    # ── 컨텍스트 매니저 ────────────────────────────────────────────────

    def __enter__(self) -> WiseCan:
        return self

    def __exit__(self, *_) -> None:
        self._http.close()

    def close(self) -> None:
        """HTTP 커넥션 풀을 닫는다."""
        self._http.close()

    # ── 내부 공통 발송 ────────────────────────────────────────────────

    def _send(
        self,
        *,
        channel: SendChannel,
        callback_number: str,
        recipient_numbers: list[str],
        message_body: str,
        subject: Optional[str] = None,
        sender_key: Optional[str] = None,
        template_code: Optional[str] = None,
        is_advertisement: bool = False,
        scheduled_at: Optional[datetime] = None,
    ) -> SendResult:
        """모든 채널 발송의 공통 로직.

        scheduled_at 이 있으면 예약 발송 엔드포인트로, 없으면 단건 발송으로 라우팅한다.
        """
        if not callback_number:
            raise ValueError("callback_number 는 필수입니다.")
        if not recipient_numbers:
            raise ValueError("recipient_numbers 는 최소 1개 이상이어야 합니다.")
        if not message_body:
            raise ValueError("message_body 는 필수입니다.")

        body: dict = {
            "callbackNumber": callback_number,
            "recipientNumbers": recipient_numbers,
            "channel": channel.value,
            "messageBody": message_body,
            "isAdvertisement": is_advertisement,
        }
        if subject is not None:
            body["subject"] = subject
        if sender_key is not None:
            body["senderKey"] = sender_key
        if template_code is not None:
            body["templateCode"] = template_code

        if scheduled_at is not None:
            body["scheduledAt"] = scheduled_at.isoformat()
            path = f"{_SEND_PATH}/scheduled"
        else:
            path = _SEND_PATH

        data = self._http.post(path, body)
        return SendResult._from_dict(data)

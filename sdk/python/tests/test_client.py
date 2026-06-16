"""WiseCan 클라이언트 단위 테스트.

실제 HTTP 통신 없이 HttpClient._http 를 pytest-mock 으로 패칭한다.
"""

from datetime import datetime, timedelta
from unittest.mock import MagicMock, patch

import pytest

from wisecan import WiseCan
from wisecan.exceptions import AuthenticationError, ValidationError
from wisecan.models import SendChannel, SendRequestStatus, SendResult, SendHistoryPage


# ── 픽스처 ────────────────────────────────────────────────────────────


@pytest.fixture()
def client():
    """테스트용 WiseCan 클라이언트 (운영 환경 키 형태, HTTP 통신 없음)."""
    return WiseCan(api_key="wsc_live_test_key_fixture", base_url="http://localhost:8080")


def _accept_response(
    send_id: str = "01J3ZRAA1AAAAAAAAAAAAAAAA",
    status: str = "QUEUED",
    recipient_count: int = 1,
    total_cost: int = 20,
    scheduled_at=None,
) -> dict:
    return {
        "sendId": send_id,
        "status": status,
        "recipientCount": recipient_count,
        "totalCost": total_cost,
        "scheduledAt": scheduled_at,
    }


# ── 생성자 ─────────────────────────────────────────────────────────────


class TestWiseCanInit:
    def test_empty_api_key_raises(self):
        with pytest.raises(ValueError, match="api_key"):
            WiseCan(api_key="")

    def test_test_prefix_uses_test_env(self):
        c = WiseCan(api_key="wsc_test_abcdef")
        assert "test" in c._http._base_url

    def test_live_prefix_uses_prod_env(self):
        c = WiseCan(api_key="wsc_live_abcdef")
        assert "test" not in c._http._base_url

    def test_custom_base_url(self):
        c = WiseCan(api_key="wsc_live_key", base_url="http://localhost:9090")
        assert c._http._base_url == "http://localhost:9090"


# ── send_sms ───────────────────────────────────────────────────────────


class TestSendSms:
    def test_success(self, client):
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            result = client.send_sms(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="테스트 문자",
            )
        assert isinstance(result, SendResult)
        assert result.send_id == "01J3ZRAA1AAAAAAAAAAAAAAAA"
        assert result.status == SendRequestStatus.QUEUED
        mock_post.assert_called_once()
        call_args = mock_post.call_args
        assert call_args[0][0] == "/console/send"
        body = call_args[0][1]
        assert body["channel"] == "SMS"
        assert body["callbackNumber"] == "01012345678"
        assert body["recipientNumbers"] == ["01099998888"]
        assert body["messageBody"] == "테스트 문자"
        assert body["isAdvertisement"] is False

    def test_advertisement_flag(self, client):
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            client.send_sms(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="광고 문자입니다.",
                is_advertisement=True,
            )
        body = mock_post.call_args[0][1]
        assert body["isAdvertisement"] is True

    def test_multiple_recipients(self, client):
        recipients = [f"0109999{i:04d}" for i in range(10)]
        with patch.object(client._http, "post", return_value=_accept_response(recipient_count=10)) as mock_post:
            result = client.send_sms(
                callback_number="01012345678",
                recipient_numbers=recipients,
                message_body="다수 발송",
            )
        assert result.recipient_count == 10
        body = mock_post.call_args[0][1]
        assert len(body["recipientNumbers"]) == 10

    def test_empty_callback_raises(self, client):
        with pytest.raises(ValueError, match="callback_number"):
            client.send_sms(callback_number="", recipient_numbers=["01099998888"], message_body="테스트")

    def test_empty_recipients_raises(self, client):
        with pytest.raises(ValueError, match="recipient_numbers"):
            client.send_sms(callback_number="01012345678", recipient_numbers=[], message_body="테스트")

    def test_empty_message_raises(self, client):
        with pytest.raises(ValueError, match="message_body"):
            client.send_sms(callback_number="01012345678", recipient_numbers=["01099998888"], message_body="")

    def test_scheduled_send_uses_scheduled_path(self, client):
        future = datetime.now() + timedelta(days=1)
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            client.send_sms(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="예약 발송",
                scheduled_at=future,
            )
        path = mock_post.call_args[0][0]
        assert path == "/console/send/scheduled"
        body = mock_post.call_args[0][1]
        assert "scheduledAt" in body


# ── send_lms ───────────────────────────────────────────────────────────


class TestSendLms:
    def test_success_with_subject(self, client):
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            client.send_lms(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="장문 메시지 내용",
                subject="공지사항",
            )
        body = mock_post.call_args[0][1]
        assert body["channel"] == "LMS"
        assert body["subject"] == "공지사항"

    def test_subject_omitted_when_none(self, client):
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            client.send_lms(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="장문 메시지",
            )
        body = mock_post.call_args[0][1]
        assert "subject" not in body


# ── send_mms ───────────────────────────────────────────────────────────


class TestSendMms:
    def test_channel_is_mms(self, client):
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            client.send_mms(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="MMS 내용",
            )
        body = mock_post.call_args[0][1]
        assert body["channel"] == "MMS"


# ── send_kakao ─────────────────────────────────────────────────────────


class TestSendKakao:
    def test_success(self, client):
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            client.send_kakao(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="주문이 완료되었습니다.",
                sender_key="sender_key_abc",
                template_code="ORDER_COMPLETE",
            )
        body = mock_post.call_args[0][1]
        assert body["channel"] == "KAKAO"
        assert body["senderKey"] == "sender_key_abc"
        assert body["templateCode"] == "ORDER_COMPLETE"

    def test_empty_sender_key_raises(self, client):
        with pytest.raises(ValueError, match="sender_key"):
            client.send_kakao(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="메시지",
                sender_key="",
                template_code="TMPL",
            )

    def test_empty_template_code_raises(self, client):
        with pytest.raises(ValueError, match="template_code"):
            client.send_kakao(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="메시지",
                sender_key="KEY",
                template_code="",
            )


# ── send_rcs ───────────────────────────────────────────────────────────


class TestSendRcs:
    def test_channel_is_rcs(self, client):
        with patch.object(client._http, "post", return_value=_accept_response()) as mock_post:
            client.send_rcs(
                callback_number="01012345678",
                recipient_numbers=["01099998888"],
                message_body="RCS 메시지",
            )
        body = mock_post.call_args[0][1]
        assert body["channel"] == "RCS"


# ── get_send_history ───────────────────────────────────────────────────


class TestGetSendHistory:
    def _page_response(self) -> dict:
        return {
            "content": [
                {
                    "sendId": "01J3ZRAA1EEEEEEEEEEEEEEEE",
                    "channel": "SMS",
                    "callbackNumber": "01012345678",
                    "recipientCount": 1,
                    "status": "QUEUED",
                    "unitCost": 20,
                    "totalCost": 20,
                    "requestedAt": "2026-06-16T10:00:00",
                    "createdAt": "2026-06-16T10:00:01",
                }
            ],
            "totalElements": 1,
            "totalPages": 1,
            "number": 0,
            "size": 20,
        }

    def test_returns_history_page(self, client):
        with patch.object(client._http, "get", return_value=self._page_response()):
            page = client.get_send_history()
        assert isinstance(page, SendHistoryPage)
        assert len(page.items) == 1
        assert page.total_elements == 1

    def test_channel_filter_passed_as_query_param(self, client):
        with patch.object(client._http, "get", return_value=self._page_response()) as mock_get:
            client.get_send_history(channel=SendChannel.SMS)
        params = mock_get.call_args[1]["params"]
        assert params["channel"] == "SMS"

    def test_pagination_params(self, client):
        with patch.object(client._http, "get", return_value=self._page_response()) as mock_get:
            client.get_send_history(page=2, size=50)
        params = mock_get.call_args[1]["params"]
        assert params["page"] == 2
        assert params["size"] == 50

    def test_no_channel_filter_omits_param(self, client):
        with patch.object(client._http, "get", return_value=self._page_response()) as mock_get:
            client.get_send_history()
        params = mock_get.call_args[1]["params"]
        assert "channel" not in params


# ── 컨텍스트 매니저 ────────────────────────────────────────────────────


class TestContextManager:
    def test_context_manager_calls_close(self):
        c = WiseCan(api_key="wsc_live_key", base_url="http://localhost:8080")
        with patch.object(c._http, "close") as mock_close:
            with c:
                pass
        mock_close.assert_called_once()

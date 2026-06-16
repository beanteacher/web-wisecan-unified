"""HttpClient 단위 테스트.

respx 로 httpx 를 인터셉트해 실제 네트워크 없이 검증한다.
"""

import pytest
import httpx
import respx

from wisecan.exceptions import (
    AuthenticationError,
    InsufficientBalanceError,
    RateLimitError,
    SendError,
    ValidationError,
    WiseCanError,
)
from wisecan.http import HttpClient

_BASE = "http://localhost:8080"


@pytest.fixture()
def http():
    return HttpClient("wsc_live_testkey", base_url=_BASE)


class TestHttpClientInit:
    def test_test_key_uses_test_base_url(self):
        c = HttpClient("wsc_test_key123")
        assert "test" in c._base_url

    def test_live_key_uses_prod_base_url(self):
        c = HttpClient("wsc_live_key123")
        assert "test" not in c._base_url

    def test_custom_base_url_overrides(self):
        c = HttpClient("wsc_live_key", base_url="http://custom:9090")
        assert c._base_url == "http://custom:9090"

    def test_trailing_slash_stripped(self):
        c = HttpClient("wsc_live_key", base_url="http://example.com/")
        assert not c._base_url.endswith("/")


class TestHttpClientPost:
    @respx.mock
    def test_post_success_returns_data(self, http):
        respx.post(f"{_BASE}/console/send").mock(
            return_value=httpx.Response(
                200,
                json={"success": True, "data": {"sendId": "ULID123", "status": "QUEUED", "recipientCount": 1, "totalCost": 20}},
            )
        )
        result = http.post("/console/send", {"channel": "SMS"})
        assert result["sendId"] == "ULID123"

    @respx.mock
    def test_post_401_raises_authentication_error(self, http):
        respx.post(f"{_BASE}/console/send").mock(
            return_value=httpx.Response(401, json={"message": "인증 실패", "errorCode": "AUTH_FAIL"})
        )
        with pytest.raises(AuthenticationError):
            http.post("/console/send", {})

    @respx.mock
    def test_post_400_raises_validation_error(self, http):
        respx.post(f"{_BASE}/console/send").mock(
            return_value=httpx.Response(
                400,
                json={"message": "유효하지 않은 요청", "fieldErrors": {"callbackNumber": ["필수"]}},
            )
        )
        with pytest.raises(ValidationError) as exc_info:
            http.post("/console/send", {})
        assert "callbackNumber" in exc_info.value.field_errors

    @respx.mock
    def test_post_402_raises_insufficient_balance(self, http):
        respx.post(f"{_BASE}/console/send").mock(
            return_value=httpx.Response(402, json={"message": "잔액 부족"})
        )
        with pytest.raises(InsufficientBalanceError):
            http.post("/console/send", {})

    @respx.mock
    def test_post_429_raises_rate_limit_error(self, http):
        respx.post(f"{_BASE}/console/send").mock(
            return_value=httpx.Response(429, json={"message": "요청 초과"})
        )
        with pytest.raises(RateLimitError):
            http.post("/console/send", {})

    @respx.mock
    def test_post_500_raises_send_error(self, http):
        respx.post(f"{_BASE}/console/send").mock(
            return_value=httpx.Response(500, json={"message": "서버 오류"})
        )
        with pytest.raises(SendError):
            http.post("/console/send", {})

    @respx.mock
    def test_invalid_json_raises_wise_can_error(self, http):
        respx.post(f"{_BASE}/console/send").mock(
            return_value=httpx.Response(200, content=b"not-json")
        )
        with pytest.raises(WiseCanError):
            http.post("/console/send", {})

    def test_network_error_raises_wise_can_error(self, http):
        with respx.mock:
            respx.post(f"{_BASE}/console/send").mock(side_effect=httpx.ConnectError("연결 실패"))
            with pytest.raises(WiseCanError, match="네트워크 오류"):
                http.post("/console/send", {})


class TestHttpClientGet:
    @respx.mock
    def test_get_success_returns_data(self, http):
        respx.get(f"{_BASE}/console/send/history").mock(
            return_value=httpx.Response(
                200,
                json={
                    "success": True,
                    "data": {
                        "content": [],
                        "totalElements": 0,
                        "totalPages": 0,
                        "number": 0,
                        "size": 20,
                    },
                },
            )
        )
        result = http.get("/console/send/history", params={"page": 0, "size": 20})
        assert result["totalElements"] == 0

    @respx.mock
    def test_get_401_raises_authentication_error(self, http):
        respx.get(f"{_BASE}/console/send/history").mock(
            return_value=httpx.Response(401, json={"message": "인증 실패"})
        )
        with pytest.raises(AuthenticationError):
            http.get("/console/send/history")

    @respx.mock
    def test_authorization_header_sent(self, http):
        route = respx.get(f"{_BASE}/console/send/history").mock(
            return_value=httpx.Response(
                200,
                json={"success": True, "data": {"content": [], "totalElements": 0, "totalPages": 0, "number": 0, "size": 20}},
            )
        )
        http.get("/console/send/history")
        request = route.calls.last.request
        assert request.headers["Authorization"] == "ApiKey wsc_live_testkey"


class TestHttpClientContextManager:
    def test_context_manager(self):
        with HttpClient("wsc_live_key", base_url=_BASE) as c:
            assert c._client is not None
        # 닫힌 후에도 예외 없음

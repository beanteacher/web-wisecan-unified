"""예외 계층 및 _from_response 팩토리 단위 테스트."""

import pytest

from wisecan.exceptions import (
    AuthenticationError,
    InsufficientBalanceError,
    PermissionError,
    RateLimitError,
    SendError,
    ValidationError,
    WiseCanError,
    _from_response,
)


class TestWiseCanError:
    def test_basic_attributes(self):
        exc = WiseCanError("오류 메시지", status_code=400, error_code="ERR_001")
        assert exc.message == "오류 메시지"
        assert exc.status_code == 400
        assert exc.error_code == "ERR_001"
        assert str(exc) == "오류 메시지"

    def test_optional_fields_default_none(self):
        exc = WiseCanError("오류")
        assert exc.status_code is None
        assert exc.error_code is None

    def test_repr(self):
        exc = WiseCanError("test", status_code=500, error_code="SRV")
        assert "WiseCanError" in repr(exc)
        assert "500" in repr(exc)


class TestFromResponse:
    def test_400_returns_validation_error(self):
        body = {"message": "유효하지 않은 요청", "errorCode": "INVALID", "fieldErrors": {"callbackNumber": ["필수"]}}
        exc = _from_response(400, body)
        assert isinstance(exc, ValidationError)
        assert exc.field_errors == {"callbackNumber": ["필수"]}

    def test_401_returns_authentication_error(self):
        body = {"message": "인증 실패", "errorCode": "AUTH_FAIL"}
        exc = _from_response(401, body)
        assert isinstance(exc, AuthenticationError)
        assert exc.status_code == 401

    def test_402_returns_insufficient_balance_error(self):
        body = {"message": "잔액 부족", "errorCode": "BALANCE_LOW"}
        exc = _from_response(402, body)
        assert isinstance(exc, InsufficientBalanceError)

    def test_403_returns_permission_error(self):
        body = {"message": "권한 없음"}
        exc = _from_response(403, body)
        assert isinstance(exc, PermissionError)

    def test_429_returns_rate_limit_error(self):
        body = {"message": "요청 초과"}
        exc = _from_response(429, body)
        assert isinstance(exc, RateLimitError)

    def test_500_returns_send_error(self):
        body = {"message": "서버 오류"}
        exc = _from_response(500, body)
        assert isinstance(exc, SendError)

    def test_503_returns_send_error(self):
        body = {"message": "서비스 불가"}
        exc = _from_response(503, body)
        assert isinstance(exc, SendError)

    def test_unknown_status_returns_base_error(self):
        body = {"message": "알 수 없는 오류"}
        exc = _from_response(418, body)
        assert isinstance(exc, WiseCanError)
        assert exc.status_code == 418

    def test_missing_message_uses_default(self):
        exc = _from_response(500, {})
        assert "500" in exc.message

    def test_validation_error_empty_field_errors(self):
        body = {"message": "잘못된 요청"}
        exc = _from_response(400, body)
        assert isinstance(exc, ValidationError)
        assert exc.field_errors == {}

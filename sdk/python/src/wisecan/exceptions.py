"""WiseCan SDK 예외 계층.

모든 SDK 예외는 WiseCanError 를 상속한다.
HTTP 오류 코드별 세분화된 예외는 _from_response() 팩토리로 생성한다.
"""


class WiseCanError(Exception):
    """SDK 최상위 예외."""

    def __init__(self, message: str, *, status_code: int | None = None, error_code: str | None = None) -> None:
        super().__init__(message)
        self.message = message
        self.status_code = status_code
        self.error_code = error_code

    def __repr__(self) -> str:
        return (
            f"{self.__class__.__name__}("
            f"message={self.message!r}, "
            f"status_code={self.status_code!r}, "
            f"error_code={self.error_code!r})"
        )


class AuthenticationError(WiseCanError):
    """API Key 가 없거나 유효하지 않음 (HTTP 401)."""


class PermissionError(WiseCanError):
    """API Key 스코프 부족 (HTTP 403)."""


class ValidationError(WiseCanError):
    """요청 파라미터 유효성 오류 (HTTP 400).

    Attributes:
        field_errors: 필드별 오류 목록. 예: {"callbackNumber": ["발신번호는 필수입니다"]}
    """

    def __init__(
        self,
        message: str,
        *,
        status_code: int | None = None,
        error_code: str | None = None,
        field_errors: dict[str, list[str]] | None = None,
    ) -> None:
        super().__init__(message, status_code=status_code, error_code=error_code)
        self.field_errors: dict[str, list[str]] = field_errors or {}


class RateLimitError(WiseCanError):
    """요청 빈도 초과 (HTTP 429).

    Attributes:
        retry_after: 재시도 가능 시간(초). 헤더에서 파싱.
    """

    def __init__(
        self,
        message: str,
        *,
        status_code: int | None = None,
        error_code: str | None = None,
        retry_after: int | None = None,
    ) -> None:
        super().__init__(message, status_code=status_code, error_code=error_code)
        self.retry_after = retry_after


class InsufficientBalanceError(WiseCanError):
    """잔액 부족으로 발송 거부 (HTTP 402 또는 비즈니스 오류)."""


class SendError(WiseCanError):
    """발송 처리 중 서버 오류 (HTTP 5xx)."""


def _from_response(status_code: int, body: dict) -> WiseCanError:
    """HTTP 응답 바디로부터 적절한 예외 인스턴스를 생성한다.

    Args:
        status_code: HTTP 상태 코드.
        body: 응답 JSON 바디. ``message``, ``errorCode`` 키를 기대.

    Returns:
        WiseCanError 서브클래스 인스턴스.
    """
    message = body.get("message", f"HTTP {status_code} 오류")
    error_code = body.get("errorCode")

    if status_code == 400:
        field_errors = body.get("fieldErrors", {})
        return ValidationError(message, status_code=status_code, error_code=error_code, field_errors=field_errors)
    if status_code == 401:
        return AuthenticationError(message, status_code=status_code, error_code=error_code)
    if status_code == 402:
        return InsufficientBalanceError(message, status_code=status_code, error_code=error_code)
    if status_code == 403:
        return PermissionError(message, status_code=status_code, error_code=error_code)
    if status_code == 429:
        return RateLimitError(message, status_code=status_code, error_code=error_code)
    if status_code >= 500:
        return SendError(message, status_code=status_code, error_code=error_code)
    return WiseCanError(message, status_code=status_code, error_code=error_code)

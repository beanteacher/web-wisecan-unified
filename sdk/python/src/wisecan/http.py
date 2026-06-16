"""HTTP 클라이언트 — httpx 기반 저수준 요청 처리.

WiseCan API 서버와의 통신을 담당한다.
인증 헤더 삽입, 응답 오류 변환, 환경(test/prod) 분기를 여기서 처리한다.
"""

from __future__ import annotations

from typing import Any

import httpx

from .exceptions import WiseCanError, _from_response

_PROD_BASE_URL = "https://api.wisecan.io"
_TEST_BASE_URL = "https://api-test.wisecan.io"

_DEFAULT_TIMEOUT = 30.0  # seconds


class HttpClient:
    """WiseCan REST API 클라이언트 (동기).

    Args:
        api_key: API Key 문자열 (``wsc_live_`` 또는 ``wsc_test_`` 접두사).
        base_url: 기본 URL. None 이면 api_key 접두사로 자동 선택.
        timeout: 요청 타임아웃(초). 기본 30초.
    """

    def __init__(
        self,
        api_key: str,
        *,
        base_url: str | None = None,
        timeout: float = _DEFAULT_TIMEOUT,
    ) -> None:
        self._api_key = api_key
        if base_url is not None:
            self._base_url = base_url.rstrip("/")
        elif api_key.startswith("wsc_test_"):
            self._base_url = _TEST_BASE_URL
        else:
            self._base_url = _PROD_BASE_URL

        self._client = httpx.Client(
            base_url=self._base_url,
            headers={
                "Authorization": f"ApiKey {api_key}",
                "Content-Type": "application/json",
                "Accept": "application/json",
                "User-Agent": "wisecan-python-sdk/0.1.0",
            },
            timeout=timeout,
        )

    # ── 공개 메서드 ────────────────────────────────────────────────────

    def post(self, path: str, body: dict) -> dict:
        """POST 요청을 보내고 응답 data 딕셔너리를 반환한다.

        Args:
            path: API 경로 (예: ``/dispatch/send``).
            body: 요청 바디 딕셔너리.

        Returns:
            응답 JSON 의 ``data`` 필드.

        Raises:
            WiseCanError: 오류 응답 또는 네트워크 장애 시.
        """
        try:
            response = self._client.post(path, json=body)
        except httpx.TransportError as exc:
            raise WiseCanError(f"네트워크 오류: {exc}") from exc
        return self._unwrap(response)

    def get(self, path: str, params: dict | None = None) -> dict:
        """GET 요청을 보내고 응답 data 딕셔너리를 반환한다.

        Args:
            path: API 경로.
            params: 쿼리 파라미터.

        Returns:
            응답 JSON 의 ``data`` 필드.

        Raises:
            WiseCanError: 오류 응답 또는 네트워크 장애 시.
        """
        try:
            response = self._client.get(path, params=params or {})
        except httpx.TransportError as exc:
            raise WiseCanError(f"네트워크 오류: {exc}") from exc
        return self._unwrap(response)

    def close(self) -> None:
        """HTTP 커넥션 풀을 닫는다."""
        self._client.close()

    # ── 컨텍스트 매니저 ────────────────────────────────────────────────

    def __enter__(self) -> HttpClient:
        return self

    def __exit__(self, *_: Any) -> None:
        self.close()

    # ── 내부 헬퍼 ─────────────────────────────────────────────────────

    @staticmethod
    def _unwrap(response: httpx.Response) -> dict:
        """응답을 파싱해 data 필드를 반환하거나 예외를 올린다.

        백엔드 ApiResponse 구조::

            { "success": true, "data": { ... } }
            { "success": false, "message": "...", "errorCode": "..." }
        """
        try:
            body: dict = response.json()
        except Exception:
            raise WiseCanError(
                f"응답 JSON 파싱 실패 (HTTP {response.status_code})",
                status_code=response.status_code,
            )

        if not response.is_success:
            raise _from_response(response.status_code, body)

        # 성공 응답 — data 필드 반환
        return body.get("data", body)

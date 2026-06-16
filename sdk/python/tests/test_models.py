"""SendResult, SendHistoryItem, SendHistoryPage 모델 단위 테스트."""

from datetime import datetime

import pytest

from wisecan.models import (
    SendChannel,
    SendHistoryItem,
    SendHistoryPage,
    SendRequestStatus,
    SendResult,
)


class TestSendChannel:
    def test_all_five_channels_exist(self):
        assert set(c.value for c in SendChannel) == {"SMS", "LMS", "MMS", "KAKAO", "RCS"}

    def test_string_equality(self):
        assert SendChannel.SMS == "SMS"
        assert SendChannel.KAKAO == "KAKAO"


class TestSendRequestStatus:
    def test_all_statuses_exist(self):
        values = {s.value for s in SendRequestStatus}
        assert values == {"PENDING", "QUEUED", "FAILED", "CANCELLED"}


class TestSendResult:
    def _sample(self, **overrides) -> dict:
        base = {
            "sendId": "01J3ZRAA1BBBBBBBBBBBBBBBB",
            "status": "QUEUED",
            "recipientCount": 3,
            "totalCost": 60,
            "scheduledAt": None,
        }
        base.update(overrides)
        return base

    def test_from_dict_basic(self):
        result = SendResult._from_dict(self._sample())
        assert result.send_id == "01J3ZRAA1BBBBBBBBBBBBBBBB"
        assert result.status == SendRequestStatus.QUEUED
        assert result.recipient_count == 3
        assert result.total_cost == 60
        assert result.scheduled_at is None

    def test_from_dict_with_scheduled_at(self):
        result = SendResult._from_dict(self._sample(scheduledAt="2026-07-01T09:00:00"))
        assert isinstance(result.scheduled_at, datetime)
        assert result.scheduled_at.year == 2026

    def test_immutable(self):
        result = SendResult._from_dict(self._sample())
        with pytest.raises(Exception):
            result.send_id = "changed"  # frozen dataclass


class TestSendHistoryItem:
    def _sample(self) -> dict:
        return {
            "sendId": "01J3ZRAA1CCCCCCCCCCCCCCCC",
            "channel": "SMS",
            "callbackNumber": "01012345678",
            "recipientCount": 1,
            "status": "QUEUED",
            "unitCost": 20,
            "totalCost": 20,
            "requestedAt": "2026-06-16T10:00:00",
            "createdAt": "2026-06-16T10:00:01",
            "subject": None,
        }

    def test_from_dict(self):
        item = SendHistoryItem._from_dict(self._sample())
        assert item.send_id == "01J3ZRAA1CCCCCCCCCCCCCCCC"
        assert item.channel == SendChannel.SMS
        assert item.callback_number == "01012345678"
        assert item.recipient_count == 1
        assert item.status == SendRequestStatus.QUEUED
        assert item.unit_cost == 20
        assert item.total_cost == 20
        assert isinstance(item.requested_at, datetime)
        assert item.subject is None

    def test_lms_with_subject(self):
        data = self._sample()
        data["channel"] = "LMS"
        data["subject"] = "공지사항"
        item = SendHistoryItem._from_dict(data)
        assert item.channel == SendChannel.LMS
        assert item.subject == "공지사항"


class TestSendHistoryPage:
    def test_empty_page(self):
        data = {"content": [], "totalElements": 0, "totalPages": 0, "number": 0, "size": 20}
        page = SendHistoryPage._from_dict(data)
        assert page.items == []
        assert page.total_elements == 0
        assert page.total_pages == 0

    def test_page_with_items(self):
        item_data = {
            "sendId": "01J3ZRAA1DDDDDDDDDDDDDDDD",
            "channel": "RCS",
            "callbackNumber": "01099998888",
            "recipientCount": 5,
            "status": "PENDING",
            "unitCost": 30,
            "totalCost": 150,
            "requestedAt": "2026-06-16T11:00:00",
            "createdAt": "2026-06-16T11:00:01",
        }
        data = {
            "content": [item_data],
            "totalElements": 1,
            "totalPages": 1,
            "number": 0,
            "size": 20,
        }
        page = SendHistoryPage._from_dict(data)
        assert len(page.items) == 1
        assert page.total_elements == 1
        assert page.items[0].channel == SendChannel.RCS

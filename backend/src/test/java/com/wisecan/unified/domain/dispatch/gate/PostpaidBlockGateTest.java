package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostpaidBlockGate — 후불 연체 차단 검증")
class PostpaidBlockGateTest {

    @InjectMocks
    private PostpaidBlockGate gate;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SendValidationContext ctx() {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, "안녕하세요", false, 1, 10L, NetworkType.TEST);
    }

    @Test
    @DisplayName("개인 회원(company_id null) — 후불 없음, 통과")
    void individualMember_passes() {
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(null);

        assertThatCode(() -> gate.validate(ctx())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("선불(PREPAID) 회사 소속 — 연체 조회 없이 통과")
    void prepaidCompany_passes() {
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(100L);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(String.class), eq(100L))).willReturn("PREPAID");

        assertThatCode(() -> gate.validate(ctx())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("후불(POSTPAID) 회사, 연체 없음 — 통과")
    void postpaidCompany_noOverdue_passes() {
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(200L);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(String.class), eq(200L))).willReturn("POSTPAID");
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Integer.class), eq(200L))).willReturn(0);

        assertThatCode(() -> gate.validate(ctx())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("후불(POSTPAID) 회사, 연체 청구서 존재 — INSUFFICIENT_BALANCE")
    void postpaidCompany_withOverdue_throws() {
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(300L);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(String.class), eq(300L))).willReturn("POSTPAID");
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Integer.class), eq(300L))).willReturn(2);

        assertThatThrownBy(() -> gate.validate(ctx()))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    @DisplayName("후불 회사, 연체 COUNT null 반환 — 0으로 간주, 통과")
    void postpaidCompany_nullOverdueCount_passes() {
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(400L);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(String.class), eq(400L))).willReturn("POSTPAID");
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Integer.class), eq(400L))).willReturn(null);

        assertThatCode(() -> gate.validate(ctx())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("order() == 90")
    void order_is_90() {
        assertThat(gate.order()).isEqualTo(90);
    }
}

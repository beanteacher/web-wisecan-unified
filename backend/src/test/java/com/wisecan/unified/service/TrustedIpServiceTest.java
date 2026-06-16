package com.wisecan.unified.service;

import com.wisecan.unified.domain.TrustedIp;
import com.wisecan.unified.dto.AuthDto;
import com.wisecan.unified.repository.TrustedIpRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrustedIpServiceTest {

    @Mock
    private TrustedIpRepository trustedIpRepository;

    @InjectMocks
    private TrustedIpService trustedIpService;

    @Test
    @DisplayName("신뢰 IP 여부 확인 — 등록된 IP면 true")
    void isTrusted_registered_returnsTrue() {
        given(trustedIpRepository.existsByMemberIdAndIpAddress(1L, "192.168.1.1")).willReturn(true);

        assertThat(trustedIpService.isTrusted(1L, "192.168.1.1")).isTrue();
    }

    @Test
    @DisplayName("신뢰 IP 여부 확인 — 미등록 IP면 false")
    void isTrusted_notRegistered_returnsFalse() {
        given(trustedIpRepository.existsByMemberIdAndIpAddress(1L, "10.0.0.1")).willReturn(false);

        assertThat(trustedIpService.isTrusted(1L, "10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("신뢰 IP 목록 조회 — 등록된 IP 반환")
    void list_returnsTrustedIps() {
        TrustedIp ip = TrustedIp.builder()
            .memberId(1L)
            .ipAddress("192.168.1.1")
            .label("사무실")
            .build();

        given(trustedIpRepository.findByMemberId(1L)).willReturn(List.of(ip));

        List<AuthDto.TrustedIpItem> result = trustedIpService.list(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ipAddress()).isEqualTo("192.168.1.1");
        assertThat(result.get(0).label()).isEqualTo("사무실");
    }

    @Test
    @DisplayName("신뢰 IP 등록 성공")
    void register_success() {
        AuthDto.TrustedIpRegisterRequest request =
            new AuthDto.TrustedIpRegisterRequest("192.168.1.100", "재택근무");

        TrustedIp saved = TrustedIp.builder()
            .memberId(1L)
            .ipAddress("192.168.1.100")
            .label("재택근무")
            .build();

        given(trustedIpRepository.countByMemberId(1L)).willReturn(2);
        given(trustedIpRepository.existsByMemberIdAndIpAddress(1L, "192.168.1.100")).willReturn(false);
        given(trustedIpRepository.save(any(TrustedIp.class))).willReturn(saved);

        AuthDto.TrustedIpItem result = trustedIpService.register(1L, request);

        assertThat(result.ipAddress()).isEqualTo("192.168.1.100");
        assertThat(result.label()).isEqualTo("재택근무");
        verify(trustedIpRepository).save(any(TrustedIp.class));
    }

    @Test
    @DisplayName("신뢰 IP 10개 초과 등록 시 IllegalStateException 발생")
    void register_exceedsMax_throwsException() {
        AuthDto.TrustedIpRegisterRequest request =
            new AuthDto.TrustedIpRegisterRequest("10.0.0.1", "초과");

        given(trustedIpRepository.countByMemberId(1L)).willReturn(10);

        assertThatThrownBy(() -> trustedIpService.register(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("10개");
    }

    @Test
    @DisplayName("이미 등록된 IP 재등록 시 IllegalStateException 발생")
    void register_duplicateIp_throwsException() {
        AuthDto.TrustedIpRegisterRequest request =
            new AuthDto.TrustedIpRegisterRequest("192.168.1.1", "중복");

        given(trustedIpRepository.countByMemberId(1L)).willReturn(3);
        given(trustedIpRepository.existsByMemberIdAndIpAddress(1L, "192.168.1.1")).willReturn(true);

        assertThatThrownBy(() -> trustedIpService.register(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 등록된");
    }

    @Test
    @DisplayName("신뢰 IP 삭제 성공")
    void delete_success() {
        given(trustedIpRepository.existsById(99L)).willReturn(true);

        trustedIpService.delete(1L, 99L);

        verify(trustedIpRepository).deleteByMemberIdAndId(1L, 99L);
    }

    @Test
    @DisplayName("존재하지 않는 신뢰 IP 삭제 시 EntityNotFoundException 발생")
    void delete_notFound_throwsException() {
        given(trustedIpRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> trustedIpService.delete(1L, 999L))
            .isInstanceOf(com.wisecan.unified.exception.EntityNotFoundException.class);
    }
}

package com.wisecan.unified.service.admin;

import com.wisecan.unified.domain.admin.RoutingCarrier;
import com.wisecan.unified.domain.admin.RoutingChannel;
import com.wisecan.unified.domain.admin.RoutingMapping;
import com.wisecan.unified.dto.admin.RoutingMappingDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.admin.RoutingMappingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * RoutingMappingService 단위 테스트 — §12.4.
 */
@ExtendWith(MockitoExtension.class)
class RoutingMappingServiceTest {

    @Mock private RoutingMappingRepository routingMappingRepository;
    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private RoutingMappingService routingMappingService;

    // ── Upsert — 신규 등록 ───────────────────────────────────────────────

    @Test
    @DisplayName("신규 매핑 등록 — (memberId, channel) 미존재 시 INSERT")
    void upsert_newMapping_insertsRecord() {
        RoutingMappingDto.UpsertRequest request = new RoutingMappingDto.UpsertRequest(
                1L, RoutingChannel.KAKAO, RoutingCarrier.LG_CNS, "LG CNS 기본");

        given(memberRepository.existsById(1L)).willReturn(true);
        given(routingMappingRepository.findByMemberIdAndChannel(1L, RoutingChannel.KAKAO))
                .willReturn(Optional.empty());

        RoutingMapping savedMapping = RoutingMapping.builder()
                .memberId(1L)
                .channel(RoutingChannel.KAKAO)
                .carrier(RoutingCarrier.LG_CNS)
                .memo("LG CNS 기본")
                .operatorId(99L)
                .build();
        given(routingMappingRepository.save(any())).willReturn(savedMapping);

        RoutingMappingDto.Response response = routingMappingService.upsert(99L, request);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.channel()).isEqualTo(RoutingChannel.KAKAO);
        assertThat(response.carrier()).isEqualTo(RoutingCarrier.LG_CNS);
        verify(routingMappingRepository).save(any(RoutingMapping.class));
    }

    @Test
    @DisplayName("기존 매핑 수정 — (memberId, channel) 존재 시 carrier 갱신")
    void upsert_existingMapping_updatesCarrier() {
        RoutingMappingDto.UpsertRequest request = new RoutingMappingDto.UpsertRequest(
                1L, RoutingChannel.KAKAO, RoutingCarrier.KT, "KT로 변경");

        given(memberRepository.existsById(1L)).willReturn(true);

        RoutingMapping existing = RoutingMapping.builder()
                .memberId(1L)
                .channel(RoutingChannel.KAKAO)
                .carrier(RoutingCarrier.LG_CNS)
                .memo("LG CNS 기본")
                .operatorId(99L)
                .build();
        given(routingMappingRepository.findByMemberIdAndChannel(1L, RoutingChannel.KAKAO))
                .willReturn(Optional.of(existing));

        RoutingMappingDto.Response response = routingMappingService.upsert(99L, request);

        assertThat(response.carrier()).isEqualTo(RoutingCarrier.KT);
    }

    @Test
    @DisplayName("존재하지 않는 회원 매핑 시도 — EntityNotFoundException")
    void upsert_memberNotFound_throwsException() {
        RoutingMappingDto.UpsertRequest request = new RoutingMappingDto.UpsertRequest(
                999L, RoutingChannel.KAKAO, RoutingCarrier.LG_CNS, null);

        given(memberRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> routingMappingService.upsert(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── 조회 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("회원별 매핑 목록 조회")
    void listByMember_returnsMappings() {
        RoutingMapping mapping = RoutingMapping.builder()
                .memberId(1L)
                .channel(RoutingChannel.RCS)
                .carrier(RoutingCarrier.INFOBANK)
                .memo("인포뱅크 RCS")
                .operatorId(99L)
                .build();
        given(routingMappingRepository.findByMemberId(1L)).willReturn(List.of(mapping));

        List<RoutingMappingDto.Response> result = routingMappingService.listByMember(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).channel()).isEqualTo(RoutingChannel.RCS);
        assertThat(result.get(0).carrier()).isEqualTo(RoutingCarrier.INFOBANK);
    }

    @Test
    @DisplayName("채널별 매핑 목록 조회")
    void listByChannel_returnsMappings() {
        RoutingMapping mapping = RoutingMapping.builder()
                .memberId(1L)
                .channel(RoutingChannel.KAKAO)
                .carrier(RoutingCarrier.KT)
                .operatorId(99L)
                .build();
        given(routingMappingRepository.findByChannel(RoutingChannel.KAKAO))
                .willReturn(List.of(mapping));

        List<RoutingMappingDto.Response> result =
                routingMappingService.listByChannel(RoutingChannel.KAKAO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).carrier()).isEqualTo(RoutingCarrier.KT);
    }

    // ── 삭제 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("매핑 삭제")
    void delete_existingMapping_deleted() {
        RoutingMapping mapping = RoutingMapping.builder()
                .memberId(1L)
                .channel(RoutingChannel.KAKAO)
                .carrier(RoutingCarrier.LG_CNS)
                .operatorId(99L)
                .build();
        given(routingMappingRepository.findById(10L)).willReturn(Optional.of(mapping));

        routingMappingService.delete(10L, 99L);

        verify(routingMappingRepository).delete(mapping);
    }

    @Test
    @DisplayName("존재하지 않는 매핑 삭제 — EntityNotFoundException")
    void delete_notFound_throwsException() {
        given(routingMappingRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routingMappingService.delete(999L, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}

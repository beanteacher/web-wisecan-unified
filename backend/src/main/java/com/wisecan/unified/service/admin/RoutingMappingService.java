package com.wisecan.unified.service.admin;

import com.wisecan.unified.domain.admin.RoutingChannel;
import com.wisecan.unified.domain.admin.RoutingMapping;
import com.wisecan.unified.dto.admin.RoutingMappingDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.admin.RoutingMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카카오/RCS 라우팅 매핑 서비스 — §12.4.
 *
 * 회원별 카카오 중계사 1:1 매핑(LG CNS·KT·인포뱅크).
 * 매핑 결과는 send_table.routing_meta 에만 적재되어 외부 발송 시스템이 사용한다.
 * 회원에게는 어떤 형태로도 노출하지 않는다 (응답·UI·로그·이력).
 *
 * RQ-ADMIN-401~405, RQ-SEND-308
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingMappingService {

    private final RoutingMappingRepository routingMappingRepository;
    private final MemberRepository memberRepository;

    /**
     * 라우팅 매핑 등록 또는 수정 (Upsert).
     * 동일 (memberId, channel) 이 존재하면 carrier·memo 를 갱신한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public RoutingMappingDto.Response upsert(Long operatorId, RoutingMappingDto.UpsertRequest request) {
        validateMemberExists(request.memberId());

        RoutingMapping mapping = routingMappingRepository
                .findByMemberIdAndChannel(request.memberId(), request.channel())
                .map(existing -> {
                    existing.updateCarrier(request.carrier(), request.memo(), operatorId);
                    return existing;
                })
                .orElseGet(() -> routingMappingRepository.save(
                        RoutingMapping.builder()
                                .memberId(request.memberId())
                                .channel(request.channel())
                                .carrier(request.carrier())
                                .memo(request.memo())
                                .operatorId(operatorId)
                                .build()));

        log.info("[라우팅 매핑 upsert] memberId={} channel={} carrier={} operatorId={}",
                request.memberId(), request.channel(), request.carrier(), operatorId);

        return RoutingMappingDto.Response.from(mapping);
    }

    /**
     * 회원의 전체 채널 매핑 조회.
     * 운영자 전용 — 회원 API 에서는 이 메서드를 호출하지 않는다.
     */
    @Transactional(readOnly = true)
    public List<RoutingMappingDto.Response> listByMember(Long memberId) {
        return routingMappingRepository.findByMemberId(memberId)
                .stream()
                .map(RoutingMappingDto.Response::from)
                .toList();
    }

    /**
     * 채널별 전체 매핑 목록 (운영자 일괄 조회).
     */
    @Transactional(readOnly = true)
    public List<RoutingMappingDto.Response> listByChannel(RoutingChannel channel) {
        return routingMappingRepository.findByChannel(channel)
                .stream()
                .map(RoutingMappingDto.Response::from)
                .toList();
    }

    /**
     * 라우팅 매핑 삭제.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long mappingId, Long operatorId) {
        RoutingMapping mapping = routingMappingRepository.findById(mappingId)
                .orElseThrow(() -> new EntityNotFoundException("RoutingMapping", mappingId));

        routingMappingRepository.delete(mapping);
        log.info("[라우팅 매핑 삭제] mappingId={} memberId={} channel={} operatorId={}",
                mappingId, mapping.getMemberId(), mapping.getChannel(), operatorId);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("Member", memberId);
        }
    }
}

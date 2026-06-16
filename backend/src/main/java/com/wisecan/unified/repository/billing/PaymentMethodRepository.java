package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.PaymentMethod;
import com.wisecan.unified.domain.billing.PaymentMethodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByMemberIdAndActiveYn(Long memberId, String activeYn);

    Optional<PaymentMethod> findByMemberIdAndDefaultYnAndActiveYn(Long memberId, String defaultYn, String activeYn);

    boolean existsByMemberIdAndMethodTypeAndActiveYn(Long memberId, PaymentMethodType methodType, String activeYn);
}

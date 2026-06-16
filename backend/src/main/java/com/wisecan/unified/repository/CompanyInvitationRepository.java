package com.wisecan.unified.repository;

import com.wisecan.unified.domain.CompanyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyInvitationRepository extends JpaRepository<CompanyInvitation, Long> {

    Optional<CompanyInvitation> findByTokenHash(String tokenHash);

    boolean existsByInviteeEmailAndCompanyIdAndStatus(String inviteeEmail, Long companyId, String status);
}

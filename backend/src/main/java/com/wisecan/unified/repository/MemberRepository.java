package com.wisecan.unified.repository;

import com.wisecan.unified.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 아이디 찾기 — 이름 + 휴대폰 번호 조합 */
    Optional<Member> findByNameAndPhone(String name, String phone);
}

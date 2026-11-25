package com.ridematch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridematch.domain.entity.CommunityMembership;
import com.ridematch.domain.enums.VerificationStatus;

/** Persistence operations for user verification within communities. */
public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, Long> {

    java.util.Optional<CommunityMembership> findByUser_IdAndCommunity_Id(Long userId, Long communityId);

    List<CommunityMembership> findByUser_IdAndVerificationStatus(Long userId, VerificationStatus verificationStatus);

    boolean existsByUser_IdAndCommunity_IdAndVerificationStatus(
            Long userId, Long communityId, VerificationStatus verificationStatus);

    List<CommunityMembership> findByCommunity_IdAndVerificationStatusOrderByCreatedAt(
            Long communityId, VerificationStatus verificationStatus);

    long countByCommunity_IdAndVerificationStatus(Long communityId, VerificationStatus verificationStatus);

    List<CommunityMembership> findByVerificationStatusOrderByCreatedAt(VerificationStatus verificationStatus);
}

package com.ridematch.dto.community;

import java.time.LocalDateTime;

import com.ridematch.domain.entity.CommunityMembership;
import com.ridematch.domain.enums.VerificationStatus;

/** Membership status view; applicant identity is provided only on admin pages. */
public record MembershipRequestResponse(Long id, Long communityId, String communityName,
        String memberName, String memberEmail, VerificationStatus status, LocalDateTime requestedAt) {

    public static MembershipRequestResponse from(CommunityMembership membership) {
        return new MembershipRequestResponse(
                membership.getId(),
                membership.getCommunity().getId(),
                membership.getCommunity().getName(),
                membership.getUser().getFullName(),
                membership.getUser().getEmail(),
                membership.getVerificationStatus(),
                membership.getCreatedAt());
    }
}

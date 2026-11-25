package com.ridematch.dto.ride;

import java.time.LocalDateTime;

import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.enums.RideMembershipStatus;

/** Safe group member view; does not expose account credentials or email addresses. */
public record RideMemberResponse(Long membershipId, Long userId, String fullName,
        RideMembershipStatus status, LocalDateTime joinedAt) {

    public static RideMemberResponse from(RideMembership membership) {
        return new RideMemberResponse(membership.getId(), membership.getUser().getId(),
                membership.getUser().getFullName(), membership.getStatus(), membership.getJoinedAt());
    }
}

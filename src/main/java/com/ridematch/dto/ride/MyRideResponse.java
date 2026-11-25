package com.ridematch.dto.ride;

import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.enums.RideMembershipStatus;

/** Current user's active ride entry for dashboard and ride list pages. */
public record MyRideResponse(RideViewResponse ride, RideMembershipStatus membershipStatus, boolean coordinator) {

    public static MyRideResponse from(RideMembership membership, int confirmedPassengers, Long currentUserId) {
        return new MyRideResponse(RideViewResponse.from(membership.getRide(), confirmedPassengers),
                membership.getStatus(), membership.getRide().getCoordinator().getId().equals(currentUserId));
    }
}

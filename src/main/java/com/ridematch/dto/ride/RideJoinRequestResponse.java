package com.ridematch.dto.ride;

import java.time.LocalDateTime;

import com.ridematch.domain.entity.RideMembership;

/** Safe coordinator inbox row for a passenger's pending ride request. */
public record RideJoinRequestResponse(Long membershipId, Long rideId, String communityName,
        String pickupLocation, String destination, LocalDateTime requestedTime, String requesterName) {

    public static RideJoinRequestResponse from(RideMembership membership) {
        var ride = membership.getRide();
        return new RideJoinRequestResponse(membership.getId(), ride.getId(), ride.getCommunity().getName(),
                ride.getOrigin(), ride.getDestination(), ride.getRequestedTime(),
                membership.getUser().getFullName());
    }
}

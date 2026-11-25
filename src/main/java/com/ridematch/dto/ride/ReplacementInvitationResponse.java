package com.ridematch.dto.ride;

import java.time.LocalDate;
import java.time.LocalTime;

import com.ridematch.domain.entity.RideMembership;

/** Safe invitation details for the invited verified community member. */
public record ReplacementInvitationResponse(Long membershipId, Long rideId, Long communityId,
        String pickupLocation, String destination, LocalDate travelDate, LocalTime departureTime,
        String coordinatorName) {

    public static ReplacementInvitationResponse from(RideMembership membership) {
        var ride = membership.getRide();
        return new ReplacementInvitationResponse(membership.getId(), ride.getId(), ride.getCommunity().getId(),
                ride.getOrigin(), ride.getDestination(), ride.getRequestedTime().toLocalDate(),
                ride.getRequestedTime().toLocalTime(), ride.getCoordinator().getFullName());
    }
}

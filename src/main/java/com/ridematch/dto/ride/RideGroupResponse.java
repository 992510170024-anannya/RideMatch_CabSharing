package com.ridematch.dto.ride;

import java.util.List;

/** Group page data for a verified member of the ride's community. */
public record RideGroupResponse(RideViewResponse ride, FareSplitResponse fareSplit,
        List<RideMemberResponse> memberships, Long currentUserId, Long coordinatorUserId, boolean coordinator) {
}

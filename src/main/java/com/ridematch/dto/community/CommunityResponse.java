package com.ridematch.dto.community;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.enums.CommunityType;

/** Safe community list representation. */
public record CommunityResponse(Long id, String name, CommunityType type) {

    public static CommunityResponse from(Community community) {
        return new CommunityResponse(community.getId(), community.getName(), community.getType());
    }
}

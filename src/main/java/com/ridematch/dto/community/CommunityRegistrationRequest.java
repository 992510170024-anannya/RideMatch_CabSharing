package com.ridematch.dto.community;

import com.ridematch.domain.enums.CommunityType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin form for adding a college or company community. */
public class CommunityRegistrationRequest {

    @NotBlank(message = "Community name is required")
    @Size(max = 160, message = "Community name must be at most 160 characters")
    private String name;

    @NotNull(message = "Choose a community type")
    private CommunityType type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CommunityType getType() { return type; }
    public void setType(CommunityType type) { this.type = type; }
}

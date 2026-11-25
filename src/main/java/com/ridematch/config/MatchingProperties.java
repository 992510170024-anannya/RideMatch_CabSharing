package com.ridematch.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurable, non-map matching thresholds and destination alias groups. */
@ConfigurationProperties(prefix = "ridematch.matching")
public class MatchingProperties {

    private Duration maximumDepartureDifference = Duration.ofMinutes(15);
    private Duration replacementSearchTimeout = Duration.ofMinutes(2);
    private List<String> nearbyDestinationGroups = new ArrayList<>();

    public Duration getMaximumDepartureDifference() {
        return maximumDepartureDifference;
    }

    public void setMaximumDepartureDifference(Duration maximumDepartureDifference) {
        this.maximumDepartureDifference = maximumDepartureDifference;
    }

    public Duration getReplacementSearchTimeout() {
        return replacementSearchTimeout;
    }

    public void setReplacementSearchTimeout(Duration replacementSearchTimeout) {
        this.replacementSearchTimeout = replacementSearchTimeout;
    }

    public List<String> getNearbyDestinationGroups() {
        return nearbyDestinationGroups;
    }

    public void setNearbyDestinationGroups(List<String> nearbyDestinationGroups) {
        this.nearbyDestinationGroups = nearbyDestinationGroups == null
                ? new ArrayList<>() : new ArrayList<>(nearbyDestinationGroups);
    }
}

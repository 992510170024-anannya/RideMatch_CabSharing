package com.ridematch.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurable time window used to flag potential external cancellation loss for review. */
@ConfigurationProperties(prefix = "ridematch.cancellation")
public class CancellationPolicyProperties {

    private Duration potentialLossReviewWindow = Duration.ofHours(24);

    public Duration getPotentialLossReviewWindow() {
        return potentialLossReviewWindow;
    }

    public void setPotentialLossReviewWindow(Duration potentialLossReviewWindow) {
        this.potentialLossReviewWindow = potentialLossReviewWindow;
    }
}

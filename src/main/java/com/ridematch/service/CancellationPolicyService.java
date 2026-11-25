package com.ridematch.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.ridematch.config.CancellationPolicyProperties;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.enums.CancellationPolicyDecision;
import com.ridematch.domain.enums.RideStatus;
import com.ridematch.dto.ride.CancellationPolicyResult;

/** Assesses a possible cancellation consequence; it never calculates or collects a charge. */
@Service
public class CancellationPolicyService {

    private final CancellationPolicyProperties properties;

    public CancellationPolicyService(CancellationPolicyProperties properties) {
        this.properties = properties;
    }

    public CancellationPolicyResult assess(Ride ride, LocalDateTime cancelledAt) {
        if (ride.getStatus() != RideStatus.TAXI_ARRANGED) {
            return new CancellationPolicyResult(CancellationPolicyDecision.NO_POTENTIAL_LOSS,
                    "No external taxi arrangement is recorded; no financial consequence is assessed.");
        }

        Duration timeUntilDeparture = Duration.between(cancelledAt, ride.getRequestedTime());
        if (timeUntilDeparture.compareTo(properties.getPotentialLossReviewWindow()) <= 0) {
            return new CancellationPolicyResult(CancellationPolicyDecision.REVIEW_ACTUAL_LOSS,
                    "An external taxi was recorded and cancellation is within the configured review window. "
                    + "Review actual financial loss under the applicable policy; no penalty is charged by RideMatch.");
        }

        return new CancellationPolicyResult(CancellationPolicyDecision.NO_POTENTIAL_LOSS,
                "Cancellation is outside the configured review window; no financial consequence is assessed.");
    }
}

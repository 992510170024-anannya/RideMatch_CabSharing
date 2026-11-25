package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ridematch.config.CancellationPolicyProperties;
import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CancellationPolicyDecision;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.RideKind;

class CancellationPolicyServiceTest {

    private CancellationPolicyService policyService;
    private Ride ride;

    @BeforeEach
    void setUp() {
        CancellationPolicyProperties properties = new CancellationPolicyProperties();
        properties.setPotentialLossReviewWindow(Duration.ofHours(24));
        policyService = new CancellationPolicyService(properties);
        ride = new Ride(new Community("North College", CommunityType.COLLEGE, null),
                new User("Coordinator", "coord@example.com", "hash"), "Campus", "Airport",
                RideKind.STANDARD, LocalDateTime.now().plusHours(2), new BigDecimal("400.00"), 4, null);
    }

    @Test
    void noExternalBookingProducesNoPotentialLossAssessment() {
        var result = policyService.assess(ride, LocalDateTime.now());

        assertEquals(CancellationPolicyDecision.NO_POTENTIAL_LOSS, result.decision());
    }

    @Test
    void taxiArrangedWithinWindowFlagsReviewWithoutCharging() {
        ride.markTaxiArranged();

        var result = policyService.assess(ride, LocalDateTime.now());

        assertEquals(CancellationPolicyDecision.REVIEW_ACTUAL_LOSS, result.decision());
        org.junit.jupiter.api.Assertions.assertTrue(result.explanation().contains("no penalty is charged"));
    }

    @Test
    void taxiArrangedOutsideWindowHasNoPotentialLossAssessment() {
        Ride laterRide = new Ride(ride.getCommunity(), ride.getCreator(), "Campus", "Airport",
                RideKind.STANDARD, LocalDateTime.now().plusHours(48), new BigDecimal("400.00"), 4, null);
        laterRide.markTaxiArranged();

        var result = policyService.assess(laterRide, LocalDateTime.now());

        assertEquals(CancellationPolicyDecision.NO_POTENTIAL_LOSS, result.decision());
    }
}

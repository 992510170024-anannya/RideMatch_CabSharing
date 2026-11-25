package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.RideKind;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.repository.RideMembershipRepository;

@ExtendWith(MockitoExtension.class)
class FareSplitServiceTest {

    @Mock
    private RideMembershipRepository membershipRepository;

    private FareSplitService fareSplitService;
    private Ride ride;

    @BeforeEach
    void setUp() {
        fareSplitService = new FareSplitService(membershipRepository);
        Community community = new Community("North College", CommunityType.COLLEGE, null);
        User creator = new User("Ride Coordinator", "coord@example.com", "hash");
        ride = new Ride(community, creator, "Campus", "Airport", RideKind.STANDARD,
                LocalDateTime.of(2026, 12, 1, 10, 0), new BigDecimal("400.00"), 4, null);
        ReflectionTestUtils.setField(ride, "id", 8L);
    }

    @Test
    void dividesEstimatedFareEquallyAcrossConfirmedPassengers() {
        when(membershipRepository.countByRide_IdAndStatus(8L, RideMembershipStatus.CONFIRMED)).thenReturn(4L);

        var split = fareSplitService.calculate(ride);

        assertEquals(new BigDecimal("400.00"), split.estimatedTotalFare());
        assertEquals(4, split.confirmedPassengerCount());
        assertEquals(new BigDecimal("100.00"), split.estimatedContributionEach());
        org.junit.jupiter.api.Assertions.assertTrue(split.explanation().contains("actual taxi fare"));
    }

    @Test
    void pendingRequestsDoNotChangeFareSplitUntilConfirmed() {
        when(membershipRepository.countByRide_IdAndStatus(8L, RideMembershipStatus.CONFIRMED)).thenReturn(2L);

        var split = fareSplitService.calculate(ride);

        assertEquals(new BigDecimal("200.00"), split.estimatedContributionEach());
    }
}

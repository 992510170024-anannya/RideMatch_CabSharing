package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.dto.ride.RideCreateRequest;
import com.ridematch.repository.CommunityRepository;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;
import com.ridematch.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private RideMembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityService communityService;
    @Mock private RideMatchingService matchingService;

    private RideService rideService;
    private Community community;
    private User coordinator;

    @BeforeEach
    void setUp() {
        rideService = new RideService(rideRepository, membershipRepository, userRepository,
                communityRepository, communityService, matchingService);
        community = new Community("North College", CommunityType.COLLEGE, null);
        ReflectionTestUtils.setField(community, "id", 7L);
        coordinator = new User("Alex Rider", "alex@example.com", "bcrypt-hash");
        ReflectionTestUtils.setField(coordinator, "id", 9L);
    }

    @Test
    void createsRideWithCoordinatorAsFirstConfirmedPassenger() {
        RideCreateRequest request = new RideCreateRequest();
        request.setPickupLocation("North Campus");
        request.setDestination("Sector 99");
        request.setTravelDate(LocalDate.now().plusDays(2));
        request.setDepartureTime(LocalTime.of(17, 30));
        request.setPassengerCapacity(4);
        request.setEstimatedTotalFare(new BigDecimal("400.00"));
        request.setNotes("Main gate pickup");
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(coordinator));
        when(communityRepository.findById(7L)).thenReturn(Optional.of(community));
        when(rideRepository.saveAndFlush(any(Ride.class))).thenAnswer(invocation -> {
            Ride savedRide = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedRide, "id", 23L);
            return savedRide;
        });
        when(membershipRepository.countByRide_IdAndStatus(23L, RideMembershipStatus.CONFIRMED)).thenReturn(1L);

        var response = rideService.createRide("alex@example.com", 7L, request);

        assertEquals(23L, response.id());
        assertEquals(3, response.availableSeats());
        assertEquals(new BigDecimal("400.00"), response.estimatedTotalFare());
        org.mockito.ArgumentCaptor<RideMembership> membershipCaptor =
                org.mockito.ArgumentCaptor.forClass(RideMembership.class);
        verify(membershipRepository).saveAndFlush(membershipCaptor.capture());
        assertEquals(RideMembershipStatus.CONFIRMED, membershipCaptor.getValue().getStatus());
    }
}

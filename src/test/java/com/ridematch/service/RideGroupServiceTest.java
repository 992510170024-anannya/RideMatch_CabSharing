package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.ridematch.domain.enums.RideKind;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.domain.entity.CancellationRecord;
import com.ridematch.dto.ride.CancellationPolicyResult;
import com.ridematch.exception.ConflictException;
import com.ridematch.exception.ForbiddenOperationException;
import com.ridematch.repository.CancellationRecordRepository;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;
import com.ridematch.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RideGroupServiceTest {

    private static final Long RIDE_ID = 4L;

    @Mock private RideRepository rideRepository;
    @Mock private RideMembershipRepository membershipRepository;
    @Mock private CancellationRecordRepository cancellationRecordRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommunityService communityService;
    @Mock private NotificationService notificationService;
    @Mock private ReplacementSearchService replacementSearchService;
    @Mock private CancellationPolicyService cancellationPolicyService;
    @Mock private FareSplitService fareSplitService;

    private RideGroupService rideGroupService;
    private Community community;
    private User coordinator;
    private User passenger;
    private Ride ride;

    @BeforeEach
    void setUp() {
        rideGroupService = new RideGroupService(rideRepository, membershipRepository,
                cancellationRecordRepository, userRepository, communityService,
                notificationService, replacementSearchService, cancellationPolicyService, fareSplitService);
        community = new Community("North College", CommunityType.COLLEGE, null);
        ReflectionTestUtils.setField(community, "id", 3L);
        coordinator = new User("Coordinator", "coord@example.com", "hash");
        passenger = new User("Passenger", "passenger@example.com", "hash");
        ReflectionTestUtils.setField(coordinator, "id", 1L);
        ReflectionTestUtils.setField(passenger, "id", 2L);
        ride = new Ride(community, coordinator, "Campus", "Airport", RideKind.STANDARD,
                LocalDateTime.of(2026, 12, 1, 10, 0), new BigDecimal("400.00"), 2, null);
        ReflectionTestUtils.setField(ride, "id", RIDE_ID);
    }

    @Test
    void joinRequestIsPendingAndDoesNotOccupyAConfirmedSeat() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(membershipRepository.countByRide_IdAndStatus(RIDE_ID, RideMembershipStatus.CONFIRMED)).thenReturn(1L);
        when(membershipRepository.findByRide_IdAndUser_Id(RIDE_ID, 2L)).thenReturn(Optional.empty());
        when(membershipRepository.saveAndFlush(any(RideMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        rideGroupService.requestToJoin("passenger@example.com", RIDE_ID);

        org.mockito.ArgumentCaptor<RideMembership> captor = org.mockito.ArgumentCaptor.forClass(RideMembership.class);
        verify(membershipRepository).saveAndFlush(captor.capture());
        assertEquals(RideMembershipStatus.REQUESTED, captor.getValue().getStatus());
    }

    @Test
    void unverifiedMemberCannotRequestToJoin() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        doThrow(new ForbiddenOperationException("Approved membership is required"))
                .when(communityService).requireApprovedMember("passenger@example.com", 3L);

        assertThrows(ForbiddenOperationException.class,
                () -> rideGroupService.requestToJoin("passenger@example.com", RIDE_ID));
        verify(membershipRepository, never()).saveAndFlush(any(RideMembership.class));
    }

    @Test
    void duplicateRideMembershipIsRejected() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(membershipRepository.countByRide_IdAndStatus(RIDE_ID, RideMembershipStatus.CONFIRMED)).thenReturn(1L);
        when(membershipRepository.findByRide_IdAndUser_Id(RIDE_ID, 2L))
                .thenReturn(Optional.of(RideMembership.requestToJoin(ride, passenger)));

        assertThrows(ConflictException.class,
                () -> rideGroupService.requestToJoin("passenger@example.com", RIDE_ID));
        verify(membershipRepository, never()).saveAndFlush(any(RideMembership.class));
    }

    @Test
    void coordinatorCanApproveRequestWhenASeatIsAvailable() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        when(userRepository.findByEmail("coord@example.com")).thenReturn(Optional.of(coordinator));
        RideMembership request = RideMembership.requestToJoin(ride, passenger);
        ReflectionTestUtils.setField(request, "id", 21L);
        when(membershipRepository.findByIdAndRide_Id(21L, RIDE_ID)).thenReturn(Optional.of(request));
        when(membershipRepository.countByRide_IdAndStatus(RIDE_ID, RideMembershipStatus.CONFIRMED)).thenReturn(1L);

        rideGroupService.acceptJoinRequest("coord@example.com", RIDE_ID, 21L);

        assertEquals(RideMembershipStatus.CONFIRMED, request.getStatus());
        assertEquals(com.ridematch.domain.enums.RideStatus.COORDINATING, ride.getStatus());
        verify(membershipRepository).save(request);
    }

    @Test
    void coordinatorCannotAcceptRequestWhenRideIsFull() {
        Ride fullRide = new Ride(community, coordinator, "Campus", "Airport", RideKind.STANDARD,
                LocalDateTime.of(2026, 12, 1, 10, 0), new BigDecimal("400.00"), 1, null);
        ReflectionTestUtils.setField(fullRide, "id", RIDE_ID);
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(fullRide));
        when(userRepository.findByEmail("coord@example.com")).thenReturn(Optional.of(coordinator));
        RideMembership request = RideMembership.requestToJoin(fullRide, passenger);
        when(membershipRepository.findByIdAndRide_Id(20L, RIDE_ID)).thenReturn(Optional.of(request));
        when(membershipRepository.countByRide_IdAndStatus(RIDE_ID, RideMembershipStatus.CONFIRMED)).thenReturn(1L);

        assertThrows(ConflictException.class,
                () -> rideGroupService.acceptJoinRequest("coord@example.com", RIDE_ID, 20L));
        verify(membershipRepository, never()).save(request);
    }

    @Test
    void confirmedPassengerCancellationCreatesHistoryAndStartsReplacementSearch() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        RideMembership membership = new RideMembership(ride, passenger, LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(membership, "id", 22L);
        when(membershipRepository.findByIdAndRide_Id(22L, RIDE_ID)).thenReturn(Optional.of(membership));
        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(cancellationPolicyService.assess(eq(ride), any(LocalDateTime.class)))
                .thenReturn(new CancellationPolicyResult(
                        com.ridematch.domain.enums.CancellationPolicyDecision.NO_POTENTIAL_LOSS,
                        "No external booking is recorded."));

        rideGroupService.cancelOwnMembership("passenger@example.com", RIDE_ID, 22L, "Plans changed");

        assertEquals(RideMembershipStatus.CANCELLED, membership.getStatus());
        verify(cancellationRecordRepository).save(any(CancellationRecord.class));
        verify(replacementSearchService).startSearch(ride, membership);
    }
}

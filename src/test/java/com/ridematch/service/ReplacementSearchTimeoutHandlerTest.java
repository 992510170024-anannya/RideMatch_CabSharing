package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.ReplacementSearch;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.NotificationType;
import com.ridematch.domain.enums.ReplacementSearchStatus;
import com.ridematch.domain.enums.RideKind;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.repository.ReplacementSearchRepository;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;

@ExtendWith(MockitoExtension.class)
class ReplacementSearchTimeoutHandlerTest {

    @Mock private ReplacementSearchRepository searchRepository;
    @Mock private RideMembershipRepository membershipRepository;
    @Mock private RideRepository rideRepository;
    @Mock private NotificationService notificationService;

    @Test
    void timeoutClosesInvitationAndNotifiesGroup() {
        Community community = new Community("North College", CommunityType.COLLEGE, null);
        User coordinator = new User("Coordinator", "coord@example.com", "hash");
        ReflectionTestUtils.setField(coordinator, "id", 1L);
        Ride ride = new Ride(community, coordinator, "Campus", "Airport", RideKind.STANDARD,
                LocalDateTime.now().plusHours(1), new BigDecimal("400.00"), 3, null);
        ReflectionTestUtils.setField(ride, "id", 4L);
        RideMembership cancelled = new RideMembership(ride,
                new User("Leaving", "leaving@example.com", "hash"), LocalDateTime.now().minusHours(1));
        cancelled.cancel(LocalDateTime.now());
        ReplacementSearch search = new ReplacementSearch(ride, cancelled,
                LocalDateTime.now().minusMinutes(3), LocalDateTime.now().minusMinutes(1));
        ReflectionTestUtils.setField(search, "id", 50L);
        User invitee = new User("Invitee", "invitee@example.com", "hash");
        RideMembership invitation = RideMembership.inviteAsReplacement(ride, invitee, search);

        when(searchRepository.findById(50L)).thenReturn(Optional.of(search));
        when(rideRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(ride));
        when(searchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(search));
        when(membershipRepository.findByReplacementSearch_IdAndStatus(50L, RideMembershipStatus.INVITED))
                .thenReturn(List.of(invitation));

        ReplacementSearchTimeoutHandler handler = new ReplacementSearchTimeoutHandler(
                searchRepository, membershipRepository, rideRepository, notificationService);

        handler.expire(50L);

        assertEquals(ReplacementSearchStatus.TIMED_OUT, search.getStatus());
        assertEquals(RideMembershipStatus.CANCELLED, invitation.getStatus());
        verify(searchRepository).save(search);
        verify(notificationService).notifyUser(invitee, ride, NotificationType.REPLACEMENT_RESULT,
                "Replacement invitation expired", "The replacement search ended before the invitation was accepted.");
    }

    @Test
    void earlySchedulerCallbackReturnsPersistedDeadlineForRescheduling() {
        Community community = new Community("North College", CommunityType.COLLEGE, null);
        User coordinator = new User("Coordinator", "coord@example.com", "hash");
        ReflectionTestUtils.setField(coordinator, "id", 1L);
        Ride ride = new Ride(community, coordinator, "Campus", "Airport", RideKind.STANDARD,
                LocalDateTime.now().plusHours(1), new BigDecimal("400.00"), 3, null);
        ReflectionTestUtils.setField(ride, "id", 4L);
        RideMembership cancelled = new RideMembership(ride,
                new User("Leaving", "leaving@example.com", "hash"), LocalDateTime.now().minusHours(1));
        cancelled.cancel(LocalDateTime.now());
        ReplacementSearch search = new ReplacementSearch(ride, cancelled,
                LocalDateTime.now().minusSeconds(1), LocalDateTime.now().plusSeconds(30));
        ReflectionTestUtils.setField(search, "id", 51L);
        when(searchRepository.findById(51L)).thenReturn(Optional.of(search));
        when(rideRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(ride));
        when(searchRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(search));

        ReplacementSearchTimeoutHandler handler = new ReplacementSearchTimeoutHandler(
                searchRepository, membershipRepository, rideRepository, notificationService);

        Instant nextDeadline = handler.expire(51L);

        assertEquals(search.getDeadline().atZone(java.time.ZoneId.systemDefault()).toInstant(), nextDeadline);
        verify(searchRepository, never()).save(search);
    }
}

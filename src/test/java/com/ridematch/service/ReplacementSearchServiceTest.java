package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridematch.config.MatchingProperties;
import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.CommunityMembership;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.RideKind;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.domain.enums.VerificationStatus;
import com.ridematch.repository.CommunityMembershipRepository;
import com.ridematch.repository.ReplacementSearchRepository;
import com.ridematch.repository.RideMembershipRepository;

@ExtendWith(MockitoExtension.class)
class ReplacementSearchServiceTest {

    @Mock private ReplacementSearchRepository searchRepository;
    @Mock private RideMembershipRepository membershipRepository;
    @Mock private CommunityMembershipRepository communityMembershipRepository;
    @Mock private NotificationService notificationService;
    @Mock private TaskScheduler taskScheduler;
    @Mock private ReplacementSearchTimeoutHandler timeoutHandler;

    private ReplacementSearchService searchService;
    private Ride ride;
    private RideMembership cancelledMembership;
    private RideMembership invitation;
    private com.ridematch.domain.entity.ReplacementSearch savedSearch;
    private User candidate;

    @BeforeEach
    void setUp() {
        MatchingProperties properties = new MatchingProperties();
        properties.setReplacementSearchTimeout(Duration.ofMinutes(2));
        searchService = new ReplacementSearchService(searchRepository, membershipRepository,
                communityMembershipRepository, notificationService, properties, taskScheduler, timeoutHandler);

        Community community = new Community("North College", CommunityType.COLLEGE, null);
        ReflectionTestUtils.setField(community, "id", 3L);
        User coordinator = new User("Coordinator", "coord@example.com", "hash");
        ReflectionTestUtils.setField(coordinator, "id", 1L);
        candidate = new User("Candidate", "candidate@example.com", "hash");
        ReflectionTestUtils.setField(candidate, "id", 9L);
        ride = new Ride(community, coordinator, "Campus", "Airport", RideKind.STANDARD,
                LocalDateTime.now().plusHours(1), new BigDecimal("400.00"), 3, null);
        ReflectionTestUtils.setField(ride, "id", 4L);
        cancelledMembership = new RideMembership(ride,
                new User("Leaving Passenger", "leaving@example.com", "hash"), LocalDateTime.now().minusMinutes(5));
        cancelledMembership.cancel(LocalDateTime.now());
        ReflectionTestUtils.setField(cancelledMembership, "id", 5L);

        CommunityMembership verifiedCandidate = new CommunityMembership(candidate, community);
        verifiedCandidate.verify(coordinator, LocalDateTime.now());
        when(searchRepository.saveAndFlush(any(com.ridematch.domain.entity.ReplacementSearch.class)))
                .thenAnswer(invocation -> {
                    savedSearch = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedSearch, "id", 50L);
                    return savedSearch;
                });
        when(searchRepository.findByIdForUpdate(50L)).thenAnswer(invocation -> Optional.of(savedSearch));
        when(membershipRepository.countByRide_IdAndStatus(4L, RideMembershipStatus.CONFIRMED)).thenReturn(1L);
        when(membershipRepository.findByRide_IdOrderByCreatedAtAsc(4L))
                .thenReturn(List.of(cancelledMembership));
        when(communityMembershipRepository.findByCommunity_IdAndVerificationStatusOrderByCreatedAt(
                3L, VerificationStatus.VERIFIED)).thenReturn(List.of(verifiedCandidate));
        when(membershipRepository.saveAndFlush(any(RideMembership.class))).thenAnswer(invocation -> {
            invitation = invocation.getArgument(0);
            return invitation;
        });
    }

    @Test
    void replacementIsAnInvitationAndDoesNotOccupyAConfirmedSeatUntilAccepted() {
        var search = searchService.startSearch(ride, cancelledMembership);

        assertEquals(50L, search.getId());
        assertEquals(RideMembershipStatus.INVITED, invitation.getStatus());
        assertEquals(search.getId(), invitation.getReplacementSearch().getId());
        verify(taskScheduler).schedule(any(Runnable.class), any(java.time.Instant.class));
        verify(notificationService).notifyUser(candidate, ride,
                com.ridematch.domain.enums.NotificationType.REPLACEMENT_INVITATION,
                "Replacement seat invitation", "A seat opened in a community ride. Accept or reject the invitation before its search deadline.");
    }
}

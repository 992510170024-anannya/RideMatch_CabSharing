package com.ridematch.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.config.MatchingProperties;
import com.ridematch.domain.entity.CommunityMembership;
import com.ridematch.domain.entity.ReplacementSearch;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.enums.NotificationType;
import com.ridematch.domain.enums.ReplacementSearchStatus;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.domain.enums.RideStatus;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.repository.CommunityMembershipRepository;
import com.ridematch.repository.ReplacementSearchRepository;
import com.ridematch.repository.RideMembershipRepository;

/** Creates invitations sequentially and uses an in-process timer for the persisted deadline. */
@Service
public class ReplacementSearchService {

    private final ReplacementSearchRepository searchRepository;
    private final RideMembershipRepository membershipRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final NotificationService notificationService;
    private final MatchingProperties matchingProperties;
    private final TaskScheduler taskScheduler;
    private final ReplacementSearchTimeoutHandler timeoutHandler;

    public ReplacementSearchService(ReplacementSearchRepository searchRepository,
            RideMembershipRepository membershipRepository,
            CommunityMembershipRepository communityMembershipRepository,
            NotificationService notificationService, MatchingProperties matchingProperties,
            TaskScheduler taskScheduler, ReplacementSearchTimeoutHandler timeoutHandler) {
        this.searchRepository = searchRepository;
        this.membershipRepository = membershipRepository;
        this.communityMembershipRepository = communityMembershipRepository;
        this.notificationService = notificationService;
        this.matchingProperties = matchingProperties;
        this.taskScheduler = taskScheduler;
        this.timeoutHandler = timeoutHandler;
    }

    @Transactional
    public ReplacementSearch startSearch(Ride ride, RideMembership cancelledMembership) {
        Duration timeout = matchingProperties.getReplacementSearchTimeout();
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalStateException("The replacement search timeout must be positive");
        }
        Instant startedInstant = Instant.now();
        Instant deadlineInstant = startedInstant.plus(timeout);
        ZoneId zone = ZoneId.systemDefault();
        ReplacementSearch search = new ReplacementSearch(ride, cancelledMembership,
                LocalDateTime.ofInstant(startedInstant, zone), LocalDateTime.ofInstant(deadlineInstant, zone));
        ReplacementSearch savedSearch = searchRepository.saveAndFlush(search);
        scheduleTimeout(savedSearch.getId(), deadlineInstant);
        inviteNextCandidate(savedSearch.getId());
        return savedSearch;
    }

    @Transactional
    public void invitationDeclined(Long searchId) {
        inviteNextCandidate(searchId);
    }

    @Transactional
    public void closeSearchesIfCapacityReached(Ride ride) {
        long confirmed = membershipRepository.countByRide_IdAndStatus(
                ride.getId(), RideMembershipStatus.CONFIRMED);
        if (confirmed < ride.getPassengerCapacity()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (ReplacementSearch candidateSearch : searchRepository.findByRide_IdAndStatus(
                ride.getId(), ReplacementSearchStatus.SEARCHING)) {
            ReplacementSearch search = searchRepository.findByIdForUpdate(candidateSearch.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Replacement search was not found"));
            if (search.getStatus() != ReplacementSearchStatus.SEARCHING) {
                continue;
            }
            for (RideMembership invitation : membershipRepository.findByReplacementSearch_IdAndStatus(
                    search.getId(), RideMembershipStatus.INVITED)) {
                invitation.cancel(now);
                membershipRepository.save(invitation);
                notificationService.notifyUser(invitation.getUser(), ride, NotificationType.REPLACEMENT_RESULT,
                        "Replacement invitation closed", "The ride reached capacity before you responded.");
            }
            search.markFilled(now);
            searchRepository.save(search);
        }
        notificationService.notifyConfirmedPassengers(ride, List.of(), NotificationType.REPLACEMENT_RESULT,
                "Replacement search closed", "The ride reached passenger capacity, so remaining replacement searches ended.");
    }

    @Transactional
    public ReplacementSearch lockSearch(Long searchId) {
        return searchRepository.findByIdForUpdate(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Replacement search was not found"));
    }

    @Transactional
    public ReplacementSearch saveSearch(ReplacementSearch search) {
        return searchRepository.save(search);
    }

    @Transactional
    public void inviteNextCandidate(Long searchId) {
        ReplacementSearch search = searchRepository.findByIdForUpdate(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Replacement search was not found"));
        if (search.getStatus() != ReplacementSearchStatus.SEARCHING) {
            return;
        }
        if (!LocalDateTime.now().isBefore(search.getDeadline())) {
            // The persisted deadline task owns timeout processing. Do not acquire the ride lock
            // here while holding the search lock; ride operations consistently lock ride first.
            return;
        }

        Ride ride = search.getRide();
        if (ride.getStatus() == RideStatus.CANCELLED || ride.getStatus() == RideStatus.COMPLETED) {
            search.cancel(LocalDateTime.now());
            searchRepository.save(search);
            return;
        }

        long confirmed = membershipRepository.countByRide_IdAndStatus(
                ride.getId(), RideMembershipStatus.CONFIRMED);
        if (confirmed >= ride.getPassengerCapacity()) {
            search.markFilled(LocalDateTime.now());
            searchRepository.save(search);
            notificationService.notifyConfirmedPassengers(ride, List.of(),
                    NotificationType.REPLACEMENT_RESULT, "Replacement seat filled",
                    "The vacant seat was filled before another replacement invitation was needed.");
            return;
        }

        Set<Long> existingUserIds = new HashSet<>();
        for (RideMembership membership : membershipRepository.findByRide_IdOrderByCreatedAtAsc(ride.getId())) {
            existingUserIds.add(membership.getUser().getId());
        }
        List<CommunityMembership> eligibleMembers = communityMembershipRepository
                .findByCommunity_IdAndVerificationStatusOrderByCreatedAt(
                        ride.getCommunity().getId(), com.ridematch.domain.enums.VerificationStatus.VERIFIED);

        for (CommunityMembership communityMembership : eligibleMembers) {
            var candidate = communityMembership.getUser();
            if (!candidate.isActive() || existingUserIds.contains(candidate.getId())) {
                continue;
            }
            RideMembership invitation = RideMembership.inviteAsReplacement(ride, candidate, search);
            membershipRepository.saveAndFlush(invitation);
            notificationService.notifyUser(candidate, ride, NotificationType.REPLACEMENT_INVITATION,
                    "Replacement seat invitation",
                    "A seat opened in a community ride. Accept or reject the invitation before its search deadline.");
            return;
        }
    }

    @Transactional
    public void cancelSearchesForRide(Long rideId) {
        List<ReplacementSearch> openSearches = searchRepository.findByRide_IdAndStatus(
                rideId, ReplacementSearchStatus.SEARCHING);
        LocalDateTime now = LocalDateTime.now();
        for (ReplacementSearch search : openSearches) {
            for (RideMembership invitation : membershipRepository.findByReplacementSearch_IdAndStatus(
                    search.getId(), RideMembershipStatus.INVITED)) {
                invitation.cancel(now);
                membershipRepository.save(invitation);
            }
            search.cancel(now);
            searchRepository.save(search);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void resumeOpenSearches() {
        ZoneId zone = ZoneId.systemDefault();
        for (ReplacementSearch search : searchRepository.findByStatus(ReplacementSearchStatus.SEARCHING)) {
            Instant deadline = search.getDeadline().atZone(zone).toInstant();
            scheduleTimeout(search.getId(), deadline.isBefore(Instant.now()) ? Instant.now() : deadline);
        }
    }

    private void scheduleTimeout(Long searchId, Instant deadline) {
        taskScheduler.schedule(() -> {
            Instant nextDeadline = timeoutHandler.expire(searchId);
            if (nextDeadline != null) {
                scheduleTimeout(searchId, nextDeadline);
            }
        }, deadline);
    }
}

package com.ridematch.service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.entity.ReplacementSearch;
import com.ridematch.domain.enums.NotificationType;
import com.ridematch.domain.enums.ReplacementSearchStatus;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.repository.ReplacementSearchRepository;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;

/** Closes an expired search and notifies the group in a database transaction. */
@Service
public class ReplacementSearchTimeoutHandler {

    private final ReplacementSearchRepository searchRepository;
    private final RideMembershipRepository membershipRepository;
    private final RideRepository rideRepository;
    private final NotificationService notificationService;

    public ReplacementSearchTimeoutHandler(ReplacementSearchRepository searchRepository,
            RideMembershipRepository membershipRepository, RideRepository rideRepository,
            NotificationService notificationService) {
        this.searchRepository = searchRepository;
        this.membershipRepository = membershipRepository;
        this.rideRepository = rideRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Instant expire(Long searchId) {
        ReplacementSearch initialSearch = searchRepository.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Replacement search was not found"));
        rideRepository.findByIdForUpdate(initialSearch.getRide().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride was not found"));
        ReplacementSearch search = searchRepository.findByIdForUpdate(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Replacement search was not found"));
        if (search.getStatus() != ReplacementSearchStatus.SEARCHING) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(search.getDeadline())) {
            return search.getDeadline().atZone(java.time.ZoneId.systemDefault()).toInstant();
        }

        List<RideMembership> outstandingInvitations = membershipRepository
                .findByReplacementSearch_IdAndStatus(searchId, RideMembershipStatus.INVITED);
        for (RideMembership invitation : outstandingInvitations) {
            invitation.cancel(now);
            membershipRepository.save(invitation);
            notificationService.notifyUser(invitation.getUser(), search.getRide(),
                    NotificationType.REPLACEMENT_RESULT, "Replacement invitation expired",
                    "The replacement search ended before the invitation was accepted.");
        }

        search.timeOut(now);
        searchRepository.save(search);
        notificationService.notifyConfirmedPassengers(search.getRide(), List.of(),
                NotificationType.REPLACEMENT_RESULT, "Replacement search ended",
                "No replacement passenger accepted before the configured search timeout.");
        return null;
    }
}

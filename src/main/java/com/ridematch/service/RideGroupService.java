package com.ridematch.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.entity.CancellationRecord;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CancellationPolicyDecision;
import com.ridematch.domain.enums.NotificationType;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.domain.enums.RideStatus;
import com.ridematch.dto.ride.CancellationPolicyResult;
import com.ridematch.dto.ride.FareSplitResponse;
import com.ridematch.dto.ride.ReplacementInvitationResponse;
import com.ridematch.dto.ride.RideGroupResponse;
import com.ridematch.dto.ride.RideMemberResponse;
import com.ridematch.dto.ride.RideViewResponse;
import com.ridematch.exception.ConflictException;
import com.ridematch.exception.ForbiddenOperationException;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.domain.enums.ReplacementSearchStatus;
import com.ridematch.repository.CancellationRecordRepository;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;
import com.ridematch.repository.UserRepository;

/** Transactional passenger, coordinator, cancellation, and fare group operations. */
@Service
public class RideGroupService {

    private final RideRepository rideRepository;
    private final RideMembershipRepository membershipRepository;
    private final CancellationRecordRepository cancellationRecordRepository;
    private final UserRepository userRepository;
    private final CommunityService communityService;
    private final NotificationService notificationService;
    private final ReplacementSearchService replacementSearchService;
    private final CancellationPolicyService cancellationPolicyService;
    private final FareSplitService fareSplitService;

    public RideGroupService(RideRepository rideRepository, RideMembershipRepository membershipRepository,
            CancellationRecordRepository cancellationRecordRepository, UserRepository userRepository,
            CommunityService communityService, NotificationService notificationService,
            ReplacementSearchService replacementSearchService,
            CancellationPolicyService cancellationPolicyService, FareSplitService fareSplitService) {
        this.rideRepository = rideRepository;
        this.membershipRepository = membershipRepository;
        this.cancellationRecordRepository = cancellationRecordRepository;
        this.userRepository = userRepository;
        this.communityService = communityService;
        this.notificationService = notificationService;
        this.replacementSearchService = replacementSearchService;
        this.cancellationPolicyService = cancellationPolicyService;
        this.fareSplitService = fareSplitService;
    }

    @Transactional(readOnly = true)
    public RideGroupResponse getGroup(String userEmail, Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride was not found"));
        communityService.requireApprovedMember(userEmail, ride.getCommunity().getId());
        User user = findUser(userEmail);
        long confirmed = membershipRepository.countByRide_IdAndStatus(rideId, RideMembershipStatus.CONFIRMED);
        List<RideMemberResponse> members = membershipRepository.findByRide_IdOrderByCreatedAtAsc(rideId)
                .stream().map(RideMemberResponse::from).toList();
        return new RideGroupResponse(RideViewResponse.from(ride, Math.toIntExact(confirmed)),
                fareSplitService.calculate(ride), members, user.getId(), ride.getCoordinator().getId(),
                isCoordinator(ride, user));
    }

    @Transactional(readOnly = true)
    public List<RideViewResponse> listCoordinatedRides(String userEmail) {
        User user = findUser(userEmail);
        Set<Long> verifiedCommunityIds = communityService.listVerifiedCommunities(userEmail).stream()
                .map(community -> community.id()).collect(Collectors.toSet());
        return rideRepository.findByCoordinator_IdOrderByRequestedTimeDesc(user.getId()).stream()
                .filter(ride -> verifiedCommunityIds.contains(ride.getCommunity().getId()))
                .map(ride -> RideViewResponse.from(ride,
                        Math.toIntExact(confirmedCount(ride))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReplacementInvitationResponse> listMyReplacementInvitations(String userEmail) {
        User user = findUser(userEmail);
        Set<Long> verifiedCommunityIds = communityService.listVerifiedCommunities(userEmail).stream()
                .map(community -> community.id()).collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        return membershipRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(
                        user.getId(), RideMembershipStatus.INVITED).stream()
                .filter(membership -> verifiedCommunityIds.contains(membership.getRide().getCommunity().getId()))
                .filter(membership -> membership.getReplacementSearch() != null)
                .filter(membership -> membership.getReplacementSearch().getStatus() == ReplacementSearchStatus.SEARCHING)
                .filter(membership -> membership.getReplacementSearch().getDeadline().isAfter(now))
                .map(ReplacementInvitationResponse::from)
                .toList();
    }

    @Transactional
    public void requestToJoin(String userEmail, Long rideId) {
        Ride ride = lockRide(rideId);
        communityService.requireApprovedMember(userEmail, ride.getCommunity().getId());
        User user = findUser(userEmail);
        if (ride.getCreator().getId().equals(user.getId())) {
            throw new ConflictException("You cannot request to join your own ride");
        }
        requireJoinable(ride);
        if (confirmedCount(ride) >= ride.getPassengerCapacity()) {
            throw new ConflictException("This ride has no confirmed seats available");
        }
        if (membershipRepository.findByRide_IdAndUser_Id(rideId, user.getId()).isPresent()) {
            throw new ConflictException("A membership request or decision already exists for this ride");
        }

        membershipRepository.saveAndFlush(RideMembership.requestToJoin(ride, user));
        notificationService.notifyUser(ride.getCoordinator(), ride, NotificationType.JOIN_REQUEST,
                "Ride join request", user.getFullName() + " requested to join your ride.");
    }

    @Transactional
    public void acceptJoinRequest(String coordinatorEmail, Long rideId, Long membershipId) {
        Ride ride = lockRide(rideId);
        User coordinator = findUser(coordinatorEmail);
        requireCoordinator(ride, coordinator);
        RideMembership membership = findMembership(rideId, membershipId);
        if (membership.getStatus() != RideMembershipStatus.REQUESTED) {
            throw new ConflictException("This membership is not awaiting a coordinator decision");
        }
        communityService.requireApprovedMember(membership.getUser().getEmail(), ride.getCommunity().getId());
        requireJoinable(ride);
        if (confirmedCount(ride) >= ride.getPassengerCapacity()) {
            throw new ConflictException("The ride reached capacity before this request was accepted");
        }

        membership.confirm(LocalDateTime.now());
        ride.markCoordinating();
        membershipRepository.save(membership);
        rideRepository.save(ride);
        replacementSearchService.closeSearchesIfCapacityReached(ride);
        notificationService.notifyUser(membership.getUser(), ride, NotificationType.JOIN_DECISION,
                "Ride request accepted", "The coordinator accepted your request. Your seat is confirmed.");
        notificationService.notifyConfirmedPassengers(ride, Set.of(membership.getUser().getId()),
                NotificationType.RIDE_UPDATE, "Passenger joined", membership.getUser().getFullName()
                        + " joined the ride. The estimated fare share has been recalculated.");
    }

    @Transactional
    public void rejectJoinRequest(String coordinatorEmail, Long rideId, Long membershipId) {
        Ride ride = lockRide(rideId);
        requireCoordinator(ride, findUser(coordinatorEmail));
        RideMembership membership = findMembership(rideId, membershipId);
        if (membership.getStatus() != RideMembershipStatus.REQUESTED) {
            throw new ConflictException("This membership is not awaiting a coordinator decision");
        }
        membership.reject(LocalDateTime.now());
        membershipRepository.save(membership);
        notificationService.notifyUser(membership.getUser(), ride, NotificationType.JOIN_DECISION,
                "Ride request declined", "The coordinator declined your request to join this ride.");
    }

    @Transactional
    public void markTaxiArranged(String coordinatorEmail, Long rideId) {
        Ride ride = lockRide(rideId);
        requireCoordinator(ride, findUser(coordinatorEmail));
        ride.markTaxiArranged();
        rideRepository.save(ride);
        notificationService.notifyConfirmedPassengers(ride, List.of(), NotificationType.RIDE_UPDATE,
                "Taxi arranged externally", "The coordinator recorded that a taxi was arranged outside RideMatch.");
    }

    @Transactional
    public CancellationPolicyResult cancelOwnMembership(String userEmail, Long rideId, Long membershipId, String reason) {
        Ride ride = lockRide(rideId);
        RideMembership membership = findMembership(rideId, membershipId);
        User user = findUser(userEmail);
        if (!membership.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("You can only cancel your own membership");
        }
        if (isCoordinator(ride, user)) {
            throw new ConflictException("The coordinator must cancel the ride for the whole group");
        }
        if (ride.getStatus() == RideStatus.CANCELLED || ride.getStatus() == RideStatus.COMPLETED) {
            throw new ConflictException("This ride is already closed");
        }

        RideMembershipStatus previousStatus = membership.getStatus();
        if (!isCancellable(previousStatus)) {
            throw new ConflictException("This membership is already closed");
        }
        LocalDateTime now = LocalDateTime.now();
        CancellationPolicyResult policy = previousStatus == RideMembershipStatus.CONFIRMED
                ? cancellationPolicyService.assess(ride, now)
                : noPotentialLoss();
        membership.cancel(now);
        membershipRepository.save(membership);
        cancellationRecordRepository.save(new CancellationRecord(membership, reason,
                policy.decision(), policy.explanation()));

        notificationService.notifyConfirmedPassengers(ride, Set.of(user.getId()), NotificationType.CANCELLATION,
                "Passenger cancelled", user.getFullName() + " cancelled. The estimated fare share was recalculated.");

        if (previousStatus == RideMembershipStatus.CONFIRMED) {
            replacementSearchService.startSearch(ride, membership);
        } else if (previousStatus == RideMembershipStatus.INVITED
                && membership.getReplacementSearch() != null) {
            replacementSearchService.invitationDeclined(membership.getReplacementSearch().getId());
        }
        return policy;
    }

    @Transactional
    public CancellationPolicyResult cancelRide(String coordinatorEmail, Long rideId, String reason) {
        Ride ride = lockRide(rideId);
        User coordinator = findUser(coordinatorEmail);
        requireCoordinator(ride, coordinator);
        if (ride.getStatus() == RideStatus.CANCELLED || ride.getStatus() == RideStatus.COMPLETED) {
            throw new ConflictException("This ride is already closed");
        }

        LocalDateTime now = LocalDateTime.now();
        CancellationPolicyResult ridePolicy = cancellationPolicyService.assess(ride, now);
        List<RideMembership> memberships = membershipRepository.findByRide_IdOrderByCreatedAtAsc(rideId);
        List<User> affectedUsers = memberships.stream()
                .filter(membership -> isCancellable(membership.getStatus()))
                .map(RideMembership::getUser).toList();

        for (RideMembership membership : memberships) {
            if (isCancellable(membership.getStatus())) {
                CancellationPolicyResult policy = membership.getStatus() == RideMembershipStatus.CONFIRMED
                        ? cancellationPolicyService.assess(ride, now) : noPotentialLoss();
                membership.cancel(now);
                membershipRepository.save(membership);
                cancellationRecordRepository.save(new CancellationRecord(membership, reason,
                        policy.decision(), policy.explanation()));
            }
        }
        ride.cancel();
        rideRepository.save(ride);
        replacementSearchService.cancelSearchesForRide(rideId);
        notificationService.notifyUsers(affectedUsers, ride, NotificationType.CANCELLATION,
                "Ride cancelled by coordinator", "The coordinator cancelled this ride. " + ridePolicy.explanation());
        return ridePolicy;
    }

    @Transactional
    public boolean acceptReplacementInvitation(String userEmail, Long rideId, Long membershipId) {
        Ride ride = lockRide(rideId);
        RideMembership invitation = findMembership(rideId, membershipId);
        User invitee = findUser(userEmail);
        if (!invitation.getUser().getId().equals(invitee.getId())) {
            throw new ForbiddenOperationException("This replacement invitation belongs to another user");
        }
        if (invitation.getStatus() != RideMembershipStatus.INVITED || invitation.getReplacementSearch() == null) {
            throw new ConflictException("This replacement invitation is no longer active");
        }
        communityService.requireApprovedMember(userEmail, ride.getCommunity().getId());
        var search = invitation.getReplacementSearch();
        var lockedSearch = replacementSearchService.lockSearch(search.getId());
        if (lockedSearch.getStatus() != com.ridematch.domain.enums.ReplacementSearchStatus.SEARCHING
                || !LocalDateTime.now().isBefore(lockedSearch.getDeadline())) {
            throw new ConflictException("The replacement invitation has expired");
        }
        if (confirmedCount(ride) >= ride.getPassengerCapacity()) {
            invitation.cancel(LocalDateTime.now());
            membershipRepository.save(invitation);
            lockedSearch.markFilled(LocalDateTime.now());
            replacementSearchService.saveSearch(lockedSearch);
            replacementSearchService.closeSearchesIfCapacityReached(ride);
            notificationService.notifyUser(invitee, ride, NotificationType.REPLACEMENT_RESULT,
                    "Replacement seat no longer available", "The vacant seat was filled before your response.");
            return false;
        }

        invitation.confirm(LocalDateTime.now());
        ride.markCoordinating();
        lockedSearch.markReplaced(invitation, LocalDateTime.now());
        membershipRepository.save(invitation);
        rideRepository.save(ride);
        replacementSearchService.saveSearch(lockedSearch);
        replacementSearchService.closeSearchesIfCapacityReached(ride);
        notificationService.notifyConfirmedPassengers(ride, Set.of(invitee.getId()),
                NotificationType.REPLACEMENT_RESULT, "Replacement passenger confirmed",
                invitee.getFullName() + " accepted the replacement invitation. The estimated fare share was recalculated.");
        return true;
    }

    @Transactional
    public void rejectReplacementInvitation(String userEmail, Long rideId, Long membershipId) {
        Ride ride = lockRide(rideId);
        RideMembership invitation = findMembership(rideId, membershipId);
        User invitee = findUser(userEmail);
        if (!invitation.getUser().getId().equals(invitee.getId())) {
            throw new ForbiddenOperationException("This replacement invitation belongs to another user");
        }
        if (invitation.getStatus() != RideMembershipStatus.INVITED || invitation.getReplacementSearch() == null) {
            throw new ConflictException("This replacement invitation is no longer active");
        }
        var search = invitation.getReplacementSearch();
        invitation.reject(LocalDateTime.now());
        membershipRepository.save(invitation);
        replacementSearchService.invitationDeclined(search.getId());
        notificationService.notifyUser(ride.getCoordinator(), ride, NotificationType.REPLACEMENT_RESULT,
                "Replacement invitation declined", invitee.getFullName() + " declined the replacement invitation.");
    }

    private Ride lockRide(Long rideId) {
        return rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride was not found"));
    }

    private RideMembership findMembership(Long rideId, Long membershipId) {
        return membershipRepository.findByIdAndRide_Id(membershipId, rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride membership was not found"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }

    private long confirmedCount(Ride ride) {
        return membershipRepository.countByRide_IdAndStatus(ride.getId(), RideMembershipStatus.CONFIRMED);
    }

    private static CancellationPolicyResult noPotentialLoss() {
        return new CancellationPolicyResult(CancellationPolicyDecision.NO_POTENTIAL_LOSS,
                "This membership had no confirmed seat, so no potential financial consequence is assessed.");
    }

    private void requireCoordinator(Ride ride, User user) {
        if (!isCoordinator(ride, user)) {
            throw new ForbiddenOperationException("Only this ride's coordinator can perform that action");
        }
    }

    private static boolean isCoordinator(Ride ride, User user) {
        return ride.getCoordinator() != null && ride.getCoordinator().getId().equals(user.getId());
    }

    private static boolean isCancellable(RideMembershipStatus status) {
        return status == RideMembershipStatus.REQUESTED
                || status == RideMembershipStatus.INVITED
                || status == RideMembershipStatus.CONFIRMED;
    }

    private static void requireJoinable(Ride ride) {
        if (ride.getStatus() != RideStatus.OPEN && ride.getStatus() != RideStatus.COORDINATING) {
            throw new ConflictException("This ride is not accepting join requests");
        }
    }
}

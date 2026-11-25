package com.ridematch.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.domain.enums.RideStatus;
import com.ridematch.dto.ride.MyRideResponse;
import com.ridematch.dto.ride.RideJoinRequestResponse;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;
import com.ridematch.repository.UserRepository;

/** Reads user-specific ride activity for the dashboard and request inbox. */
@Service
public class RideDashboardService {

    private static final List<RideMembershipStatus> CURRENT_STATUSES = List.of(
            RideMembershipStatus.CONFIRMED, RideMembershipStatus.REQUESTED, RideMembershipStatus.INVITED);

    private final RideRepository rideRepository;
    private final RideMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CommunityService communityService;

    public RideDashboardService(RideRepository rideRepository, RideMembershipRepository membershipRepository,
            UserRepository userRepository, CommunityService communityService) {
        this.rideRepository = rideRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.communityService = communityService;
    }

    @Transactional(readOnly = true)
    public List<MyRideResponse> listMyCurrentRides(String email) {
        var user = findUser(email);
        Set<Long> verifiedCommunityIds = communityService.listVerifiedCommunities(email).stream()
                .map(community -> community.id()).collect(Collectors.toSet());
        return membershipRepository.findByUser_IdAndStatusInOrderByCreatedAtDesc(user.getId(), CURRENT_STATUSES)
                .stream()
                .filter(membership -> verifiedCommunityIds.contains(membership.getRide().getCommunity().getId()))
                .filter(membership -> membership.getRide().getRequestedTime().isAfter(LocalDateTime.now()))
                .filter(membership -> membership.getRide().getStatus() != RideStatus.CANCELLED
                        && membership.getRide().getStatus() != RideStatus.COMPLETED)
                .map(membership -> MyRideResponse.from(membership,
                        Math.toIntExact(membershipRepository.countByRide_IdAndStatus(
                                membership.getRide().getId(), RideMembershipStatus.CONFIRMED)), user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideJoinRequestResponse> listPendingJoinRequests(String coordinatorEmail) {
        var user = findUser(coordinatorEmail);
        Set<Long> verifiedCommunityIds = communityService.listVerifiedCommunities(coordinatorEmail).stream()
                .map(community -> community.id()).collect(Collectors.toSet());
        return rideRepository.findByCoordinator_IdOrderByRequestedTimeDesc(user.getId()).stream()
                .filter(ride -> verifiedCommunityIds.contains(ride.getCommunity().getId()))
                .flatMap(ride -> membershipRepository.findByRide_IdAndStatusOrderByCreatedAtAsc(
                        ride.getId(), RideMembershipStatus.REQUESTED).stream())
                .filter(request -> request.getRide().getRequestedTime().isAfter(LocalDateTime.now()))
                .map(RideJoinRequestResponse::from)
                .toList();
    }

    private com.ridematch.domain.entity.User findUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(com.ridematch.domain.entity.User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }
}

package com.ridematch.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.RideKind;
import com.ridematch.dto.community.CommunityResponse;
import com.ridematch.dto.ride.RideCreateRequest;
import com.ridematch.dto.ride.RideMatchResponse;
import com.ridematch.dto.ride.RideSearchRequest;
import com.ridematch.dto.ride.RideViewResponse;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.exception.ConflictException;
import com.ridematch.repository.CommunityRepository;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;
import com.ridematch.repository.UserRepository;

/** Orchestrates verified community ride creation and searches. */
@Service
public class RideService {

    private final RideRepository rideRepository;
    private final RideMembershipRepository rideMembershipRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommunityService communityService;
    private final RideMatchingService rideMatchingService;
    private final NotificationService notificationService;

    public RideService(RideRepository rideRepository, RideMembershipRepository rideMembershipRepository,
            UserRepository userRepository, CommunityRepository communityRepository,
            CommunityService communityService, RideMatchingService rideMatchingService) {
        this(rideRepository, rideMembershipRepository, userRepository, communityRepository,
                communityService, rideMatchingService, null);
    }

    @Autowired
    public RideService(RideRepository rideRepository, RideMembershipRepository rideMembershipRepository,
            UserRepository userRepository, CommunityRepository communityRepository,
            CommunityService communityService, RideMatchingService rideMatchingService,
            NotificationService notificationService) {
        this.rideRepository = rideRepository;
        this.rideMembershipRepository = rideMembershipRepository;
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.communityService = communityService;
        this.rideMatchingService = rideMatchingService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public CommunityResponse requireRideAccess(String userEmail, Long communityId) {
        communityService.requireApprovedMember(userEmail, communityId);
        return communityService.getCommunity(communityId);
    }

    @Transactional(readOnly = true)
    public List<CommunityResponse> listVerifiedCommunities(String userEmail) {
        return communityService.listVerifiedCommunities(userEmail);
    }

    @Transactional
    public RideViewResponse createRide(String userEmail, Long communityId, RideCreateRequest request) {
        communityService.requireApprovedMember(userEmail, communityId);
        User creator = userRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community was not found"));

        LocalDateTime departure = LocalDateTime.of(request.getTravelDate(), request.getDepartureTime());
        if (!departure.isAfter(LocalDateTime.now())) {
            throw new ConflictException("Ride departure must be in the future");
        }
        LocalDateTime departureEnd = request.getDepartureTimeEnd() == null ? null
                : LocalDateTime.of(request.getTravelDate(), request.getDepartureTimeEnd());
        if (departureEnd != null && departureEnd.isBefore(departure)) {
            throw new ConflictException("End of time range must be after its start");
        }
        Ride ride = new Ride(community, creator, request.getPickupLocation(), request.getDestination(),
                RideKind.STANDARD, departure, departureEnd, request.getEstimatedTotalFare(),
                request.getPassengerCapacity(), request.getNotes());
        Ride savedRide = rideRepository.saveAndFlush(ride);

        // The creator is also the first passenger and coordinator, so capacity includes them.
        rideMembershipRepository.saveAndFlush(new RideMembership(savedRide, creator, LocalDateTime.now()));
        if (notificationService != null) notificationService.notifyCommunityOfRide(savedRide);
        long activePassengers = rideMembershipRepository.countByRide_IdAndStatus(
                savedRide.getId(), com.ridematch.domain.enums.RideMembershipStatus.CONFIRMED);
        return RideViewResponse.from(savedRide, Math.toIntExact(activePassengers));
    }

    @Transactional(readOnly = true)
    public List<RideMatchResponse> searchRides(String userEmail, RideSearchRequest request) {
        if (!LocalDateTime.of(request.getTravelDate(), request.getDepartureTime()).isAfter(LocalDateTime.now())) {
            throw new ConflictException("Search departure must be in the future");
        }
        communityService.requireApprovedMember(userEmail, request.getCommunityId());
        User user = userRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        return rideMatchingService.findMatches(user.getId(), request);
    }
}

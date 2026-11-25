package com.ridematch.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.config.MatchingProperties;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.enums.DestinationMatchType;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.domain.enums.RideStatus;
import com.ridematch.dto.ride.RideMatchResponse;
import com.ridematch.dto.ride.RideSearchRequest;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;

/** Finds and ranks ride candidates using explicit community, time, and location rules. */
@Service
public class RideMatchingService {

    private static final List<RideStatus> ELIGIBLE_STATUSES =
            List.of(RideStatus.OPEN, RideStatus.COORDINATING);

    private final RideRepository rideRepository;
    private final RideMembershipRepository rideMembershipRepository;
    private final MatchingProperties matchingProperties;
    private final LocationNormalizer locationNormalizer;

    public RideMatchingService(RideRepository rideRepository,
            RideMembershipRepository rideMembershipRepository,
            MatchingProperties matchingProperties, LocationNormalizer locationNormalizer) {
        this.rideRepository = rideRepository;
        this.rideMembershipRepository = rideMembershipRepository;
        this.matchingProperties = matchingProperties;
        this.locationNormalizer = locationNormalizer;
    }

    @Transactional(readOnly = true)
    public List<RideMatchResponse> findMatches(Long requestingUserId, RideSearchRequest request) {
        LocalDateTime requestedDeparture = LocalDateTime.of(request.getTravelDate(), request.getDepartureTime());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = request.getTravelDate().atStartOfDay();
        LocalDateTime nextDayStart = request.getTravelDate().plusDays(1).atStartOfDay();
        Duration maximumDifference = matchingProperties.getMaximumDepartureDifference();
        if (maximumDifference == null || maximumDifference.isZero() || maximumDifference.isNegative()
                || maximumDifference.toMillis() == 0) {
            throw new IllegalStateException("The maximum departure difference must be a positive duration");
        }

        String normalizedPickup = locationNormalizer.normalize(request.getPickupLocation());
        String normalizedDestination = locationNormalizer.normalize(request.getDestination());

        List<Ride> candidates = rideRepository
                .findByCommunity_IdAndStatusInAndRequestedTimeGreaterThanEqualAndRequestedTimeLessThanOrderByRequestedTime(
                        request.getCommunityId(), ELIGIBLE_STATUSES, dayStart, nextDayStart);

        return candidates.stream()
                .filter(ride -> isSameCommunity(ride, request.getCommunityId()))
                .filter(ride -> ride.getRequestedTime().toLocalDate().equals(request.getTravelDate()))
                .filter(ride -> (ride.getRequestedTimeEnd() == null ? ride.getRequestedTime()
                        : ride.getRequestedTimeEnd()).isAfter(now))
                .filter(ride -> ELIGIBLE_STATUSES.contains(ride.getStatus()))
                .filter(ride -> !isOwnRide(ride, requestingUserId))
                .filter(ride -> !rideMembershipRepository.existsByRide_IdAndUser_Id(ride.getId(), requestingUserId))
                .filter(ride -> locationNormalizer.normalize(ride.getOrigin()).equals(normalizedPickup))
                .map(ride -> toMatch(ride, requestedDeparture, normalizedDestination, maximumDifference))
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    int scoreOrder = Integer.compare(right.compatibilityScore(), left.compatibilityScore());
                    if (scoreOrder != 0) {
                        return scoreOrder;
                    }
                    int timeOrder = Long.compare(left.departureDifferenceMinutes(), right.departureDifferenceMinutes());
                    if (timeOrder != 0) {
                        return timeOrder;
                    }
                    return left.departureTime().compareTo(right.departureTime());
                })
                .toList();
    }

    private RideMatchResponse toMatch(Ride ride, LocalDateTime requestedDeparture,
            String normalizedDestination, Duration maximumDifference) {
        Duration difference;
        if (ride.getRequestedTimeEnd() != null && !requestedDeparture.isBefore(ride.getRequestedTime())
                && !requestedDeparture.isAfter(ride.getRequestedTimeEnd())) {
            difference = Duration.ZERO;
        } else {
            LocalDateTime closestTime = requestedDeparture.isBefore(ride.getRequestedTime())
                    ? ride.getRequestedTime()
                    : ride.getRequestedTimeEnd() == null ? ride.getRequestedTime() : ride.getRequestedTimeEnd();
            difference = Duration.between(requestedDeparture, closestTime).abs();
        }
        if (difference.compareTo(maximumDifference) > 0) {
            return null;
        }

        String candidateDestination = locationNormalizer.normalize(ride.getDestination());
        DestinationMatchType destinationMatchType = classifyDestination(normalizedDestination, candidateDestination);
        if (destinationMatchType == null) {
            return null;
        }

        long activePassengers = rideMembershipRepository.countByRide_IdAndStatus(
                ride.getId(), RideMembershipStatus.CONFIRMED);
        if (activePassengers >= ride.getPassengerCapacity()) {
            return null;
        }

        int score = calculateScore(destinationMatchType, difference, maximumDifference);
        long differenceMinutes = Math.round(difference.toMillis() / 60_000.0);
        int availableSeats = ride.getPassengerCapacity() - Math.toIntExact(activePassengers);
        String reason = destinationMatchType == DestinationMatchType.EXACT
                ? "Exact destination match"
                : "Destination matches a configured nearby-location group";
        reason += "; departure differs by " + differenceMinutes + " minute(s)";

        return new RideMatchResponse(ride.getId(), ride.getOrigin(), ride.getDestination(),
                ride.getRequestedTime().toLocalDate(), ride.getRequestedTime().toLocalTime(),
                ride.getRequestedTimeEnd() == null ? null : ride.getRequestedTimeEnd().toLocalTime(),
                ride.getPassengerCapacity(), availableSeats, ride.getEstimatedTotalFare(), ride.getNotes(),
                destinationMatchType, differenceMinutes, score, reason);
    }

    private DestinationMatchType classifyDestination(String requested, String candidate) {
        if (requested.equals(candidate)) {
            return DestinationMatchType.EXACT;
        }
        for (String group : matchingProperties.getNearbyDestinationGroups()) {
            List<String> aliases = java.util.Arrays.stream(group.split("\\|"))
                    .map(locationNormalizer::normalize)
                    .filter(alias -> !alias.isBlank())
                    .toList();
            if (aliases.contains(requested) && aliases.contains(candidate)) {
                return DestinationMatchType.NEARBY;
            }
        }
        return null;
    }

    private static int calculateScore(DestinationMatchType destinationMatchType,
            Duration difference, Duration maximumDifference) {
        int destinationPoints = destinationMatchType == DestinationMatchType.EXACT ? 70 : 45;
        double timeRatio = (double) difference.toMillis() / maximumDifference.toMillis();
        int timePoints = (int) Math.round(30 * (1.0 - timeRatio));
        return destinationPoints + Math.max(0, timePoints);
    }

    private static boolean isSameCommunity(Ride ride, Long communityId) {
        return ride.getCommunity() != null && Objects.equals(ride.getCommunity().getId(), communityId);
    }

    private static boolean isOwnRide(Ride ride, Long requestingUserId) {
        return ride.getCreator() != null && Objects.equals(ride.getCreator().getId(), requestingUserId);
    }
}

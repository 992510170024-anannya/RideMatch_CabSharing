package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridematch.config.MatchingProperties;
import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.DestinationMatchType;
import com.ridematch.domain.enums.RideKind;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.domain.enums.RideStatus;
import com.ridematch.dto.ride.RideMatchResponse;
import com.ridematch.dto.ride.RideSearchRequest;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.RideRepository;

@ExtendWith(MockitoExtension.class)
class RideMatchingServiceTest {

    private static final Long SEARCHER_ID = 1L;
    private static final Long COMMUNITY_ID = 10L;
    private static final LocalDate TRAVEL_DATE = LocalDate.now().plusDays(5);

    @Mock
    private RideRepository rideRepository;
    @Mock
    private RideMembershipRepository membershipRepository;

    private RideMatchingService matchingService;
    private Community community;
    private User otherUser;

    @BeforeEach
    void setUp() {
        MatchingProperties properties = new MatchingProperties();
        properties.setMaximumDepartureDifference(Duration.ofMinutes(15));
        properties.setNearbyDestinationGroups(List.of("Sector 99|Noida Sector 99|Sector 100|Noida Sector 100"));
        matchingService = new RideMatchingService(rideRepository, membershipRepository,
                properties, new LocationNormalizer());

        community = new Community("JIIT", CommunityType.COLLEGE, null);
        otherUser = new User("Other Passenger", "other@example.com", "encoded-password");
        ReflectionTestUtils.setField(community, "id", COMMUNITY_ID);
        ReflectionTestUtils.setField(otherUser, "id", 2L);
    }

    @Test
    void exactDestinationIsReturnedAsExactMatch() {
        Ride ride = ride(100L, community, "JIIT Sector 128", "Sector 99", at("17:05"), 3, RideStatus.OPEN);
        stubCandidates(ride);
        when(membershipRepository.countByRide_IdAndStatus(100L, RideMembershipStatus.CONFIRMED)).thenReturn(1L);

        List<RideMatchResponse> matches = matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00"));

        assertEquals(1, matches.size());
        assertEquals(DestinationMatchType.EXACT, matches.get(0).destinationMatchType());
        assertEquals(2, matches.get(0).availableSeats());
    }

    @Test
    void configuredNearbyDestinationIsReturnedAsNearbyMatch() {
        Ride ride = ride(101L, community, "JIIT Sector 128", "Sector 100", at("17:00"), 3, RideStatus.OPEN);
        stubCandidates(ride);
        when(membershipRepository.countByRide_IdAndStatus(101L, RideMembershipStatus.CONFIRMED)).thenReturn(1L);

        List<RideMatchResponse> matches = matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00"));

        assertEquals(1, matches.size());
        assertEquals(DestinationMatchType.NEARBY, matches.get(0).destinationMatchType());
    }

    @Test
    void departureBeyondConfiguredDifferenceIsExcluded() {
        Ride ride = ride(102L, community, "JIIT Sector 128", "Sector 99", at("17:16"), 3, RideStatus.OPEN);
        stubCandidates(ride);

        assertTrue(matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00")).isEmpty());
    }

    @Test
    void rideFromDifferentCommunityIsExcluded() {
        Community differentCommunity = new Community("Other College", CommunityType.COLLEGE, null);
        ReflectionTestUtils.setField(differentCommunity, "id", 11L);
        Ride ride = ride(103L, differentCommunity, "JIIT Sector 128", "Sector 99", at("17:00"), 3, RideStatus.OPEN);
        stubCandidates(ride);

        assertTrue(matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00")).isEmpty());
    }

    @Test
    void fullRideIsExcludedUsingOnlyActiveSeatCount() {
        Ride ride = ride(104L, community, "JIIT Sector 128", "Sector 99", at("17:00"), 2, RideStatus.OPEN);
        stubCandidates(ride);
        when(membershipRepository.countByRide_IdAndStatus(104L, RideMembershipStatus.CONFIRMED)).thenReturn(2L);

        assertTrue(matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00")).isEmpty());
    }

    @Test
    void cancelledRideIsExcluded() {
        Ride ride = ride(105L, community, "JIIT Sector 128", "Sector 99", at("17:00"), 3, RideStatus.OPEN);
        ride.cancel();
        stubCandidates(ride);

        assertTrue(matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00")).isEmpty());
    }

    @Test
    void completedRideIsExcluded() {
        Ride ride = ride(106L, community, "JIIT Sector 128", "Sector 99", at("17:00"), 3, RideStatus.OPEN);
        ride.complete();
        stubCandidates(ride);

        assertTrue(matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00")).isEmpty());
    }

    @Test
    void requestingUserDoesNotMatchTheirOwnRide() {
        User ownUser = new User("Searcher", "searcher@example.com", "encoded-password");
        ReflectionTestUtils.setField(ownUser, "id", SEARCHER_ID);
        Ride ride = new Ride(community, ownUser, "JIIT Sector 128", "Sector 99", RideKind.STANDARD,
                at("17:00"), new BigDecimal("500.00"), 3, null);
        ReflectionTestUtils.setField(ride, "id", 107L);
        stubCandidates(ride);

        assertTrue(matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00")).isEmpty());
    }

    @Test
    void requestingUserDoesNotSeeRideTheyAlreadyJoinedOrRequested() {
        Ride ride = ride(110L, community, "JIIT Sector 128", "Sector 99", at("17:00"), 4, RideStatus.OPEN);
        stubCandidates(ride);
        when(membershipRepository.existsByRide_IdAndUser_Id(110L, SEARCHER_ID)).thenReturn(true);

        assertTrue(matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00")).isEmpty());
    }

    @Test
    void rideWithPastDepartureIsExcluded() {
        LocalDateTime pastDeparture = LocalDateTime.now().minusMinutes(5);
        Ride ride = ride(111L, community, "JIIT Sector 128", "Sector 99", pastDeparture, 4, RideStatus.OPEN);
        stubCandidates(ride);
        RideSearchRequest pastSearch = new RideSearchRequest();
        pastSearch.setCommunityId(COMMUNITY_ID);
        pastSearch.setPickupLocation("JIIT Sector 128");
        pastSearch.setDestination("Sector 99");
        pastSearch.setTravelDate(pastDeparture.toLocalDate());
        pastSearch.setDepartureTime(pastDeparture.toLocalTime());

        assertTrue(matchingService.findMatches(SEARCHER_ID, pastSearch).isEmpty());
    }

    @Test
    void resultsAreRankedByCompatibilityScore() {
        Ride exactLater = ride(108L, community, "JIIT Sector 128", "Sector 99", at("17:10"), 4, RideStatus.OPEN);
        Ride nearbyOnTime = ride(109L, community, "JIIT Sector 128", "Sector 100", at("17:00"), 4, RideStatus.OPEN);
        stubCandidates(nearbyOnTime, exactLater);
        when(membershipRepository.countByRide_IdAndStatus(108L, RideMembershipStatus.CONFIRMED)).thenReturn(1L);
        when(membershipRepository.countByRide_IdAndStatus(109L, RideMembershipStatus.CONFIRMED)).thenReturn(1L);

        List<RideMatchResponse> matches = matchingService.findMatches(SEARCHER_ID, search("Sector 99", "17:00"));

        assertEquals(2, matches.size());
        assertEquals(108L, matches.get(0).rideId());
        assertTrue(matches.get(0).compatibilityScore() > matches.get(1).compatibilityScore());
    }

    private void stubCandidates(Ride... rides) {
        when(rideRepository
                .findByCommunity_IdAndStatusInAndRequestedTimeGreaterThanEqualAndRequestedTimeLessThanOrderByRequestedTime(
                        eq(COMMUNITY_ID), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(rides));
    }

    private Ride ride(Long rideId, Community rideCommunity, String pickup, String destination,
            LocalDateTime departure, int capacity, RideStatus status) {
        Ride ride = new Ride(rideCommunity, otherUser, pickup, destination, RideKind.STANDARD,
                departure, new BigDecimal("500.00"), capacity, null);
        ReflectionTestUtils.setField(ride, "id", rideId);
        if (status == RideStatus.CANCELLED) {
            ride.cancel();
        } else if (status == RideStatus.COMPLETED) {
            ride.complete();
        }
        return ride;
    }

    private RideSearchRequest search(String destination, String time) {
        RideSearchRequest request = new RideSearchRequest();
        request.setCommunityId(COMMUNITY_ID);
        request.setPickupLocation("jiit sector 128");
        request.setDestination(destination);
        request.setTravelDate(TRAVEL_DATE);
        request.setDepartureTime(LocalTime.parse(time));
        return request;
    }

    private LocalDateTime at(String time) {
        return LocalDateTime.of(TRAVEL_DATE, LocalTime.parse(time));
    }
}

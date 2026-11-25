package com.ridematch.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.enums.RideStatus;

/** Persistence operations for ride requests and coordination groups. */
public interface RideRepository extends JpaRepository<Ride, Long> {

    /** Locks a ride row while a future join service checks capacity and adds a passenger. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ride from Ride ride where ride.id = :rideId")
    Optional<Ride> findByIdForUpdate(@Param("rideId") Long rideId);

    List<Ride> findByCommunity_IdAndStatusAndRequestedTimeBetweenOrderByRequestedTime(
            Long communityId, RideStatus status, LocalDateTime start, LocalDateTime end);

    List<Ride> findByCommunity_IdAndStatusInAndRequestedTimeGreaterThanEqualAndRequestedTimeLessThanOrderByRequestedTime(
            Long communityId, java.util.Collection<RideStatus> statuses,
            LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<Ride> findByCreator_IdOrderByCreatedAtDesc(Long creatorId);

    List<Ride> findByCoordinator_IdOrderByRequestedTimeDesc(Long coordinatorId);
}

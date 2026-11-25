package com.ridematch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.enums.RideMembershipStatus;

/** Persistence operations for passengers belonging to rides. */
public interface RideMembershipRepository extends JpaRepository<RideMembership, Long> {

    Optional<RideMembership> findByRide_IdAndUser_Id(Long rideId, Long userId);

    boolean existsByRide_IdAndUser_IdAndStatus(Long rideId, Long userId, RideMembershipStatus status);

    boolean existsByRide_IdAndUser_Id(Long rideId, Long userId);

    long countByRide_IdAndStatus(Long rideId, RideMembershipStatus status);

    List<RideMembership> findByRide_IdAndStatusOrderByJoinedAt(
            Long rideId, RideMembershipStatus status);

    List<RideMembership> findByRide_IdOrderByCreatedAtAsc(Long rideId);

    List<RideMembership> findByRide_IdAndStatusOrderByCreatedAtAsc(Long rideId, RideMembershipStatus status);

    List<RideMembership> findByReplacementSearch_IdAndStatus(Long replacementSearchId, RideMembershipStatus status);

    List<RideMembership> findByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, RideMembershipStatus status);

    List<RideMembership> findByUser_IdAndStatusInOrderByCreatedAtDesc(
            Long userId, java.util.Collection<RideMembershipStatus> statuses);

    Optional<RideMembership> findByIdAndRide_Id(Long membershipId, Long rideId);
}

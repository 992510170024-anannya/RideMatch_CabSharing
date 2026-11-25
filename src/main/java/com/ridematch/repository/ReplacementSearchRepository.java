package com.ridematch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ridematch.domain.entity.ReplacementSearch;
import com.ridematch.domain.enums.ReplacementSearchStatus;

import jakarta.persistence.LockModeType;

/** Persistence operations for deadline-bounded replacement searches. */
public interface ReplacementSearchRepository extends JpaRepository<ReplacementSearch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select search from ReplacementSearch search where search.id = :searchId")
    Optional<ReplacementSearch> findByIdForUpdate(@Param("searchId") Long searchId);

    List<ReplacementSearch> findByStatus(ReplacementSearchStatus status);

    List<ReplacementSearch> findByRide_IdAndStatus(Long rideId, ReplacementSearchStatus status);
}

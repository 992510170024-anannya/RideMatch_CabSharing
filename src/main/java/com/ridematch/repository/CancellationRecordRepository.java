package com.ridematch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridematch.domain.entity.CancellationRecord;

/** Persistence operations for ride participation cancellation history. */
public interface CancellationRecordRepository extends JpaRepository<CancellationRecord, Long> {

    List<CancellationRecord> findByRideMembership_IdOrderByCreatedAtDesc(Long rideMembershipId);
}

package com.ridematch.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.ridematch.domain.enums.ReplacementSearchStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Persisted time-bounded replacement search associated with one cancelled seat. */
@Entity
@Table(name = "replacement_searches",
        uniqueConstraints = @UniqueConstraint(name = "uk_replacement_search_cancelled_membership",
                columnNames = "cancelled_membership_id"),
        indexes = {
                @Index(name = "idx_replacement_search_ride_status", columnList = "ride_id, search_status"),
                @Index(name = "idx_replacement_search_deadline", columnList = "search_status, deadline")
        })
public class ReplacementSearch extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false, foreignKey = @ForeignKey(name = "fk_replacement_search_ride"))
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancelled_membership_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_replacement_search_cancelled_membership"))
    private RideMembership cancelledMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filled_by_membership_id",
            foreignKey = @ForeignKey(name = "fk_replacement_search_filled_by_membership"))
    private RideMembership filledByMembership;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_status", nullable = false, length = 20)
    private ReplacementSearchStatus status = ReplacementSearchStatus.SEARCHING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected ReplacementSearch() {
    }

    public ReplacementSearch(Ride ride, RideMembership cancelledMembership,
            LocalDateTime startedAt, LocalDateTime deadline) {
        this.ride = Objects.requireNonNull(ride, "ride must not be null");
        this.cancelledMembership = Objects.requireNonNull(cancelledMembership, "cancelledMembership must not be null");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.deadline = Objects.requireNonNull(deadline, "deadline must not be null");
        if (!deadline.isAfter(startedAt)) {
            throw new IllegalArgumentException("deadline must be after startedAt");
        }
    }

    public void markReplaced(RideMembership replacement, LocalDateTime completedAt) {
        requireSearching();
        this.filledByMembership = Objects.requireNonNull(replacement, "replacement must not be null");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = ReplacementSearchStatus.REPLACED;
    }

    public void markFilled(LocalDateTime completedAt) {
        requireSearching();
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = ReplacementSearchStatus.FILLED;
    }

    public void timeOut(LocalDateTime completedAt) {
        requireSearching();
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = ReplacementSearchStatus.TIMED_OUT;
    }

    public void cancel(LocalDateTime completedAt) {
        if (status == ReplacementSearchStatus.SEARCHING) {
            this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
            this.status = ReplacementSearchStatus.CANCELLED;
        }
    }

    private void requireSearching() {
        if (status != ReplacementSearchStatus.SEARCHING) {
            throw new IllegalStateException("Replacement search is already closed");
        }
    }

    public Long getId() { return id; }
    public Ride getRide() { return ride; }
    public RideMembership getCancelledMembership() { return cancelledMembership; }
    public RideMembership getFilledByMembership() { return filledByMembership; }
    public ReplacementSearchStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

package com.ridematch.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.ridematch.domain.enums.RideMembershipStatus;

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

/** A request, invitation, confirmed seat, or closed participation in one ride. */
@Entity
@Table(name = "ride_memberships",
        uniqueConstraints = @UniqueConstraint(name = "uk_ride_membership_ride_user", columnNames = {"ride_id", "user_id"}),
        indexes = {
                @Index(name = "idx_ride_memberships_ride_status", columnList = "ride_id, membership_status"),
                @Index(name = "idx_ride_memberships_user_status", columnList = "user_id, membership_status"),
                @Index(name = "idx_ride_memberships_replacement_search", columnList = "replacement_search_id, membership_status")
        })
public class RideMembership extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ride_memberships_ride"))
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ride_memberships_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private RideMembershipStatus status = RideMembershipStatus.REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_search_id",
            foreignKey = @ForeignKey(name = "fk_ride_memberships_replacement_search"))
    private ReplacementSearch replacementSearch;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected RideMembership() {
    }

    /** Creates the creator's confirmed first passenger membership. */
    public RideMembership(Ride ride, User user, LocalDateTime joinedAt) {
        this.ride = Objects.requireNonNull(ride, "ride must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        this.decidedAt = joinedAt;
        this.status = RideMembershipStatus.CONFIRMED;
    }

    private RideMembership(Ride ride, User user, RideMembershipStatus status,
            ReplacementSearch replacementSearch) {
        this.ride = Objects.requireNonNull(ride, "ride must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.replacementSearch = replacementSearch;
    }

    public static RideMembership requestToJoin(Ride ride, User user) {
        return new RideMembership(ride, user, RideMembershipStatus.REQUESTED, null);
    }

    public static RideMembership inviteAsReplacement(Ride ride, User user,
            ReplacementSearch replacementSearch) {
        return new RideMembership(ride, user, RideMembershipStatus.INVITED,
                Objects.requireNonNull(replacementSearch, "replacementSearch must not be null"));
    }

    public void confirm(LocalDateTime confirmedAt) {
        if (status != RideMembershipStatus.REQUESTED && status != RideMembershipStatus.INVITED) {
            throw new IllegalStateException("Only a pending request or invitation can be confirmed");
        }
        this.status = RideMembershipStatus.CONFIRMED;
        this.joinedAt = Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
        this.decidedAt = confirmedAt;
    }

    public void cancel(LocalDateTime cancelledAt) {
        if (status == RideMembershipStatus.CANCELLED || status == RideMembershipStatus.REJECTED) {
            throw new IllegalStateException("This membership is already closed");
        }
        this.status = RideMembershipStatus.CANCELLED;
        this.cancelledAt = Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
    }

    public void reject(LocalDateTime rejectedAt) {
        if (status != RideMembershipStatus.REQUESTED && status != RideMembershipStatus.INVITED) {
            throw new IllegalStateException("Only a pending request or invitation can be rejected");
        }
        this.status = RideMembershipStatus.REJECTED;
        this.decidedAt = Objects.requireNonNull(rejectedAt, "rejectedAt must not be null");
    }

    public boolean isConfirmed() { return status == RideMembershipStatus.CONFIRMED; }
    public Long getId() { return id; }
    public Ride getRide() { return ride; }
    public User getUser() { return user; }
    public RideMembershipStatus getStatus() { return status; }
    public ReplacementSearch getReplacementSearch() { return replacementSearch; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}

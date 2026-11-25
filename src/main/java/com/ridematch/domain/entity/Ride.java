package com.ridematch.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Check;

import com.ridematch.domain.enums.RideKind;
import com.ridematch.domain.enums.RideStatus;

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

/** A ride request and coordination group within one verified community. */
@Entity
@Table(name = "rides", indexes = {
        @Index(name = "idx_rides_community_status_time", columnList = "community_id, status, requested_time"),
        @Index(name = "idx_rides_creator", columnList = "creator_user_id"),
        @Index(name = "idx_rides_coordinator", columnList = "coordinator_user_id"),
        @Index(name = "idx_rides_origin_destination", columnList = "origin, destination")
})
@Check(name = "ck_rides_capacity_positive", constraints = "passenger_capacity > 0")
@Check(name = "ck_rides_estimated_fare_positive", constraints = "estimated_total_fare > 0")
public class Ride extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rides_community"))
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rides_creator"))
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordinator_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rides_coordinator"))
    private User coordinator;

    @Column(nullable = false, length = 180)
    private String origin;

    @Column(nullable = false, length = 180)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_kind", nullable = false, length = 20)
    private RideKind kind;

    @Column(name = "requested_time", nullable = false)
    private LocalDateTime requestedTime;

    @Column(name = "requested_time_end")
    private LocalDateTime requestedTimeEnd;

    @Column(name = "estimated_total_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedTotalFare;

    @Column(name = "passenger_capacity", nullable = false)
    private int passengerCapacity;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideStatus status = RideStatus.OPEN;

    protected Ride() {
    }

    public Ride(Community community, User creator, String origin, String destination,
            RideKind kind, LocalDateTime requestedTime, BigDecimal estimatedTotalFare,
            int passengerCapacity, String notes) {
        this(community, creator, origin, destination, kind, requestedTime, null,
                estimatedTotalFare, passengerCapacity, notes);
    }

    public Ride(Community community, User creator, String origin, String destination,
            RideKind kind, LocalDateTime requestedTime, LocalDateTime requestedTimeEnd,
            BigDecimal estimatedTotalFare, int passengerCapacity, String notes) {
        this.community = Objects.requireNonNull(community, "community must not be null");
        this.creator = Objects.requireNonNull(creator, "creator must not be null");
        this.coordinator = creator;
        this.origin = requirePlace(origin, "origin");
        this.destination = requirePlace(destination, "destination");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.requestedTime = Objects.requireNonNull(requestedTime, "requestedTime must not be null");
        if (requestedTimeEnd != null && requestedTimeEnd.isBefore(requestedTime)) {
            throw new IllegalArgumentException("requestedTimeEnd must not be before requestedTime");
        }
        this.requestedTimeEnd = requestedTimeEnd;
        this.estimatedTotalFare = requirePositiveFare(estimatedTotalFare);
        if (passengerCapacity < 1) {
            throw new IllegalArgumentException("passengerCapacity must be at least 1");
        }
        this.passengerCapacity = passengerCapacity;
        this.notes = normalizeNotes(notes);
    }

    public void assignCoordinator(User coordinator) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
        if (status == RideStatus.OPEN) {
            status = RideStatus.COORDINATING;
        }
    }

    public void markTaxiArranged() {
        if (status == RideStatus.CANCELLED || status == RideStatus.COMPLETED) {
            throw new IllegalStateException("A closed ride cannot be marked as arranged");
        }
        status = RideStatus.TAXI_ARRANGED;
    }

    public void markCoordinating() {
        if (status == RideStatus.OPEN) {
            status = RideStatus.COORDINATING;
        }
    }

    public void cancel() {
        if (status == RideStatus.COMPLETED) {
            throw new IllegalStateException("A completed ride cannot be cancelled");
        }
        status = RideStatus.CANCELLED;
    }

    public void complete() {
        if (status == RideStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled ride cannot be completed");
        }
        status = RideStatus.COMPLETED;
    }

    public Long getId() { return id; }
    public Community getCommunity() { return community; }
    public User getCreator() { return creator; }
    public User getCoordinator() { return coordinator; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public RideKind getKind() { return kind; }
    public LocalDateTime getRequestedTime() { return requestedTime; }
    public LocalDateTime getRequestedTimeEnd() { return requestedTimeEnd; }
    public BigDecimal getEstimatedTotalFare() { return estimatedTotalFare; }
    public int getPassengerCapacity() { return passengerCapacity; }
    public String getNotes() { return notes; }
    public RideStatus getStatus() { return status; }

    private static String requirePlace(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requirePositiveFare(BigDecimal fare) {
        Objects.requireNonNull(fare, "estimatedTotalFare must not be null");
        if (fare.signum() <= 0) {
            throw new IllegalArgumentException("estimatedTotalFare must be positive");
        }
        return fare;
    }

    private static String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}

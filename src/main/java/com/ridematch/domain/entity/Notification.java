package com.ridematch.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.ridematch.domain.enums.NotificationType;

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

/** A persisted in-application notification for a user. */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_read_created", columnList = "user_id, read_at, created_at"),
        @Index(name = "idx_notifications_ride", columnList = "ride_id")
})
public class Notification extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notifications_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", foreignKey = @ForeignKey(name = "fk_notifications_ride"))
    private Ride ride;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {
    }

    public Notification(User user, Ride ride, NotificationType type, String title, String message) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.ride = ride;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.title = requireText(title, "title");
        this.message = requireText(message, "message");
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = Objects.requireNonNull(readAt, "readAt must not be null");
        }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Ride getRide() { return ride; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getReadAt() { return readAt; }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

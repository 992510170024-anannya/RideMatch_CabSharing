package com.ridematch.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.ridematch.domain.enums.VerificationStatus;

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

/** Connects a user to a community and records that community's verification decision. */
@Entity
@Table(name = "community_memberships",
        uniqueConstraints = @UniqueConstraint(name = "uk_community_membership_user_community", columnNames = {"user_id", "community_id"}),
        indexes = {
                @Index(name = "idx_community_memberships_community_status", columnList = "community_id, verification_status"),
                @Index(name = "idx_community_memberships_user", columnList = "user_id")
        })
public class CommunityMembership extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_memberships_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_memberships_community"))
    private Community community;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id", foreignKey = @ForeignKey(name = "fk_community_memberships_verifier"))
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    protected CommunityMembership() {
    }

    public CommunityMembership(User user, Community community) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.community = Objects.requireNonNull(community, "community must not be null");
    }

    public void verify(User verifier, LocalDateTime decisionTime) {
        this.verifiedBy = Objects.requireNonNull(verifier, "verifier must not be null");
        this.verifiedAt = Objects.requireNonNull(decisionTime, "decisionTime must not be null");
        this.verificationStatus = VerificationStatus.VERIFIED;
    }

    public void reject(User verifier, LocalDateTime decisionTime) {
        this.verifiedBy = Objects.requireNonNull(verifier, "verifier must not be null");
        this.verifiedAt = Objects.requireNonNull(decisionTime, "decisionTime must not be null");
        this.verificationStatus = VerificationStatus.REJECTED;
    }

    public boolean isVerified() { return verificationStatus == VerificationStatus.VERIFIED; }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Community getCommunity() { return community; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public User getVerifiedBy() { return verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
}

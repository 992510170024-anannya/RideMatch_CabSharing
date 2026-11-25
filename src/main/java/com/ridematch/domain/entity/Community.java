package com.ridematch.domain.entity;

import java.util.Objects;

import com.ridematch.domain.enums.CommunityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A college or company boundary for membership verification and ride matching. */
@Entity
@Table(name = "communities",
        uniqueConstraints = @UniqueConstraint(name = "uk_communities_name_type", columnNames = {"name", "community_type"}),
        indexes = @Index(name = "idx_communities_name", columnList = "name"))
public class Community extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "community_type", nullable = false, length = 20)
    private CommunityType type;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "verifier_user_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_communities_verifier"))
    private User verifier;

    protected Community() {
    }

    public Community(String name, CommunityType type, User verifier) {
        this.name = requireName(name);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.verifier = verifier;
    }

    public void assignVerifier(User verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CommunityType getType() { return type; }
    public User getVerifier() { return verifier; }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name.trim();
    }
}

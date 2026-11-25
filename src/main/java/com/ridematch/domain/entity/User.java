package com.ridematch.domain.entity;

import java.util.Locale;
import java.util.Objects;

import com.ridematch.domain.enums.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A RideMatch account; password material is stored only as a secure hash. */
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "user_role", nullable = false, length = 20, columnDefinition = "varchar(20) default 'USER'")
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private boolean active = true;

    protected User() {
    }

    public User(String fullName, String email, String passwordHash) {
        this.fullName = requireText(fullName, "fullName");
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "passwordHash");
    }

    public void updateProfile(String fullName, String email) {
        this.fullName = requireText(fullName, "fullName");
        this.email = normalizeEmail(email);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = requireText(passwordHash, "passwordHash");
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }

    private static String normalizeEmail(String email) {
        return requireText(email, "email").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

package com.ridematch.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridematch.domain.entity.User;

/** Persistence operations for user accounts. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);
}

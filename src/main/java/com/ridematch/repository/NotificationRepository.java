package com.ridematch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridematch.domain.entity.Notification;

/** Persistence operations for user notifications. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByUser_IdAndReadAtIsNull(Long userId);
}

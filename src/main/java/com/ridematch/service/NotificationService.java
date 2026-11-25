package com.ridematch.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.entity.Notification;
import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.entity.RideMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.NotificationType;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.dto.community.NotificationResponse;
import com.ridematch.exception.ForbiddenOperationException;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.repository.NotificationRepository;
import com.ridematch.repository.RideMembershipRepository;
import com.ridematch.repository.UserRepository;
import com.ridematch.domain.enums.VerificationStatus;
import com.ridematch.repository.CommunityMembershipRepository;

/** Persists in-app notifications for ride participants and invitees. */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RideMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CommunityMembershipRepository communityMembershipRepository;

    public NotificationService(NotificationRepository notificationRepository,
            RideMembershipRepository membershipRepository, UserRepository userRepository,
            CommunityMembershipRepository communityMembershipRepository) {
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.communityMembershipRepository = communityMembershipRepository;
    }

    public void notifyCommunityOfRide(Ride ride) {
        communityMembershipRepository.findByCommunity_IdAndVerificationStatusOrderByCreatedAt(
                ride.getCommunity().getId(), VerificationStatus.VERIFIED).stream()
                .map(com.ridematch.domain.entity.CommunityMembership::getUser)
                .filter(User::isActive)
                .filter(user -> !user.getId().equals(ride.getCreator().getId()))
                .forEach(user -> notifyUser(user, ride, NotificationType.RIDE_CREATED,
                        "New ride in " + ride.getCommunity().getName(),
                        ride.getCreator().getFullName() + " created a ride from " + ride.getOrigin()
                                + " to " + ride.getDestination() + " for "
                                + ride.getRequestedTime().toLocalDate() + " at "
                                + ride.getRequestedTime().toLocalTime()
                                + (ride.getRequestedTimeEnd() == null ? "."
                                        : "–" + ride.getRequestedTimeEnd().toLocalTime() + ".")));
    }

    public void notifyUser(User user, Ride ride, NotificationType type, String title, String message) {
        notificationRepository.save(new Notification(user, ride, type, title, message));
    }

    public void notifyConfirmedPassengers(Ride ride, Collection<Long> excludedUserIds,
            NotificationType type, String title, String message) {
        Set<Long> excluded = excludedUserIds == null ? Set.of() : new HashSet<>(excludedUserIds);
        membershipRepository.findByRide_IdAndStatusOrderByCreatedAtAsc(
                ride.getId(), RideMembershipStatus.CONFIRMED).stream()
                .map(RideMembership::getUser)
                .filter(user -> !excluded.contains(user.getId()))
                .forEach(user -> notifyUser(user, ride, type, title, message));
    }

    public void notifyUsers(Collection<User> users, Ride ride, NotificationType type,
            String title, String message) {
        Set<Long> notified = new HashSet<>();
        users.stream().filter(user -> user.getId() == null || notified.add(user.getId()))
                .forEach(user -> notifyUser(user, ride, type, title, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(String email) {
        User user = findUser(email);
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional
    public void markRead(String email, Long notificationId) {
        User user = findUser(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("You can only update your own notifications");
        }
        notification.markRead(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }
}

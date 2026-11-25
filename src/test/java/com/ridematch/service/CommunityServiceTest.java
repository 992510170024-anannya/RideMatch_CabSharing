package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.CommunityMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.UserRole;
import com.ridematch.domain.enums.VerificationStatus;
import com.ridematch.exception.ConflictException;
import com.ridematch.exception.ForbiddenOperationException;
import com.ridematch.repository.CommunityMembershipRepository;
import com.ridematch.repository.CommunityRepository;
import com.ridematch.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private CommunityMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;

    private CommunityService communityService;
    private User user;
    private Community community;

    @BeforeEach
    void setUp() {
        communityService = new CommunityService(communityRepository, membershipRepository, userRepository);
        user = new User("Alex Rider", "alex@example.com", "bcrypt-hash");
        community = new Community("North College", CommunityType.COLLEGE, null);
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(community, "id", 42L);
    }

    @Test
    void duplicateMembershipRequestIsRejected() {
        CommunityMembership existing = new CommunityMembership(user, community);
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(communityRepository.findById(42L)).thenReturn(Optional.of(community));
        when(membershipRepository.findByUser_IdAndCommunity_Id(7L, 42L)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> communityService.requestMembership("Alex@Example.com", 42L));
        verify(membershipRepository, never()).saveAndFlush(existing);
    }

    @Test
    void unapprovedUserCannotPassRideAuthorizationCheck() {
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(communityRepository.findById(42L)).thenReturn(Optional.of(community));
        when(membershipRepository.existsByUser_IdAndCommunity_IdAndVerificationStatus(
                7L, 42L, VerificationStatus.VERIFIED)).thenReturn(false);

        assertThrows(ForbiddenOperationException.class,
                () -> communityService.requireApprovedMember("alex@example.com", 42L));
    }

    @Test
    void approvedUserPassesRideAuthorizationCheck() {
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(communityRepository.findById(42L)).thenReturn(Optional.of(community));
        when(membershipRepository.existsByUser_IdAndCommunity_IdAndVerificationStatus(
                7L, 42L, VerificationStatus.VERIFIED)).thenReturn(true);

        assertDoesNotThrow(() -> communityService.requireApprovedMember("alex@example.com", 42L));
    }

    @Test
    void nonAdminCannotRegisterCommunity() {
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));

        assertThrows(ForbiddenOperationException.class,
                () -> communityService.registerCommunity("alex@example.com", "North College", CommunityType.COLLEGE));
        verify(communityRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(Community.class));
    }

    @Test
    void adminCanApprovePendingMembership() {
        ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
        CommunityMembership pending = new CommunityMembership(user, community);
        ReflectionTestUtils.setField(pending, "id", 99L);
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findById(99L)).thenReturn(Optional.of(pending));
        when(membershipRepository.save(pending)).thenReturn(pending);

        var response = communityService.approveMembership("alex@example.com", 99L);

        assertEquals(VerificationStatus.VERIFIED, response.status());
        verify(membershipRepository).save(pending);
    }
}

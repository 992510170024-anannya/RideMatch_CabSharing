package com.ridematch.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.entity.CommunityMembership;
import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.CommunityType;
import com.ridematch.domain.enums.UserRole;
import com.ridematch.domain.enums.VerificationStatus;
import com.ridematch.dto.community.CommunityResponse;
import com.ridematch.dto.community.MembershipRequestResponse;
import com.ridematch.exception.ConflictException;
import com.ridematch.exception.ForbiddenOperationException;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.repository.CommunityMembershipRepository;
import com.ridematch.repository.CommunityRepository;
import com.ridematch.repository.UserRepository;

/** Community registration, membership requests, and administrative verification. */
@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public CommunityService(CommunityRepository communityRepository,
            CommunityMembershipRepository membershipRepository, UserRepository userRepository) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunityResponse> listCommunities() {
        return communityRepository.findAllByOrderByName().stream()
                .map(CommunityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityResponse> listVerifiedCommunities(String userEmail) {
        User user = findUser(userEmail);
        return membershipRepository.findByUser_IdAndVerificationStatus(user.getId(), VerificationStatus.VERIFIED)
                .stream().map(CommunityMembership::getCommunity)
                .map(CommunityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CommunityResponse getCommunity(Long communityId) {
        return CommunityResponse.from(findCommunity(communityId));
    }

    @Transactional(readOnly = true)
    public List<MembershipRequestResponse> listVerifiedMembers(String adminEmail, Long communityId) {
        requireAdmin(adminEmail);
        findCommunity(communityId);
        return membershipRepository.findByCommunity_IdAndVerificationStatusOrderByCreatedAt(
                communityId, VerificationStatus.VERIFIED).stream().map(MembershipRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countPendingMemberships(Long communityId) {
        return membershipRepository.countByCommunity_IdAndVerificationStatus(communityId, VerificationStatus.PENDING);
    }

    @Transactional
    public CommunityResponse registerCommunity(String adminEmail, String name, CommunityType type) {
        User admin = requireAdmin(adminEmail);
        if (communityRepository.existsByNameIgnoreCaseAndType(name.trim(), type)) {
            throw new ConflictException("A community with this name and type already exists");
        }

        Community community = new Community(name, type, admin);
        try {
            return CommunityResponse.from(communityRepository.saveAndFlush(community));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A community with this name and type already exists");
        }
    }

    @Transactional
    public MembershipRequestResponse requestMembership(String userEmail, Long communityId) {
        User user = findUser(userEmail);
        Community community = findCommunity(communityId);
        if (membershipRepository.findByUser_IdAndCommunity_Id(user.getId(), communityId).isPresent()) {
            throw new ConflictException("A membership request already exists for this community");
        }

        CommunityMembership membership = new CommunityMembership(user, community);
        try {
            return MembershipRequestResponse.from(membershipRepository.saveAndFlush(membership));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A membership request already exists for this community");
        }
    }

    @Transactional(readOnly = true)
    public List<MembershipRequestResponse> listPendingMemberships(String adminEmail) {
        requireAdmin(adminEmail);
        return membershipRepository.findByVerificationStatusOrderByCreatedAt(VerificationStatus.PENDING)
                .stream().map(MembershipRequestResponse::from).toList();
    }

    @Transactional
    public MembershipRequestResponse approveMembership(String adminEmail, Long membershipId) {
        User admin = requireAdmin(adminEmail);
        CommunityMembership membership = findMembership(membershipId);
        requirePending(membership);
        membership.verify(admin, LocalDateTime.now());
        return MembershipRequestResponse.from(membershipRepository.save(membership));
    }

    @Transactional
    public MembershipRequestResponse rejectMembership(String adminEmail, Long membershipId) {
        User admin = requireAdmin(adminEmail);
        CommunityMembership membership = findMembership(membershipId);
        requirePending(membership);
        membership.reject(admin, LocalDateTime.now());
        return MembershipRequestResponse.from(membershipRepository.save(membership));
    }

    /** Authorization precondition for any future community ride operation. */
    @Transactional(readOnly = true)
    public void requireApprovedMember(String userEmail, Long communityId) {
        User user = findUser(userEmail);
        findCommunity(communityId);
        boolean verified = membershipRepository.existsByUser_IdAndCommunity_IdAndVerificationStatus(
                user.getId(), communityId, VerificationStatus.VERIFIED);
        if (!verified) {
            throw new ForbiddenOperationException("Approved membership in this community is required");
        }
    }

    private User requireAdmin(String email) {
        User user = findUser(email);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenOperationException("Administrator access is required");
        }
        return user;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }

    private Community findCommunity(Long communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community was not found"));
    }

    private CommunityMembership findMembership(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership request was not found"));
    }

    private static void requirePending(CommunityMembership membership) {
        if (membership.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new ConflictException("Only pending membership requests can be reviewed");
        }
    }
}

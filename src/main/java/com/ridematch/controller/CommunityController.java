package com.ridematch.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ridematch.dto.community.CommunityRegistrationRequest;
import com.ridematch.exception.ConflictException;
import com.ridematch.service.CommunityService;

import jakarta.validation.Valid;

/** Community listing, membership requests, and administrator verification pages. */
@Controller
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/communities")
    public String listCommunities(Authentication authentication, Model model) {
        model.addAttribute("communities", communityService.listCommunities());
        model.addAttribute("verifiedCommunityIds", communityService.listVerifiedCommunities(authentication.getName())
                .stream().map(community -> community.id()).toList());
        model.addAttribute("isAdmin", authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        return "community/list";
    }

    @PostMapping("/communities/{communityId}/membership-requests")
    public String requestMembership(@PathVariable Long communityId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        communityService.requestMembership(authentication.getName(), communityId);
        redirectAttributes.addFlashAttribute("notice", "Membership request submitted for review.");
        return "redirect:/communities";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/communities/new")
    public String newCommunity(Model model) {
        model.addAttribute("communityRegistrationRequest", new CommunityRegistrationRequest());
        return "community/new";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/communities")
    public String registerCommunity(Authentication authentication,
            @Valid @ModelAttribute("communityRegistrationRequest") CommunityRegistrationRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "community/new";
        }
        try {
            communityService.registerCommunity(authentication.getName(), request.getName(), request.getType());
            return "redirect:/communities?communityRegistered";
        } catch (ConflictException exception) {
            bindingResult.rejectValue("name", "community.conflict", exception.getMessage());
            return "community/new";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/community-memberships")
    public String pendingMemberships(Authentication authentication, Model model) {
        model.addAttribute("requests", communityService.listPendingMemberships(authentication.getName()));
        return "community/pending-memberships";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/communities/{communityId}")
    public String adminCommunity(@PathVariable Long communityId, Authentication authentication, Model model) {
        model.addAttribute("community", communityService.getCommunity(communityId));
        model.addAttribute("members", communityService.listVerifiedMembers(authentication.getName(), communityId));
        model.addAttribute("pendingCount", communityService.countPendingMemberships(communityId));
        return "admin/community";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/community-memberships/{membershipId}/approve")
    public String approveMembership(@PathVariable Long membershipId, Authentication authentication) {
        communityService.approveMembership(authentication.getName(), membershipId);
        return "redirect:/admin/community-memberships?reviewed";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/community-memberships/{membershipId}/reject")
    public String rejectMembership(@PathVariable Long membershipId, Authentication authentication) {
        communityService.rejectMembership(authentication.getName(), membershipId);
        return "redirect:/admin/community-memberships?reviewed";
    }
}

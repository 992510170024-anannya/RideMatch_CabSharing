package com.ridematch.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ridematch.dto.ride.CancellationPolicyResult;
import com.ridematch.service.RideGroupService;
import com.ridematch.service.RideDashboardService;

/** Web actions for join decisions, invitations, group cancellation, and fare review. */
@Controller
public class RideGroupController {

    private final RideGroupService rideGroupService;
    private final RideDashboardService rideDashboardService;

    public RideGroupController(RideGroupService rideGroupService, RideDashboardService rideDashboardService) {
        this.rideGroupService = rideGroupService;
        this.rideDashboardService = rideDashboardService;
    }

    @GetMapping("/rides/{rideId}/group")
    public String group(@PathVariable Long rideId, Authentication authentication, Model model) {
        model.addAttribute("group", rideGroupService.getGroup(authentication.getName(), rideId));
        return "ride/group";
    }

    @GetMapping("/rides/my-rides")
    public String myRides(Authentication authentication, Model model) {
        model.addAttribute("rides", rideDashboardService.listMyCurrentRides(authentication.getName()));
        return "ride/my-rides";
    }

    @GetMapping("/ride-invitations")
    public String invitations(Authentication authentication, Model model) {
        model.addAttribute("invitations", rideGroupService.listMyReplacementInvitations(authentication.getName()));
        return "ride/invitations";
    }

    @PostMapping("/rides/{rideId}/join")
    public String requestToJoin(@PathVariable Long rideId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        rideGroupService.requestToJoin(authentication.getName(), rideId);
        redirectAttributes.addFlashAttribute("notice", "Join request sent to the ride coordinator.");
        return "redirect:/rides/" + rideId + "/group";
    }

    @PostMapping("/rides/{rideId}/memberships/{membershipId}/approve")
    public String approveJoinRequest(@PathVariable Long rideId, @PathVariable Long membershipId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        rideGroupService.acceptJoinRequest(authentication.getName(), rideId, membershipId);
        redirectAttributes.addFlashAttribute("notice", "Passenger request approved; confirmed seats and fare share were updated.");
        return "redirect:/rides/" + rideId + "/group";
    }

    @PostMapping("/rides/{rideId}/memberships/{membershipId}/reject")
    public String rejectJoinRequest(@PathVariable Long rideId, @PathVariable Long membershipId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        rideGroupService.rejectJoinRequest(authentication.getName(), rideId, membershipId);
        redirectAttributes.addFlashAttribute("notice", "Passenger request declined.");
        return "redirect:/rides/" + rideId + "/group";
    }

    @PostMapping("/rides/{rideId}/taxi-arranged")
    public String markTaxiArranged(@PathVariable Long rideId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        rideGroupService.markTaxiArranged(authentication.getName(), rideId);
        redirectAttributes.addFlashAttribute("notice", "External taxi arrangement recorded.");
        return "redirect:/rides/" + rideId + "/group";
    }

    @PostMapping("/rides/{rideId}/memberships/{membershipId}/cancel")
    public String cancelMembership(@PathVariable Long rideId, @PathVariable Long membershipId,
            @RequestParam(required = false) String reason, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        CancellationPolicyResult policy = rideGroupService.cancelOwnMembership(
                authentication.getName(), rideId, membershipId, reason);
        redirectAttributes.addFlashAttribute("policyMessage", policy.explanation());
        return "redirect:/rides/" + rideId + "/group";
    }

    @PostMapping("/rides/{rideId}/cancel")
    public String cancelRide(@PathVariable Long rideId, @RequestParam(required = false) String reason,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        CancellationPolicyResult policy = rideGroupService.cancelRide(authentication.getName(), rideId, reason);
        redirectAttributes.addFlashAttribute("policyMessage", policy.explanation());
        return "redirect:/rides/" + rideId + "/group";
    }

    @PostMapping("/rides/{rideId}/memberships/{membershipId}/accept-invitation")
    public String acceptReplacementInvitation(@PathVariable Long rideId, @PathVariable Long membershipId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        boolean accepted = rideGroupService.acceptReplacementInvitation(
                authentication.getName(), rideId, membershipId);
        redirectAttributes.addFlashAttribute("notice", accepted
                ? "Replacement seat confirmed; the estimated fare share was updated."
                : "The vacant seat was already filled.");
        return "redirect:/rides/" + rideId + "/group";
    }

    @PostMapping("/rides/{rideId}/memberships/{membershipId}/decline-invitation")
    public String declineReplacementInvitation(@PathVariable Long rideId, @PathVariable Long membershipId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        rideGroupService.rejectReplacementInvitation(authentication.getName(), rideId, membershipId);
        redirectAttributes.addFlashAttribute("notice", "Invitation declined. The search may invite another verified member.");
        return "redirect:/ride-invitations";
    }
}

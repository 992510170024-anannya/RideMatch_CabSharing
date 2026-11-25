package com.ridematch.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ridematch.service.CommunityService;
import com.ridematch.service.NotificationService;
import com.ridematch.service.RideDashboardService;
import com.ridematch.service.RideGroupService;
import com.ridematch.service.UserService;

/** Landing, dashboard, ride-request inbox, and administrator overview pages. */
@Controller
public class DashboardController {

    private final UserService userService;
    private final RideDashboardService rideDashboardService;
    private final RideGroupService rideGroupService;
    private final NotificationService notificationService;
    private final CommunityService communityService;

    public DashboardController(UserService userService, RideDashboardService rideDashboardService,
            RideGroupService rideGroupService, NotificationService notificationService,
            CommunityService communityService) {
        this.userService = userService;
        this.rideDashboardService = rideDashboardService;
        this.rideGroupService = rideGroupService;
        this.notificationService = notificationService;
        this.communityService = communityService;
    }

    @GetMapping("/")
    public String landing(Authentication authentication, Model model) {
        model.addAttribute("signedIn", authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .noneMatch(authority -> authority.getAuthority().equals("ROLE_ANONYMOUS")));
        return "landing";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("profile", userService.getProfile(email));
        var rides = rideDashboardService.listMyCurrentRides(email);
        model.addAttribute("rides", rides);
        model.addAttribute("createdRideCount", rides.stream().filter(ride -> ride.coordinator()).count());
        model.addAttribute("pendingJoinRequests", rideDashboardService.listPendingJoinRequests(email));
        model.addAttribute("invitations", rideGroupService.listMyReplacementInvitations(email));
        var notifications = notificationService.listForUser(email);
        model.addAttribute("notifications", notifications);
        model.addAttribute("cancellationUpdates", notifications.stream()
                .filter(notification -> notification.type().name().equals("CANCELLATION"))
                .toList());
        model.addAttribute("verifiedCommunities", communityService.listVerifiedCommunities(email));
        return "dashboard/index";
    }

    @GetMapping("/ride-requests")
    public String joinRequests(Authentication authentication, Model model) {
        model.addAttribute("requests", rideDashboardService.listPendingJoinRequests(authentication.getName()));
        return "ride/requests";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminDashboard(Authentication authentication, Model model) {
        model.addAttribute("communities", communityService.listCommunities());
        model.addAttribute("requests", communityService.listPendingMemberships(authentication.getName()));
        return "admin/dashboard";
    }
}

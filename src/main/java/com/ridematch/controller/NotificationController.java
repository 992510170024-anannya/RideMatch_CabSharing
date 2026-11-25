package com.ridematch.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ridematch.service.NotificationService;

/** Authenticated in-app notification listing and read action. */
@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String notifications(Authentication authentication, Model model) {
        model.addAttribute("notifications", notificationService.listForUser(authentication.getName()));
        return "notification/list";
    }

    @PostMapping("/notifications/{notificationId}/read")
    public String markRead(@PathVariable Long notificationId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        notificationService.markRead(authentication.getName(), notificationId);
        redirectAttributes.addFlashAttribute("notice", "Notification marked as read.");
        return "redirect:/notifications";
    }
}

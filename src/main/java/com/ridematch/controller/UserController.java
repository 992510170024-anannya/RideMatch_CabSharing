package com.ridematch.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.ridematch.dto.user.ProfileUpdateRequest;
import com.ridematch.exception.ConflictException;
import com.ridematch.service.UserService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Authenticated profile view and safe profile updates. */
@Controller
public class UserController {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    public UserController(UserService userService, SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        var profile = userService.getProfile(authentication.getName());
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(profile.fullName());
        request.setEmail(profile.email());
        model.addAttribute("profile", profile);
        model.addAttribute("profileUpdateRequest", request);
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
            @Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest request,
            BindingResult bindingResult, Model model,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", userService.getProfile(authentication.getName()));
            return "user/profile";
        }
        try {
            var updatedProfile = userService.updateProfile(authentication.getName(), request);
            SecurityContext updatedContext = SecurityContextHolder.createEmptyContext();
            updatedContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    updatedProfile.email(), null, authentication.getAuthorities()));
            SecurityContextHolder.setContext(updatedContext);
            securityContextRepository.saveContext(updatedContext, servletRequest, servletResponse);
            return "redirect:/profile?updated";
        } catch (ConflictException exception) {
            bindingResult.addError(new FieldError("profileUpdateRequest", "email", exception.getMessage()));
            model.addAttribute("profile", userService.getProfile(authentication.getName()));
            return "user/profile";
        }
    }
}

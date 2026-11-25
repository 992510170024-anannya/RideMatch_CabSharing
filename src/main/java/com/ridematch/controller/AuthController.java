package com.ridematch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.ridematch.dto.auth.RegistrationRequest;
import com.ridematch.exception.ConflictException;
import com.ridematch.service.AuthService;

import jakarta.validation.Valid;

/** Registration and login-page routes; Spring Security processes login and logout. */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
            BindingResult bindingResult) {
        if (request.getPassword() != null && request.getConfirmPassword() != null
                && !request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.addError(new FieldError("registrationRequest", "confirmPassword",
                    "Passwords do not match"));
        }
        if (bindingResult.hasErrors()) {
            request.setPassword(null);
            request.setConfirmPassword(null);
            return "auth/register";
        }

        try {
            authService.register(request);
            return "redirect:/auth/login?registered";
        } catch (ConflictException exception) {
            bindingResult.rejectValue("email", "email.conflict", exception.getMessage());
            request.setPassword(null);
            request.setConfirmPassword(null);
            return "auth/register";
        }
    }

    @GetMapping("/auth/login")
    public String loginPage() {
        return "auth/login";
    }
}

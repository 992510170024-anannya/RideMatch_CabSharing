package com.ridematch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.UserRole;
import com.ridematch.dto.auth.RegistrationRequest;
import com.ridematch.exception.ConflictException;
import com.ridematch.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "RideMatch#2026Strong";

    @Mock
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void registerStoresBcryptHashAndAssignsUserRole() {
        RegistrationRequest request = request(" Alex Rider ", "ALEX@example.com", RAW_PASSWORD, RAW_PASSWORD);
        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User savedUser = captor.getValue();
        assertNotEquals(RAW_PASSWORD, savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, savedUser.getPasswordHash()));
        assertEquals("alex@example.com", savedUser.getEmail());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertEquals("alex@example.com", response.email());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.register(request("Alex Rider", "alex@example.com", RAW_PASSWORD, RAW_PASSWORD)));
    }

    @Test
    void registerRejectsMismatchedPasswordConfirmation() {
        assertThrows(ConflictException.class,
                () -> authService.register(request("Alex Rider", "alex@example.com", RAW_PASSWORD, "Different#2026")));
    }

    private static RegistrationRequest request(String fullName, String email, String password, String confirmation) {
        RegistrationRequest request = new RegistrationRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(confirmation);
        return request;
    }
}

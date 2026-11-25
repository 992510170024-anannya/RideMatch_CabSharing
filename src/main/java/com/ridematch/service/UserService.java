package com.ridematch.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.entity.User;
import com.ridematch.dto.user.ProfileUpdateRequest;
import com.ridematch.dto.user.UserResponse;
import com.ridematch.exception.ConflictException;
import com.ridematch.exception.ResourceNotFoundException;
import com.ridematch.repository.UserRepository;

/** Reads and updates the authenticated user's safe profile fields. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return UserResponse.from(findUser(email));
    }

    @Transactional
    public UserResponse updateProfile(String currentEmail, ProfileUpdateRequest request) {
        User user = findUser(currentEmail);
        String newEmail = normalizeEmail(request.getEmail());
        if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        user.updateProfile(request.getFullName(), newEmail);
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("An account with this email already exists");
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

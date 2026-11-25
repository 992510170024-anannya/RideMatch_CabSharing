package com.ridematch.dto.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RegistrationRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validStrongRegistrationPassesValidation() {
        RegistrationRequest request = request("Alex Rider", "alex@example.com", "RideMatch#2026Strong");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void invalidEmailAndWeakPasswordAreRejected() {
        RegistrationRequest request = request("Alex Rider", "not-an-email", "weak");

        Set<?> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void missingRequiredFieldsAreRejected() {
        assertFalse(validator.validate(new RegistrationRequest()).isEmpty());
    }

    private static RegistrationRequest request(String fullName, String email, String password) {
        RegistrationRequest request = new RegistrationRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(password);
        return request;
    }
}

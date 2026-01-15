package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GlobalExceptionHandler validation error handling.
 * Tests both field-level and cross-field constraint violations.
 *
 * <p>Note: @AssertTrue constraints on methods (like isPasswordsMatch()) create field-level
 * FieldError instances with the property name derived from the method name (e.g., "passwordsMatch"),
 * not ObjectError instances. True ObjectError instances are created by class-level validation
 * annotations (like @ScriptAssert or custom validators), but those scenarios are less common.
 * The GlobalExceptionHandler safely handles both FieldError and ObjectError types.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@Import(GlobalExceptionHandlerValidationTest.TestControllerConfig.class)
public class GlobalExceptionHandlerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test DTO with field-level validation constraints.
     */
    public record FieldValidationRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
        String name,

        @NotBlank(message = "Email is required")
        String email
    ) {
    }

    /**
     * Test DTO with cross-field validation constraint using @AssertTrue.
     * Validates that password and confirmPassword match.
     * Note: @AssertTrue on methods creates FieldError, not ObjectError.
     */
    public record ObjectValidationRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
    ) {
        /**
         * Cross-field validation: passwords must match.
         * Creates a FieldError with property name "passwordsMatch".
         */
        @AssertTrue(message = "Passwords must match")
        public boolean isPasswordsMatch() {
            if (password == null || confirmPassword == null) {
                return true; // Let @NotBlank handle null cases
            }
            return password.equals(confirmPassword);
        }
    }

    /**
     * Test DTO with both field-level and cross-field validation constraints.
     */
    public record MixedValidationRequest(
        @NotBlank(message = "Start date is required")
        String startDate,

        @NotBlank(message = "End date is required")
        String endDate
    ) {
        /**
         * Cross-field validation: end date must be after start date.
         * Creates a FieldError with property name "dateRangeValid".
         */
        @AssertTrue(message = "End date must be after start date")
        public boolean isDateRangeValid() {
            if (startDate == null || endDate == null || startDate.isBlank() || endDate.isBlank()) {
                return true; // Let @NotBlank handle null/blank cases
            }
            return endDate.compareTo(startDate) > 0;
        }
    }

    /**
     * Test configuration to expose endpoints for validation testing.
     */
    @TestConfiguration
    public static class TestControllerConfig {

        @Bean
        public ValidationTestController validationTestController() {
            return new ValidationTestController();
        }
    }

    @RestController
    public static class ValidationTestController {

        @PostMapping("/api/test/field-validation")
        public ResponseEntity<String> testFieldValidation(
            @Valid @RequestBody FieldValidationRequest request
        ) {
            return ResponseEntity.ok("Valid");
        }

        @PostMapping("/api/test/object-validation")
        public ResponseEntity<String> testObjectValidation(
            @Valid @RequestBody ObjectValidationRequest request
        ) {
            return ResponseEntity.ok("Valid");
        }

        @PostMapping("/api/test/mixed-validation")
        public ResponseEntity<String> testMixedValidation(
            @Valid @RequestBody MixedValidationRequest request
        ) {
            return ResponseEntity.ok("Valid");
        }
    }

    @Test
    public void testFieldValidation_SingleFieldError() throws Exception {
        FieldValidationRequest request = new FieldValidationRequest(
            "",  // Empty name (violates @NotBlank and @Size)
            "test@example.com"
        );

        mockMvc.perform(post("/api/test/field-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists())
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testFieldValidation_MultipleFieldErrors() throws Exception {
        FieldValidationRequest request = new FieldValidationRequest(
            "ab",  // Too short (violates @Size)
            ""     // Empty email (violates @NotBlank)
        );

        mockMvc.perform(post("/api/test/field-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists())
            .andExpect(jsonPath("$.fieldErrors", hasKey("name")))
            .andExpect(jsonPath("$.fieldErrors", hasKey("email")))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testFieldValidation_AllFieldsValid() throws Exception {
        FieldValidationRequest request = new FieldValidationRequest(
            "John Doe",
            "john@example.com"
        );

        mockMvc.perform(post("/api/test/field-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    public void testObjectValidation_PasswordMismatch() throws Exception {
        ObjectValidationRequest request = new ObjectValidationRequest(
            "testuser",
            "password123",
            "password456"  // Passwords don't match
        );

        mockMvc.perform(post("/api/test/object-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists())
            .andExpect(jsonPath("$.fieldErrors.passwordsMatch")
                .value("Passwords must match"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testObjectValidation_PasswordMatch() throws Exception {
        ObjectValidationRequest request = new ObjectValidationRequest(
            "testuser",
            "password123",
            "password123"  // Passwords match
        );

        mockMvc.perform(post("/api/test/object-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    public void testMixedValidation_FieldAndObjectErrors() throws Exception {
        MixedValidationRequest request = new MixedValidationRequest(
            "",           // Empty start date (field error)
            "2024-01-01"  // Valid end date
        );

        mockMvc.perform(post("/api/test/mixed-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists())
            .andExpect(jsonPath("$.fieldErrors", hasKey("startDate")))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testMixedValidation_ObjectError() throws Exception {
        MixedValidationRequest request = new MixedValidationRequest(
            "2024-12-31",  // Start date after end date
            "2024-01-01"   // End date before start date (violates cross-field constraint)
        );

        mockMvc.perform(post("/api/test/mixed-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists())
            .andExpect(jsonPath("$.fieldErrors.dateRangeValid")
                .value("End date must be after start date"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testMixedValidation_AllValid() throws Exception {
        MixedValidationRequest request = new MixedValidationRequest(
            "2024-01-01",
            "2024-12-31"
        );

        mockMvc.perform(post("/api/test/mixed-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    public void testObjectValidation_WithFieldErrors() throws Exception {
        ObjectValidationRequest request = new ObjectValidationRequest(
            "",          // Empty username (field error)
            "pass",      // Password too short (field error)
            "different"  // Passwords don't match (cross-field constraint error)
        );

        mockMvc.perform(post("/api/test/object-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists())
            .andExpect(jsonPath("$.fieldErrors", hasKey("username")))
            .andExpect(jsonPath("$.fieldErrors", hasKey("password")))
            .andExpect(jsonPath("$.fieldErrors", hasKey("passwordsMatch")))
            .andExpect(jsonPath("$.timestamp").exists());
    }
}

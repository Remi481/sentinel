package com.remi481.sentinel.security;

import com.remi481.sentinel.SentinelApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupConfigurationValidatorTest {

    private static final String VALID_SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private static final String VALID_KEY = "enrollment-key-1234567890abcdef";
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "admin-password-1234";

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("sentinel.security.jwt-secret", VALID_SECRET)
                .withProperty("sentinel.security.enrollment-key", VALID_KEY)
                .withProperty("sentinel.security.admin-username", VALID_USERNAME)
                .withProperty("sentinel.security.admin-password", VALID_PASSWORD);
    }

    private List<String> problems(MockEnvironment environment) {
        StartupConfigurationValidator validator = new StartupConfigurationValidator();
        validator.setEnvironment(environment);
        return validator.collectProblems();
    }

    @Test
    void validConfigurationHasNoProblems() {
        assertTrue(problems(validEnvironment()).isEmpty());
    }

    @Test
    void missingJwtSecretIsNamed() {
        MockEnvironment environment = validEnvironment().withProperty("sentinel.security.jwt-secret", "");
        List<String> problems = problems(environment);
        assertFalse(problems.isEmpty());
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("SENTINEL_JWT_SECRET")));
    }

    @Test
    void shortJwtSecretIsNamed() {
        MockEnvironment environment = validEnvironment().withProperty("sentinel.security.jwt-secret", "too-short");
        assertTrue(problems(environment).stream().anyMatch(problem -> problem.contains("SENTINEL_JWT_SECRET")));
    }

    @Test
    void missingEnrollmentKeyIsNamed() {
        MockEnvironment environment = validEnvironment().withProperty("sentinel.security.enrollment-key", "");
        assertTrue(problems(environment).stream().anyMatch(problem -> problem.contains("SENTINEL_ENROLLMENT_KEY")));
    }

    @Test
    void shortEnrollmentKeyIsNamed() {
        MockEnvironment environment = validEnvironment().withProperty("sentinel.security.enrollment-key", "short");
        assertTrue(problems(environment).stream().anyMatch(problem -> problem.contains("SENTINEL_ENROLLMENT_KEY")));
    }

    @Test
    void adminPairMismatchIsNamed() {
        MockEnvironment environment = validEnvironment()
                .withProperty("sentinel.security.admin-username", VALID_USERNAME)
                .withProperty("sentinel.security.admin-password", "");
        List<String> problems = problems(environment);
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("SENTINEL_ADMIN_USERNAME")
                && problem.contains("SENTINEL_ADMIN_PASSWORD")));
    }

    @Test
    void shortAdminPasswordIsNamed() {
        MockEnvironment environment = validEnvironment().withProperty("sentinel.security.admin-password", "short");
        assertTrue(problems(environment).stream().anyMatch(problem -> problem.contains("SENTINEL_ADMIN_PASSWORD")));
    }

    @Test
    void noAdminConfiguredIsAllowed() {
        MockEnvironment environment = validEnvironment()
                .withProperty("sentinel.security.admin-username", "")
                .withProperty("sentinel.security.admin-password", "");
        assertTrue(problems(environment).isEmpty());
    }

    @Test
    void productionRequiresDatabasePassword() {
        MockEnvironment environment = validEnvironment();
        environment.setActiveProfiles("prod");
        assertTrue(problems(environment).stream().anyMatch(problem -> problem.contains("SENTINEL_DB_PASSWORD")));
    }

    @Test
    void productionRejectsDefaultDatabasePassword() {
        MockEnvironment environment = validEnvironment()
                .withProperty("SENTINEL_DB_PASSWORD", "sentinel");
        environment.setActiveProfiles("prod");
        assertTrue(problems(environment).stream().anyMatch(problem -> problem.contains("SENTINEL_DB_PASSWORD")));
    }

    @Test
    void productionNeedsNoDatabasePasswordWhenNotActive() {
        assertDoesNotThrow(() -> problems(validEnvironment()));
    }

    @Test
    void contextFailsWithMissingJwtSecretAndNamesIt() {
        assertStartupFailsNaming("SENTINEL_JWT_SECRET", "--sentinel.security.jwt-secret=");
    }

    @Test
    void contextFailsWithMissingEnrollmentKeyAndNamesIt() {
        assertStartupFailsNaming("SENTINEL_ENROLLMENT_KEY", "--sentinel.security.enrollment-key=");
    }

    @Test
    void contextFailsWithMismatchedAdminAndNamesIt() {
        assertStartupFailsNaming("SENTINEL_ADMIN_PASSWORD",
                "--sentinel.security.admin-username=someone",
                "--sentinel.security.admin-password=");
    }

    @Test
    void contextFailsWithDefaultDatabasePasswordInProductionAndNamesIt() {
        assertStartupFailsNaming("SENTINEL_DB_PASSWORD",
                "--spring.profiles.active=prod",
                "--SENTINEL_DB_PASSWORD=sentinel");
    }

    private void assertStartupFailsNaming(String variableName, String... arguments) {
        Throwable failure = null;
        try {
            ConfigurableApplicationContext context = new SpringApplicationBuilder(SentinelApiApplication.class)
                    .web(WebApplicationType.NONE)
                    .run(arguments);
            context.close();
        } catch (Throwable throwable) {
            failure = throwable;
        }

        assertNotNull(failure, "Expected the application to refuse to start");
        String messages = allMessages(failure);
        assertTrue(messages.contains(variableName),
                "Expected the failure to name " + variableName + " but was: " + messages);
        assertFalse(messages.contains(VALID_SECRET), "The failure must not print secret values");
    }

    private String allMessages(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString().trim();
    }
}

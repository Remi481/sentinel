package com.remi481.sentinel.security;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class StartupConfigurationValidator implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final int MIN_JWT_SECRET_BYTES = 32;
    private static final int MIN_ENROLLMENT_KEY_LENGTH = 24;
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;
    private static final int MIN_DB_PASSWORD_LENGTH = 16;
    private static final String DEFAULT_DB_PASSWORD = "sentinel";

    // Must match the placeholder in deploy/.env.example; a test asserts both stay in sync.
    static final String PLACEHOLDER_VALUE = "REPLACE_WITH_GENERATED_VALUE";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<String> problems = collectProblems();
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Sentinel cannot start: " + String.join(". ", problems) + ".");
        }
    }

    List<String> collectProblems() {
        List<String> problems = new ArrayList<>();

        String jwtSecret = environment.getProperty("sentinel.security.jwt-secret");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            problems.add("SENTINEL_JWT_SECRET is missing");
        } else if (PLACEHOLDER_VALUE.equals(jwtSecret)) {
            problems.add("SENTINEL_JWT_SECRET is still the placeholder from deploy/.env.example");
        } else if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            problems.add("SENTINEL_JWT_SECRET is shorter than " + MIN_JWT_SECRET_BYTES + " bytes");
        }

        String enrollmentKey = environment.getProperty("sentinel.security.enrollment-key");
        if (enrollmentKey == null || enrollmentKey.isBlank()) {
            problems.add("SENTINEL_ENROLLMENT_KEY is missing");
        } else if (PLACEHOLDER_VALUE.equals(enrollmentKey)) {
            problems.add("SENTINEL_ENROLLMENT_KEY is still the placeholder from deploy/.env.example");
        } else if (enrollmentKey.length() < MIN_ENROLLMENT_KEY_LENGTH) {
            problems.add("SENTINEL_ENROLLMENT_KEY is shorter than " + MIN_ENROLLMENT_KEY_LENGTH + " characters");
        }

        String adminUsername = environment.getProperty("sentinel.security.admin-username");
        String adminPassword = environment.getProperty("sentinel.security.admin-password");
        boolean usernameSet = adminUsername != null && !adminUsername.isBlank();
        boolean passwordSet = adminPassword != null && !adminPassword.isBlank();
        if (usernameSet != passwordSet) {
            problems.add("SENTINEL_ADMIN_USERNAME and SENTINEL_ADMIN_PASSWORD must be set together");
        } else if (passwordSet && adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            problems.add("SENTINEL_ADMIN_PASSWORD is shorter than " + MIN_ADMIN_PASSWORD_LENGTH + " characters");
        }

        if (environment.matchesProfiles("prod")) {
            String dbPassword = environment.getProperty("SENTINEL_DB_PASSWORD");
            if (dbPassword == null || dbPassword.isBlank()) {
                problems.add("SENTINEL_DB_PASSWORD is missing");
            } else if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
                problems.add("SENTINEL_DB_PASSWORD is still the insecure default");
            } else if (PLACEHOLDER_VALUE.equals(dbPassword)) {
                problems.add("SENTINEL_DB_PASSWORD is still the placeholder from deploy/.env.example");
            } else if (dbPassword.length() < MIN_DB_PASSWORD_LENGTH) {
                problems.add("SENTINEL_DB_PASSWORD is shorter than " + MIN_DB_PASSWORD_LENGTH + " characters");
            }
        }

        return problems;
    }
}

package com.remi481.sentinel.security;

import com.remi481.sentinel.entity.AppUser;
import com.remi481.sentinel.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    public AdminBootstrapRunner(AppUserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                SecurityProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = properties.adminUsername();
        String password = properties.adminPassword();

        boolean usernameSet = username != null && !username.isBlank();
        boolean passwordSet = password != null && !password.isBlank();

        if (userRepository.count() > 0) {
            return;
        }
        if (!usernameSet && !passwordSet) {
            log.warn("No admin user exists and SENTINEL_ADMIN_USERNAME and SENTINEL_ADMIN_PASSWORD are not set. Dashboard login will not work until an admin is created.");
            return;
        }
        if (!usernameSet || !passwordSet) {
            throw new IllegalStateException("sentinel.security.admin-username and sentinel.security.admin-password (SENTINEL_ADMIN_USERNAME, SENTINEL_ADMIN_PASSWORD) must be set together");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("sentinel.security.admin-password (SENTINEL_ADMIN_PASSWORD) must be at least 12 characters");
        }

        userRepository.save(new AppUser(username, passwordEncoder.encode(password), "ADMIN", Instant.now()));
        log.info("Created admin user: " + username);
    }
}
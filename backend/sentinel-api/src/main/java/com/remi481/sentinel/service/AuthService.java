package com.remi481.sentinel.service;

import com.remi481.sentinel.dto.LoginResponse;
import com.remi481.sentinel.entity.AppUser;
import com.remi481.sentinel.exception.UnauthorizedException;
import com.remi481.sentinel.repository.AppUserRepository;
import com.remi481.sentinel.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_LOGIN_MESSAGE = "Invalid username or password";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final String dummyHash;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public LoginResponse login(String username, String password) {
        AppUser user = userRepository.findByUsername(username).orElse(null);

        boolean validCredentials;
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            validCredentials = false;
        } else {
            validCredentials = passwordEncoder.matches(password, user.getPasswordHash());
        }

        if (!validCredentials) {
            throw new UnauthorizedException(INVALID_LOGIN_MESSAGE);
        }

        String token = jwtTokenService.createToken(user);
        return new LoginResponse(token, jwtTokenService.expiresInSeconds());
    }
}
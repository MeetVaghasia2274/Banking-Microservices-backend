package com.finance.userservice.service;

import com.finance.userservice.dto.AuthResponse;
import com.finance.userservice.dto.LoginRequest;
import com.finance.userservice.dto.RegisterRequest;
import com.finance.userservice.dto.UserProfileResponse;
import com.finance.userservice.model.User;
import com.finance.userservice.repository.UserRepository;
import com.finance.userservice.security.JwtUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered!");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtils.generateJwtToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            
            if (jwtUtils.validateJwtToken(token)) {
                Date expiration = jwtUtils.getExpirationDateFromJwtToken(token);
                long remainingMillis = expiration.getTime() - System.currentTimeMillis();

                if (remainingMillis > 0) {
                    String redisKey = "blacklist:" + token;
                    try {
                        redisTemplate.opsForValue().set(redisKey, "true", remainingMillis, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        System.err.println("[WARN] Redis is unavailable to blacklist token: " + e.getMessage());
                    }
                    System.out.println("Blacklisted token: " + token + " for " + remainingMillis + " ms");
                }

            }
        }
    }

    public UserProfileResponse getUserProfile(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = bearerToken.substring(7);
        if (!jwtUtils.validateJwtToken(token)) {
            throw new RuntimeException("Invalid or expired JWT token");
        }

        String email = jwtUtils.getEmailFromJwtToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

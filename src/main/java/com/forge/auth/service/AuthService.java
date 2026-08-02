package com.forge.auth.service;

import com.forge.auth.dto.LoginRequest;
import com.forge.auth.dto.LoginResponse;
import com.forge.auth.dto.ProfileRequest;
import com.forge.auth.dto.RegisterRequest;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.security.JwtTokenProvider;
import com.forge.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        return new LoginResponse(token, refreshToken, "Bearer", toUserInfo(user));
    }

    public LoginResponse refresh(String refreshTokenValue) {
        if (!jwtTokenProvider.validateRefreshToken(refreshTokenValue)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshTokenValue);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getPassword());
        String token = jwtTokenProvider.generateToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return new LoginResponse(token, refreshToken, "Bearer", toUserInfo(user));
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        String username = request.getEmail().substring(0, request.getEmail().indexOf('@'));
        if (userRepository.existsByUsername(username)) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 6);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setDisplayName(username);

        userRepository.save(user);
        log.info("User registered: {}", user.getUsername());
    }

    public LoginResponse.UserInfo getProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toUserInfo(user);
    }

    public LoginResponse.UserInfo updateProfile(ProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getLeetcodeUsername() != null) user.setLeetcodeUsername(request.getLeetcodeUsername());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getTargetLevel() != null) user.setTargetLevel(request.getTargetLevel());
        if (request.getPreferredAnalysisTime() != null)
            user.setPreferredAnalysisTime(java.time.LocalTime.parse(request.getPreferredAnalysisTime()));

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getUsername());
        return toUserInfo(user);
    }

    private LoginResponse.UserInfo toUserInfo(User user) {
        return new LoginResponse.UserInfo(
                user.getId().toString(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getLeetcodeUsername(),
                user.getTargetLevel(),
                user.getPreferredAnalysisTime() != null ? user.getPreferredAnalysisTime().toString() : null,
                user.getDailyGenerationsUsed() != null ? user.getDailyGenerationsUsed() : 0,
                user.getLastGenerationDate() != null ? user.getLastGenerationDate().toString() : null
        );
    }
}

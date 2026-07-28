package com.forge.auth.controller;

import com.forge.auth.dto.LoginRequest;
import com.forge.auth.dto.LoginResponse;
import com.forge.auth.dto.ProfileRequest;
import com.forge.auth.dto.RegisterRequest;
import com.forge.auth.service.AuthService;
import com.forge.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please sign in."));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getProfile() {
        LoginResponse.UserInfo profile = authService.getProfile();
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> updateProfile(@RequestBody ProfileRequest request) {
        LoginResponse.UserInfo profile = authService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }
}

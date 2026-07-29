package com.forge.auth.controller;

import com.forge.auth.dto.LoginRequest;
import com.forge.auth.dto.LoginResponse;
import com.forge.auth.dto.ProfileRequest;
import com.forge.auth.dto.RegisterRequest;
import com.forge.auth.service.AuthService;
import com.forge.common.dto.ApiResponse;
import com.forge.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request);
        setAuthCookies(response, loginResponse);
        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please sign in."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@CookieValue(value = "forge_refresh", required = false) String refreshToken,
                                                              HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Refresh token not found"));
        }
        LoginResponse loginResponse = authService.refresh(refreshToken);
        setAuthCookies(response, loginResponse);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.<Void>success("Logged out", null));
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

    private void setAuthCookies(HttpServletResponse response, LoginResponse loginResponse) {
        Cookie accessCookie = new Cookie("forge_token", loginResponse.getToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setAttribute("SameSite", "None");
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtTokenProvider.getExpirationMs() / 1000));
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("forge_refresh", loginResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setAttribute("SameSite", "None");
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge((int) (jwtTokenProvider.getRefreshExpirationMs() / 1000));
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("forge_token", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setAttribute("SameSite", "None");
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("forge_refresh", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setAttribute("SameSite", "None");
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}

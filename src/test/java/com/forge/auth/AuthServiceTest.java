package com.forge.auth;

import com.forge.auth.dto.ProfileRequest;
import com.forge.auth.dto.RegisterRequest;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.auth.service.AuthService;
import com.forge.common.exception.BadRequestException;
import com.forge.security.JwtTokenProvider;
import com.forge.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new AuthService(authenticationManager, userRepository, passwordEncoder, jwtTokenProvider);
        userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateProfileRejectsEmailAlreadyUsedByAnotherUser() {
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("old@x.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("other@x.com")).thenReturn(true);

        ProfileRequest request = new ProfileRequest();
        request.setEmail("other@x.com");

        assertThrows(BadRequestException.class, () -> service.updateProfile(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfileAllowsKeepingOwnEmailWithDifferentCasing() {
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("me@x.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProfileRequest request = new ProfileRequest();
        request.setEmail("ME@x.com");

        assertDoesNotThrow(() -> service.updateProfile(request));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("me@x.com", captor.getValue().getEmail());
    }

    @Test
    void registerNormalizesEmailToLowercaseBeforeSaving() {
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.existsByEmail("New.User@X.com".toLowerCase())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest request = new RegisterRequest();
        request.setEmail("New.User@X.com");
        request.setPassword("secret");

        service.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("new.user@x.com", captor.getValue().getEmail());
    }
}

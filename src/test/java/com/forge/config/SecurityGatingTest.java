package com.forge.config;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.security.JwtTokenProvider;
import com.forge.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityGatingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;

    private String tokenFor(UUID userId, String username, String role) {
        return jwtTokenProvider.generateToken(new UserPrincipal(userId, username, "irrelevant", role));
    }

    @Test
    void internalEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/internal/engine-report"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalEndpointsRejectNonAdminUsers() throws Exception {
        User user = new User();
        user.setUsername("plain-user-" + UUID.randomUUID().toString().substring(0, 8));
        user.setDisplayName("plain");
        user.setRole("USER");
        user = userRepository.save(user);

        mockMvc.perform(get("/api/internal/engine-report")
                        .header("Authorization", "Bearer " + tokenFor(user.getId(), user.getUsername(), "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalEndpointsAllowAdminUsers() throws Exception {
        User admin = userRepository.findByUsername("forge")
                .orElseThrow(() -> new IllegalStateException("dev seed user missing"));

        mockMvc.perform(get("/api/internal/engine-report")
                        .header("Authorization", "Bearer " + tokenFor(admin.getId(), admin.getUsername(), "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void registerEndpointIsWiredInDevProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

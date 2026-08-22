package com.forge.config;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("prod")
public class ProdAdminSeedInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;

    public ProdAdminSeedInitializer(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    @Value("${ADMIN_USERNAME:}") String adminUsername,
                                    @Value("${ADMIN_PASSWORD:}") String adminPassword,
                                    @Value("${ADMIN_EMAIL:}") String adminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Fresh prod database has no users but ADMIN_USERNAME/ADMIN_PASSWORD are not set. "
                            + "Refusing to boot without an admin account — set ADMIN_PASSWORD in the "
                            + "Render dashboard and redeploy.");
        }
        seedAdminUser();
    }

    @Transactional
    public void seedAdminUser() {
        if (userRepository.count() > 0) {
            return;
        }

        User user = new User();
        user.setUsername(adminUsername);
        user.setEmail(adminEmail.isBlank() ? adminUsername + "@example.com" : adminEmail);
        user.setDisplayName(adminUsername);
        user.setRole("ADMIN");
        user.setPassword(passwordEncoder.encode(adminPassword));
        userRepository.save(user);
        log.info("Prod admin user created: {}", user.getUsername());
    }
}

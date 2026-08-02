package com.forge.config;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevSeedInitializer implements ApplicationRunner {

    static final String SEED_USERNAME = "forge";
    static final String SEED_PASSWORD = "forge123";
    static final String SEED_EMAIL = "forge@example.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedDevUser();
    }

    @Transactional
    public void seedDevUser() {
        User user = userRepository.findByUsername(SEED_USERNAME).orElse(null);

        if (user == null) {
            user = new User();
            user.setUsername(SEED_USERNAME);
            user.setEmail(SEED_EMAIL);
            user.setDisplayName(SEED_USERNAME);
            user.setPassword(passwordEncoder.encode(SEED_PASSWORD));
            userRepository.save(user);
            log.info("Dev seed user created: {} / {}", SEED_USERNAME, SEED_PASSWORD);
            return;
        }

        if (passwordEncoder.matches(SEED_PASSWORD, user.getPassword())) {
            return;
        }

        user.setPassword(passwordEncoder.encode(SEED_PASSWORD));
        userRepository.save(user);
        log.info("Dev seed user password reset for: {}", SEED_USERNAME);
    }
}

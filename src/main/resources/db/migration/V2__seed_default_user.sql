-- Seed default user for Forge
-- Password: forge123 (BCrypt encoded)
-- Generated with: BCryptPasswordEncoder().encode("forge123")

INSERT INTO users (id, username, password, email, display_name, created_at, updated_at)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'forge',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'forge@example.com',
    'Forge User',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

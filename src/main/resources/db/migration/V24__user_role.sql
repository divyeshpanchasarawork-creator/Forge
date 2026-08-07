-- Admin role for the internal engine endpoints. Single-user app: the sole user is the owner.
ALTER TABLE users ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'USER';
UPDATE users SET role = 'ADMIN';

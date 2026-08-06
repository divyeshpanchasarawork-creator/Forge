-- Edge-case hardening: enforce unique emails. NULL emails stay exempt on both Postgres and H2,
-- so a plain unique index is used instead of a partial one.
CREATE UNIQUE INDEX uk_users_email ON users(email);

ALTER TABLE scorer_weights ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;

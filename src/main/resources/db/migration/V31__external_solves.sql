-- External solves detected via LeetCode's recentAcSubmissionList during profile sync.
-- One row per (user, problem): sync upserts unlogged rows, logging an attempt in Forge
-- flips logged so the solve is never surfaced twice.
CREATE TABLE IF NOT EXISTS external_solves (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255),
    title_slug VARCHAR(255) NOT NULL,
    solved_at TIMESTAMP,
    logged BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_external_solves_user_slug UNIQUE (user_id, title_slug)
);

CREATE INDEX IF NOT EXISTS idx_external_solves_pending ON external_solves(user_id, logged);

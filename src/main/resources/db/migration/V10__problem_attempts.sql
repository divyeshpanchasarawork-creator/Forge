CREATE TABLE problem_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    problem_title VARCHAR(255) NOT NULL,
    problem_slug VARCHAR(255) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    topic_tag_slug VARCHAR(100),
    topic_tag_name VARCHAR(100),
    outcome VARCHAR(20) NOT NULL DEFAULT 'SOLVED',
    hints_used INTEGER DEFAULT 0,
    time_taken_seconds INTEGER,
    quality INTEGER DEFAULT 0,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attempts_outcome CHECK (outcome IN ('SOLVED', 'FAILED', 'PARTIAL', 'SKIPPED')),
    CONSTRAINT chk_attempts_quality CHECK (quality BETWEEN 0 AND 5)
);

CREATE INDEX idx_attempts_user ON problem_attempts(user_id);
CREATE INDEX idx_attempts_user_slug ON problem_attempts(user_id, problem_slug);
CREATE INDEX idx_attempts_attempted_at ON problem_attempts(attempted_at);

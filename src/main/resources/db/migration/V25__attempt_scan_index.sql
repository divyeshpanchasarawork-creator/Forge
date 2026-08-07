-- Range-scan index for the bounded recent-attempts query backing the scoring
-- context (findByUserIdOrderByAttemptedAtDesc over the most recent 500 rows).

CREATE INDEX IF NOT EXISTS idx_attempts_user_at
    ON problem_attempts(user_id, attempted_at);

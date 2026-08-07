-- Composite indexes for the hottest read paths (recommendation lists, revision queries,
-- attempt time-series, weak/strong topic lookups). Single-column indexes already exist;
-- these cover the multi-column WHERE / ORDER BY shapes used in production.

CREATE INDEX IF NOT EXISTS idx_attempts_user_outcome_at
    ON problem_attempts(user_id, outcome, attempted_at);

CREATE INDEX IF NOT EXISTS idx_revisions_user_date_completed
    ON revisions(user_id, scheduled_date, completed);

CREATE INDEX IF NOT EXISTS idx_recommendations_user_status
    ON recommendations(user_id, status);

CREATE INDEX IF NOT EXISTS idx_topics_user_confidence
    ON topics(user_id, confidence);

CREATE TABLE daily_metrics (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    metric_date DATE NOT NULL,
    mastery DOUBLE PRECISION DEFAULT 0,
    confidence DOUBLE PRECISION DEFAULT 0,
    retention DOUBLE PRECISION DEFAULT 100,
    skill_rating DOUBLE PRECISION DEFAULT 1000,
    consistency DOUBLE PRECISION DEFAULT 0,
    solved_delta INTEGER DEFAULT 0,
    revisions_done INTEGER DEFAULT 0,
    journal_hours DOUBLE PRECISION DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_metrics_user_date UNIQUE (user_id, metric_date)
);

CREATE INDEX idx_daily_metrics_user_date ON daily_metrics(user_id, metric_date);

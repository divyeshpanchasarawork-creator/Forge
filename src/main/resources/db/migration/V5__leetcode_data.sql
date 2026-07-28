-- LeetCode Integration V5
-- Store synced LeetCode data per user

-- ============================================
-- 1. LeetCode snapshot (one per user)
-- ============================================
CREATE TABLE leetcode_snapshots (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    total_solved INT DEFAULT 0,
    easy_solved INT DEFAULT 0,
    medium_solved INT DEFAULT 0,
    hard_solved INT DEFAULT 0,
    easy_beats_pct DOUBLE,
    medium_beats_pct DOUBLE,
    hard_beats_pct DOUBLE,
    ranking INT,
    contest_rating DOUBLE,
    contest_ranking INT,
    contest_attended_count INT DEFAULT 0,
    streak INT DEFAULT 0,
    total_active_days INT DEFAULT 0,
    submission_calendar TEXT,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lc_snapshot_user UNIQUE (user_id)
);

-- ============================================
-- 2. LeetCode tag stats (per user per tag)
-- ============================================
CREATE TABLE leetcode_tag_stats (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    tag_name VARCHAR(100) NOT NULL,
    tag_slug VARCHAR(100) NOT NULL,
    problems_solved INT DEFAULT 0,
    skill_level VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_lc_tags_user ON leetcode_tag_stats(user_id);

-- ============================================
-- 3. Add source column to topics
-- ============================================
ALTER TABLE topics ADD COLUMN source VARCHAR(20) DEFAULT 'MANUAL';

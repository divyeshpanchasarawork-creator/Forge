-- Forge Multi-User Support V4
-- Add user_id to all data tables, extend users with profile fields

-- ============================================
-- 1. Extend users table with profile fields
-- ============================================
ALTER TABLE users ADD COLUMN leetcode_username VARCHAR(50);
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);
ALTER TABLE users ALTER COLUMN password SET NULL;

-- ============================================
-- 2. Add user_id to topics
-- ============================================
ALTER TABLE topics ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE topics SET user_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE topics ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_topics_user_id ON topics(user_id);

-- ============================================
-- 3. Add user_id to problems
-- ============================================
ALTER TABLE problems ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE problems SET user_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE problems ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_problems_user_id ON problems(user_id);

-- ============================================
-- 4. Add user_id to revisions
-- ============================================
ALTER TABLE revisions ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE revisions SET user_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE revisions ALTER COLUMN user_id SET NOT NULL;

-- ============================================
-- 5. Add user_id to recommendations
-- ============================================
ALTER TABLE recommendations ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE recommendations SET user_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE recommendations ALTER COLUMN user_id SET NOT NULL;

-- ============================================
-- 6. Rebuild journals table with user_id + composite unique
-- ============================================
-- H2 does not support DROP UNIQUE on inline constraints, so we rebuild.
CREATE TABLE journals_new (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    entry_date DATE NOT NULL,
    morning_goal TEXT,
    evening_reflection TEXT,
    energy INT,
    mood INT,
    hours_studied DOUBLE DEFAULT 0,
    achievements TEXT,
    challenges TEXT,
    lessons TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_journals_new_energy CHECK (energy BETWEEN 1 AND 5),
    CONSTRAINT chk_journals_new_mood CHECK (mood BETWEEN 1 AND 5),
    CONSTRAINT uk_journals_user_date UNIQUE (user_id, entry_date)
);

INSERT INTO journals_new (id, user_id, entry_date, morning_goal, evening_reflection, energy, mood, hours_studied, achievements, challenges, lessons, created_at, updated_at)
SELECT id, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', entry_date, morning_goal, evening_reflection, energy, mood, hours_studied, achievements, challenges, lessons, created_at, updated_at
FROM journals;

DROP TABLE journals;
ALTER TABLE journals_new RENAME TO journals;
CREATE INDEX idx_journals_entry_date ON journals(entry_date);
CREATE INDEX idx_journals_user_id ON journals(user_id);

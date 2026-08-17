-- Dedupe any pre-existing duplicate pending revisions (keep the earliest).
DELETE FROM revisions
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY user_id, topic_id ORDER BY created_at) AS rn
        FROM revisions
        WHERE completed = FALSE
    ) ranked
    WHERE ranked.rn > 1
);

-- Enforce at-most-one pending revision per (user, topic) at the database level so a
-- concurrent scheduler instance can never materialize a duplicate. The partial-index
-- equivalent is not portable (H2 2.x dropped index filter conditions), so the guard rides
-- on a nullable application-maintained key: pending revisions carry the topic id here and
-- NULL it on completion, and NULLs never collide in a unique index on either database.
ALTER TABLE revisions ADD COLUMN pending_topic UUID;

UPDATE revisions SET pending_topic = topic_id WHERE completed = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_revisions_pending ON revisions(user_id, pending_topic);

-- Drop the dead problems / problem_topics tables. No entity or query reads them;
-- problem tracking moved to problem_attempts.
DROP TABLE IF EXISTS problem_topics;
DROP TABLE IF EXISTS problems;

-- leetcode_snapshots.submission_calendar is fetched and immediately discarded.
ALTER TABLE leetcode_snapshots DROP COLUMN IF EXISTS submission_calendar;

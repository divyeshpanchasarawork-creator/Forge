-- Remove the dormant dev seed user (its stored bcrypt hash does not match the
-- documented "forge123" password, so the account is unusable in every environment).
-- Prod no longer carries a known-credential account; dev recreates it on startup
-- via DevSeedInitializer (dev profile only).
-- Child tables are deleted explicitly (no ON DELETE CASCADE on user_id).
DELETE FROM problem_attempts WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM problem_suggestions WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM daily_metrics WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM leetcode_tag_stats WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM leetcode_snapshots WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM journals WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM recommendations WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM revisions WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM topics WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM problems WHERE user_id IN (SELECT id FROM users WHERE username = 'forge');
DELETE FROM users WHERE username = 'forge';

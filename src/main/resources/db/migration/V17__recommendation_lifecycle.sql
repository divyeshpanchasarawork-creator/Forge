ALTER TABLE recommendations ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE recommendations ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;
ALTER TABLE recommendations ADD COLUMN IF NOT EXISTS outcome VARCHAR(20);

UPDATE recommendations SET status = 'DISMISSED' WHERE dismissed = TRUE;

CREATE INDEX IF NOT EXISTS idx_recommendations_user_status ON recommendations (user_id, status);

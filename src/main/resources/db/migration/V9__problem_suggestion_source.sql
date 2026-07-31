ALTER TABLE problem_suggestions ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'WEAK_TAG';
CREATE INDEX idx_problem_suggestions_user_source ON problem_suggestions (user_id, source);

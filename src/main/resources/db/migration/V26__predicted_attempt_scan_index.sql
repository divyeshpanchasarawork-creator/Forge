-- Index for the calibration/engine-report snapshot scan
-- (findWithPredictedScores: WHERE predicted_score IS NOT NULL AND signals_json IS NOT NULL
--  ORDER BY attempted_at DESC). A Postgres partial index would be ideal here, but H2 (dev)
-- does not support partial/INCLUDE indexes, so we use a plain composite index: b-tree
-- indexes skip NULLs, so rows where either column is NULL are excluded from the index,
-- making this behave like the partial index on both databases.

CREATE INDEX IF NOT EXISTS idx_attempts_predicted_scan
    ON problem_attempts(predicted_score, signals_json, attempted_at DESC);

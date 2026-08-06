-- Rec-engine calibration: single global scorer weights row + per-attempt signal snapshots.
-- The recommendation scorer keeps a single calibrated weight vector for this single-user app.
CREATE TABLE scorer_weights (
    id UUID NOT NULL,
    weights_json TEXT NOT NULL,
    sample_count INTEGER NOT NULL DEFAULT 0,
    metric_before DOUBLE PRECISION,
    metric_after DOUBLE PRECISION,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Snapshot of the signal values + predicted total at attempt time, so the calibration
-- job can re-score stored samples under any candidate weight vector.
ALTER TABLE problem_attempts ADD COLUMN signals_json TEXT;
ALTER TABLE problem_attempts ADD COLUMN predicted_score INTEGER;

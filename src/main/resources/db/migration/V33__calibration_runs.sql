-- Append-only ledger of nightly calibration outcomes (applied / kept / skipped).
-- The scorer_weights row is overwritten in place, so without this history the engine
-- health card could show only the latest state and never a trend.
CREATE TABLE IF NOT EXISTS calibration_runs (
    id UUID NOT NULL PRIMARY KEY,
    ran_at TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL,
    sample_count INTEGER NOT NULL DEFAULT 0,
    min_samples INTEGER NOT NULL DEFAULT 0,
    metric_before DOUBLE PRECISION,
    metric_after DOUBLE PRECISION,
    swapped BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_calibration_runs_ran_at ON calibration_runs(ran_at DESC);

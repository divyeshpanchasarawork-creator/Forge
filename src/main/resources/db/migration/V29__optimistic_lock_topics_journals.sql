-- Optimistic locking on the two hot-write entities. Both Topic and Journal are updated by
-- concurrent request paths (practice submits, revision completes, scheduler retention refresh,
-- daily journal upsert), so stale-overwrite guardrails mirror the scorer_weights pattern (V21).
ALTER TABLE topics ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE journals ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;

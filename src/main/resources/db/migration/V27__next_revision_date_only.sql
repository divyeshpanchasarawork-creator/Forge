-- SM-2 nextRevision is day-granularity (interval in days); store it as DATE so
-- "due" comparisons are timezone-independent instead of depending on the server clock.
ALTER TABLE topics ALTER COLUMN next_revision TYPE DATE;

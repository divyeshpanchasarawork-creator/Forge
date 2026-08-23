-- Remove write-only Topic fields: masteryProbability was maintained by MasteryService but
-- never read, and lastQuality was written on attempts/revisions with no consumer.
ALTER TABLE topics DROP COLUMN IF EXISTS mastery_probability;
ALTER TABLE topics DROP COLUMN IF EXISTS last_quality;

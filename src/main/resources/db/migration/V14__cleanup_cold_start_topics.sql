-- Mark existing cold-start starter topics (created before the source field was tagged)
UPDATE topics
SET source = 'COLD_START'
WHERE category = 'Fundamentals'
  AND confidence = 2
  AND mastery = 5
  AND status = 'NOT_STARTED'
  AND source = 'MANUAL';

-- Remove un-engaged cold-start topics that duplicate a LeetCode-synced topic
DELETE FROM topics t
WHERE t.source = 'COLD_START'
  AND (t.attempts_total IS NULL OR t.attempts_total = 0)
  AND EXISTS (
      SELECT 1 FROM topics lc
      WHERE lc.source = 'LEETCODE'
        AND LOWER(lc.title) = LOWER(t.title)
  );

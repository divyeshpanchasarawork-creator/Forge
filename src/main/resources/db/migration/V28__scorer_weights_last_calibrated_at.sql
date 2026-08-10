-- Tracks when scorer weights were LAST APPLIED (swapped in by calibration). This stays NULL
-- when calibration only recorded metrics without swapping, so the UI never claims a
-- calibration happened when it didn't.
ALTER TABLE scorer_weights ADD COLUMN last_calibrated_at TIMESTAMP NULL;

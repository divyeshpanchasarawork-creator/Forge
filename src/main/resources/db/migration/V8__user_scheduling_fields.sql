ALTER TABLE users ADD COLUMN preferred_analysis_time TIME;
ALTER TABLE users ADD COLUMN daily_generations_used INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN last_generation_date DATE;

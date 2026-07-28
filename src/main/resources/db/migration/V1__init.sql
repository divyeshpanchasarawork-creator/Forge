-- Forge Database Schema V1
-- All tables for the personal engineering companion

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    display_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Topics
CREATE TABLE topics (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    confidence INT DEFAULT 0,
    mastery INT DEFAULT 0,
    notes TEXT,
    last_revision TIMESTAMP,
    next_revision TIMESTAMP,
    status VARCHAR(20) DEFAULT 'NOT_STARTED',
    revision_count INT DEFAULT 0,
    estimated_retention DOUBLE DEFAULT 100.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_topics_confidence CHECK (confidence BETWEEN 0 AND 10),
    CONSTRAINT chk_topics_mastery CHECK (mastery BETWEEN 0 AND 100),
    CONSTRAINT chk_topics_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'MASTERED'))
);

-- Problems
CREATE TABLE problems (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    leetcode_id VARCHAR(20),
    difficulty VARCHAR(10) NOT NULL,
    time_taken INT,
    attempts INT DEFAULT 1,
    confidence INT DEFAULT 0,
    mistakes TEXT,
    summary TEXT,
    notes TEXT,
    solution_url VARCHAR(500),
    solved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_problems_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_problems_confidence CHECK (confidence BETWEEN 0 AND 10)
);

-- Problem-Topic join table
CREATE TABLE problem_topics (
    problem_id UUID NOT NULL,
    topic_id UUID NOT NULL,
    PRIMARY KEY (problem_id, topic_id),
    CONSTRAINT fk_problem_topics_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    CONSTRAINT fk_problem_topics_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

-- Revisions
CREATE TABLE revisions (
    id UUID PRIMARY KEY,
    topic_id UUID NOT NULL,
    scheduled_date DATE NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    priority INT DEFAULT 1,
    reason TEXT,
    completion_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_revisions_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

-- Recommendations
CREATE TABLE recommendations (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    reason TEXT NOT NULL,
    priority INT DEFAULT 1,
    action VARCHAR(50) NOT NULL,
    dismissed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Journal
CREATE TABLE journals (
    id UUID PRIMARY KEY,
    entry_date DATE NOT NULL UNIQUE,
    morning_goal TEXT,
    evening_reflection TEXT,
    energy INT,
    mood INT,
    hours_studied DOUBLE DEFAULT 0,
    achievements TEXT,
    challenges TEXT,
    lessons TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_journals_energy CHECK (energy BETWEEN 1 AND 5),
    CONSTRAINT chk_journals_mood CHECK (mood BETWEEN 1 AND 5)
);

-- Indexes for performance
CREATE INDEX idx_topics_status ON topics(status);
CREATE INDEX idx_topics_next_revision ON topics(next_revision);
CREATE INDEX idx_topics_category ON topics(category);
CREATE INDEX idx_topics_confidence ON topics(confidence);
CREATE INDEX idx_revisions_scheduled_date ON revisions(scheduled_date);
CREATE INDEX idx_revisions_topic_id ON revisions(topic_id);
CREATE INDEX idx_revisions_completed ON revisions(completed);
CREATE INDEX idx_problems_solved_at ON problems(solved_at);
CREATE INDEX idx_problems_difficulty ON problems(difficulty);
CREATE INDEX idx_problem_topics_topic ON problem_topics(topic_id);
CREATE INDEX idx_journals_entry_date ON journals(entry_date);
CREATE INDEX idx_recommendations_dismissed ON recommendations(dismissed);
CREATE INDEX idx_recommendations_priority ON recommendations(priority);

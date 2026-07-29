CREATE TABLE problem_suggestions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    title_slug VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    topic_tag_slug VARCHAR(100),
    topic_tag_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prob_suggestions_user ON problem_suggestions(user_id);
CREATE INDEX idx_prob_suggestions_tag ON problem_suggestions(topic_tag_slug);

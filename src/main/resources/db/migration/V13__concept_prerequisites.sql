CREATE TABLE concept_prerequisites (
    id UUID PRIMARY KEY,
    concept_slug VARCHAR(100) NOT NULL,
    prerequisite_slug VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prereq_pair UNIQUE (concept_slug, prerequisite_slug)
);

CREATE INDEX idx_prereq_concept ON concept_prerequisites(concept_slug);
CREATE INDEX idx_prereq_prerequisite ON concept_prerequisites(prerequisite_slug);

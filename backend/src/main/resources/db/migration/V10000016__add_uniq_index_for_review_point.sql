ALTER TABLE review_point
    ADD COLUMN initial_reviewed_at TIMESTAMP NOT NULL;

UPDATE review_point SET initial_reviewed_at = last_reviewed_at;

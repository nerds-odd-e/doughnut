ALTER TABLE review_point
    ADD COLUMN next_review_at TIMESTAMP;

UPDATE review_point SET next_review_at = last_reviewed_at;

ALTER TABLE review_point
    MODIFY next_review_at TIMESTAMP NOT NULL;

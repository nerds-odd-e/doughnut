ALTER TABLE review_point
    ADD COLUMN next_review_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

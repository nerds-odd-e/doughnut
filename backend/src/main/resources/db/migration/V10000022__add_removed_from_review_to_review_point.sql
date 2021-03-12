ALTER TABLE review_point
    ADD COLUMN removed_from_review BOOLEAN;

UPDATE review_point SET removed_from_review = 0;

ALTER TABLE review_point
    MODIFY removed_from_review BOOLEAN NOT NULL DEFAULT 0;
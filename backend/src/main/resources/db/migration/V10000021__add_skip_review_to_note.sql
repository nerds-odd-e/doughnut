ALTER TABLE note
    ADD COLUMN skip_review BOOLEAN;

UPDATE note SET skip_review = 0;

ALTER TABLE note
    MODIFY skip_review BOOLEAN NOT NULL DEFAULT 0;
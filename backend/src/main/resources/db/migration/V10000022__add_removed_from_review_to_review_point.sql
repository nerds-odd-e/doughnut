ALTER TABLE review_point
    ADD COLUMN removed_from_review BOOLEAN;

UPDATE review_point SET removed_from_review = 0;

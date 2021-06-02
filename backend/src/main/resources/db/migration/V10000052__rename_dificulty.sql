ALTER TABLE review_point
RENAME COLUMN difficulty to repetition_count;

UPDATE review_point SET repetition_count = (forgetting_curve_index - 100) / 8;

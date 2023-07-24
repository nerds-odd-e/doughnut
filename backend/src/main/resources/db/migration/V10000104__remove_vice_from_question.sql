ALTER TABLE quiz_question DROP COLUMN vice_review_point_ids;
ALTER TABLE quiz_question ADD `thing_id` int unsigned DEFAULT NULL;
ALTER TABLE quiz_question ADD FOREIGN KEY (thing_id) REFERENCES thing(id) ON DELETE CASCADE;

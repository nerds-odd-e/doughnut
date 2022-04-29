ALTER TABLE review_point ADD `thing_id` int unsigned DEFAULT NULL;

UPDATE review_point
INNER JOIN thing ON thing.note_id = review_point.note_id AND thing.link_id = review_point.link_id
SET thing_id = thing.id;

ALTER TABLE `review_point` MODIFY `thing_id` int unsigned NOT NULL;
ALTER TABLE review_point ADD FOREIGN KEY (thing_id) REFERENCES thing(id) ON DELETE CASCADE;

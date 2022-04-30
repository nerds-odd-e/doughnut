ALTER TABLE `review_point` MODIFY `thing_id` int unsigned NOT NULL;
ALTER TABLE review_point ADD FOREIGN KEY (thing_id) REFERENCES thing(id) ON DELETE CASCADE;

ALTER TABLE review_point DROP FOREIGN KEY FK_review_point_user_id;
ALTER TABLE review_point DROP INDEX user_id;
ALTER TABLE review_point ADD UNIQUE INDEX user_thing (user_id, thing_id);
ALTER TABLE thing ADD UNIQUE INDEX link_note (link_id, note_id);
ALTER TABLE review_point ADD FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

ALTER TABLE review_point DROP FOREIGN KEY fk_review_point_note_id;
ALTER TABLE review_point DROP FOREIGN KEY FK_review_point_link_id;
ALTER TABLE review_point DROP COLUMN note_id;
ALTER TABLE review_point DROP COLUMN link_id;


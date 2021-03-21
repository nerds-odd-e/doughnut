ALTER TABLE review_point
 DROP FOREIGN KEY fk_review_point_user_id;
ALTER TABLE review_point
 DROP INDEX user_id;

ALTER TABLE review_point
 ADD COLUMN link_id INT UNSIGNED;

ALTER TABLE review_point
ADD CONSTRAINT FK_review_point_link_id
FOREIGN KEY (link_id) REFERENCES link(id);

ALTER TABLE review_point
ADD CONSTRAINT FK_review_point_user_id
FOREIGN KEY (user_id) REFERENCES user(id);


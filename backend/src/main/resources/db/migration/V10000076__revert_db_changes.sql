ALTER TABLE text_content DROP COLUMN `language`;
ALTER TABLE comment_read_status DROP FOREIGN KEY fk_comment_read_status_comment_id;
ALTER TABLE comment_read_status DROP FOREIGN KEY fk_comment_read_status_user_id;
ALTER TABLE comment DROP FOREIGN KEY fk_comment_id;
ALTER TABLE comment DROP FOREIGN KEY fk_comment_user_id;
DROP TABLE comment_read_status;
DROP TABLE comment;
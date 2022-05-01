ALTER TABLE link DROP FOREIGN KEY FK_link_user_id;
ALTER TABLE note DROP FOREIGN KEY fk_user_id;
ALTER TABLE link DROP COLUMN user_id;
ALTER TABLE note DROP COLUMN user_id;
ALTER TABLE link DROP COLUMN created_at;
ALTER TABLE note DROP COLUMN created_at;
ALTER TABLE thing RENAME COLUMN user_id TO creator_id;


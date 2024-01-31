ALTER TABLE note DROP FOREIGN KEY fk_note_master_review_setting_id;
ALTER TABLE note DROP COLUMN master_review_setting_id;
DROP TABLE review_setting;


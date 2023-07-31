ALTER TABLE note MODIFY title varchar(150) NULL;
ALTER TABLE note ADD target_note_id int unsigned DEFAULT NULL;
ALTER TABLE note ADD FOREIGN KEY (target_note_id) REFERENCES note(id) ON DELETE CASCADE;
ALTER TABLE note ADD construct_type_id int;

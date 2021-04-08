ALTER TABLE note
DROP FOREIGN KEY fk_note_ownership_id;
ALTER TABLE note
DROP COLUMN ownership_id;


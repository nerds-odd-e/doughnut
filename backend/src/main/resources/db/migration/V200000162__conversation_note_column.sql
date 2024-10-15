ALTER TABLE conversation
ADD COLUMN note_id int unsigned DEFAULT NULL,
ADD FOREIGN KEY
(note_id) REFERENCES note
(id);

ALTER TABLE quiz_question ADD COLUMN note_id INT UNSIGNED;

UPDATE quiz_question rp
INNER JOIN thing t ON rp.thing_id = t.id
SET rp.note_id = t.note_id;

ALTER TABLE quiz_question DROP FOREIGN KEY quiz_question_ibfk_1;

ALTER TABLE quiz_question DROP COLUMN thing_id;

ALTER TABLE quiz_question ADD CONSTRAINT quiz_question_fk_note_id FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE;

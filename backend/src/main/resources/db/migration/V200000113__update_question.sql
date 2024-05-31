ALTER TABLE quiz_question
DROP FOREIGN KEY quiz_question_fk_linking_note_id;

ALTER TABLE quiz_question
DROP COLUMN question_type,
DROP COLUMN category_link_id,
DROP COLUMN option_thing_ids;
ALTER TABLE quiz_question

ADD COLUMN check_spell BOOLEAN DEFAULT FALSE,
ADD COLUMN has_image BOOLEAN DEFAULT FALSE;

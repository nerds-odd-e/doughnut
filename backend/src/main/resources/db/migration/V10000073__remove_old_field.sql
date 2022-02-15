ALTER TABLE note DROP COLUMN title_idn;
ALTER TABLE note DROP COLUMN description_idn;

ALTER TABLE note DROP FOREIGN KEY note_ibfk_2;
ALTER TABLE note DROP COLUMN translation_text_content_id;
ALTER TABLE note DROP COLUMN is_outdated_translation_idn;

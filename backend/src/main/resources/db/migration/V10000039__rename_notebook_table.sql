RENAME TABLE note_book TO notebook;
ALTER TABLE notebook
DROP FOREIGN KEY fk_notes_book_top_note_id;
ALTER TABLE notebook
DROP COLUMN top_note_id;


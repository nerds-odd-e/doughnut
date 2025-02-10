ALTER TABLE memory_tracker DROP INDEX user_note;
ALTER TABLE memory_tracker ADD UNIQUE KEY user_note_spelling (user_id, note_id, spelling);

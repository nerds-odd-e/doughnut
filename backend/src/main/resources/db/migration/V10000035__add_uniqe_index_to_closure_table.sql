ALTER TABLE notes_closure
    ADD UNIQUE INDEX (note_id, depth);


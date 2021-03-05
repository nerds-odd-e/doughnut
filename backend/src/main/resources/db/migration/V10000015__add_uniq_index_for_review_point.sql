ALTER TABLE review_point
    ADD UNIQUE INDEX (user_id, note_id);

ALTER TABLE subscription
    ADD UNIQUE INDEX (user_id, note_id);

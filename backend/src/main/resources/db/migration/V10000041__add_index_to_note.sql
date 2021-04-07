ALTER TABLE note
    ADD CONSTRAINT `fk_note_notebook_id`
        FOREIGN KEY (notebook_id) REFERENCES notebook (id)
        ON UPDATE RESTRICT;

ALTER TABLE subscription
    ADD COLUMN notebook_id INT UNSIGNED


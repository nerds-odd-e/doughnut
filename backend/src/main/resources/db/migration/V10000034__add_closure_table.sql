CREATE TABLE notes_closure (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    note_id INT UNSIGNED NOT NULL,
    ancestor_id INT UNSIGNED NOT NULL,
    depth INT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `fk_notes_closure_note_id`
        FOREIGN KEY (note_id) REFERENCES note (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT `fk_notes_closure_ancestor_id`
        FOREIGN KEY (ancestor_id) REFERENCES note (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE notes_closure
    ADD UNIQUE INDEX (note_id, ancestor_id);


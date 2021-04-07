CREATE TABLE notebook_head_note (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    notebook_id INT UNSIGNED NOT NULL,
    head_note_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `fk_notebook_head_note_head_note_id`
        FOREIGN KEY (head_note_id) REFERENCES note (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT `fk_notebook_head_note_notebook_id`
        FOREIGN KEY (notebook_id) REFERENCES notebook (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE note
    RENAME COLUMN note_book_id TO notebook_id;


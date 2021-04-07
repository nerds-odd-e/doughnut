CREATE TABLE note_book (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    top_note_id INT UNSIGNED NOT NULL,
    ownership_id INT UNSIGNED NOT NULL,
    creator_id INT UNSIGNED NOT NULL,
    skip_review_entirely BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (id),
    CONSTRAINT `fk_notes_book_creator_id`
        FOREIGN KEY (creator_id) REFERENCES user (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT `fk_notes_book_top_note_id`
        FOREIGN KEY (top_note_id) REFERENCES note (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT `fk_notes_book_ownership_id`
        FOREIGN KEY (ownership_id) REFERENCES ownership (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE note
    ADD COLUMN note_book_id INT UNSIGNED


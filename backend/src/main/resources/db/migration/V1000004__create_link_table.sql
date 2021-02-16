CREATE TABLE link (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_id INT UNSIGNED NOT NULL,
    target_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `fk_link_note_source`
        FOREIGN KEY (source_id) REFERENCES note (id)
        ON DELETE CASCADE,
    CONSTRAINT `fk_link_note_target`
            FOREIGN KEY (target_id) REFERENCES note (id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

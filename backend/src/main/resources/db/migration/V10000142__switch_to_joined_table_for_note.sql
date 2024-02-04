CREATE TABLE hierarchical_note (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `note_id` INT UNSIGNED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unq_note_id` (`note_id`), -- Ensure note_id is unique
    CONSTRAINT `fk_hierarchical_note_note_id`
        FOREIGN KEY (`note_id`)
        REFERENCES `note` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO hierarchical_note (note_id)
SELECT id
FROM note;

ALTER TABLE note
DROP COLUMN note_type;

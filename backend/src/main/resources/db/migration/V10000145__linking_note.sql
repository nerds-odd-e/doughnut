CREATE TABLE linking_note (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    note_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY unq_note_id (note_id),
    CONSTRAINT fk_linking_note_note_id FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE,
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO linking_note (note_id)
SELECT id
FROM note
WHERE target_note_id IS NOT NULL;

DELETE FROM hierarchical_note
WHERE note_id IN (
    SELECT note_id
    FROM linking_note
);

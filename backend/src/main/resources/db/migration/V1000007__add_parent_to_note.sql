ALTER TABLE note
ADD COLUMN (
    parent_id INT UNSIGNED,
    constraint `fk_parent_id`
        FOREIGN KEY (parent_id) REFERENCES note (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
);

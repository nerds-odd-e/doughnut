CREATE TABLE tree_node (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    note_id INT UNSIGNED NOT NULL,
    sibling_order BIGINT NOT NULL DEFAULT 1,
    parent_id INT UNSIGNED,
    PRIMARY KEY (id),
    constraint `fk_tree_node_parent_id`
        FOREIGN KEY (parent_id) REFERENCES tree_node (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT `fk_tree_node_note_id`
        FOREIGN KEY (note_id) REFERENCES note (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE tree_node
    ADD INDEX (parent_id, sibling_order);

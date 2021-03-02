CREATE TABLE review_point (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    note_id INT UNSIGNED NOT NULL,
    difficulty INT NOT NULL default 100,
    forgetting_curve_index INT NOT NULL default 100,
    last_reviewed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    constraint `fk_review_point_user_id`
        FOREIGN KEY (user_id) REFERENCES user (id)
        ON UPDATE RESTRICT,
    CONSTRAINT `fk_review_point_note_id`
        FOREIGN KEY (note_id) REFERENCES note (id)
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE review_point
    ADD INDEX (last_reviewed_at);

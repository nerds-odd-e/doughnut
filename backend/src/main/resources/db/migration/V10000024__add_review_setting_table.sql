CREATE TABLE review_setting (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    rememberSpelling BOOLEAN NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE note
    ADD COLUMN master_review_setting_id INT UNSIGNED DEFAULT NULL;

ALTER TABLE note
    ADD CONSTRAINT `fk_note_master_review_setting_id`
        FOREIGN KEY (master_review_setting_id) REFERENCES review_setting (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT;



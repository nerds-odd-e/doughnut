CREATE TABLE subscription (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    note_id INT UNSIGNED NOT NULL,
    daily_target_of_new_notes INT UNSIGNED NOT NULL DEFAULT 5,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE subscription
    ADD CONSTRAINT `fk_subscription_user_id`
        FOREIGN KEY (user_id) REFERENCES user (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT;
ALTER TABLE subscription
    ADD CONSTRAINT `fk_subscription_note_id`
        FOREIGN KEY (note_id) REFERENCES note (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT;

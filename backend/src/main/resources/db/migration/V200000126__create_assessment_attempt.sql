CREATE TABLE assessment_attempt (
    id int unsigned NOT NULL AUTO_INCREMENT,
    user_id int unsigned NOT NULL,
    notebook_id int unsigned NOT NULL,
    submitted_at timestamp NULL DEFAULT NOW(),
    answers_total int unsigned NULL,
    answers_correct int unsigned NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (notebook_id) REFERENCES notebook(id)
);

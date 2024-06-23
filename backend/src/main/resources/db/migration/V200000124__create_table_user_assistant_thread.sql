CREATE TABLE user_assistant_thread (
    id int unsigned NOT NULL AUTO_INCREMENT,
    user_id int unsigned NOT NULL,
    note_id int unsigned NOT NULL,
    thread_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (note_id) REFERENCES note(id)
);

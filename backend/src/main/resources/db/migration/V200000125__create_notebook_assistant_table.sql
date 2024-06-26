CREATE TABLE notebook_assistant (
    id int unsigned NOT NULL AUTO_INCREMENT,
    creator_id int unsigned NOT NULL,
    notebook_id int unsigned NOT NULL,
    assistant VARCHAR(255) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (creator_id) REFERENCES user(id),
    FOREIGN KEY (notebook_id) REFERENCES notebook(id)
);

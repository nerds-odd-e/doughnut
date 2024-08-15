CREATE TABLE certificate (
    id int unsigned NOT NULL AUTO_INCREMENT,
    user_id int unsigned NOT NULL,
    notebook_id int unsigned NOT NULL,
    expiry_date timestamp NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (notebook_id) REFERENCES notebook(id)
);

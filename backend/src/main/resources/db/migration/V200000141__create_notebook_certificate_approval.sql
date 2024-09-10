CREATE TABLE notebook_certificate_approval (
    id int unsigned NOT NULL AUTO_INCREMENT,
    notebook_id int unsigned NOT NULL UNIQUE,
    last_approval_time TIMESTAMP NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (notebook_id) REFERENCES notebook(id)
);

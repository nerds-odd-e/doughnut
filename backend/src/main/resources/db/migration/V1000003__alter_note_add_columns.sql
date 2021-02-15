ALTER TABLE note
ADD COLUMN (
description VARCHAR(256),
user_id INT UNSIGNED,
created_datetime TIMESTAMP,
constraint `fk_user_id`
    FOREIGN KEY (user_id) REFERENCES user (id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
);

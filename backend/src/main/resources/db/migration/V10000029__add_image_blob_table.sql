CREATE TABLE image_blob (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    data BLOB,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

CREATE TABLE image (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    name varchar(255) not null,
    type varchar(255) not null,
    storage_type varchar(20) not null,
    image_blob_id INT UNSIGNED,
    PRIMARY KEY (id),
    constraint `fk_image_image_blob_id`
        FOREIGN KEY (image_blob_id) REFERENCES image_blob (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    constraint `fk_image_user_id`
        FOREIGN KEY (user_id) REFERENCES user (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE note
    ADD COLUMN image_id INT UNSIGNED;

ALTER TABLE note
    ADD CONSTRAINT `fk_note_image_id`
        FOREIGN KEY (image_id) REFERENCES image (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT;

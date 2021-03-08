CREATE TABLE circle (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name varchar(255) not null,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

CREATE TABLE circle_user (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    circle_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    constraint `fk_circle_user_user_id`
        FOREIGN KEY (user_id) REFERENCES user (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    constraint `fk_circle_user_circle_id`
        FOREIGN KEY (circle_id) REFERENCES circle (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

CREATE UNIQUE INDEX circle_user_circle_user_id ON circle_user(circle_id, user_id);

CREATE TABLE ownership (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED,
    circle_id INT UNSIGNED,
    PRIMARY KEY (id),
    constraint `fk_ownership_user_id`
        FOREIGN KEY (user_id) REFERENCES user (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    constraint `fk_ownership_circle_id`
        FOREIGN KEY (circle_id) REFERENCES circle (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

ALTER TABLE note
    ADD COLUMN ownership_id INT UNSIGNED;

INSERT INTO ownership (id, user_id)
SELECT id, id FROM user;

UPDATE note SET ownership_id = user_id;

CREATE UNIQUE INDEX ownership_user_id ON ownership(user_id);
CREATE UNIQUE INDEX ownership_circle_id ON ownership(circle_id);

ALTER TABLE note
    ADD CONSTRAINT `fk_note_ownership_id`
        FOREIGN KEY (ownership_id) REFERENCES ownership (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT;

CREATE TABLE round (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    player_id INT UNSIGNED,
    game_id INT UNSIGNED,
    round_no INT,
    mode VARCHAR(255),
    dice INT,
    damage INT,
    step INT,
    create_date DATETIME,
    update_date DATETIME
);

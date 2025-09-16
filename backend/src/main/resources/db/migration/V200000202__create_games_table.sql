CREATE TABLE game (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    start_date DATETIME,
    num_of_players INT,
    winner VARCHAR(255),
    end_date DATETIME,
    max_steps INT,
    update_date DATETIME
);

CREATE TABLE user (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name varchar(100) not null,
    external_identifier varchar(100) not null,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1;

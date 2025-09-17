ALTER TABLE round
ADD FOREIGN KEY (player_id) REFERENCES player(id),
ADD FOREIGN KEY (game_id) REFERENCES game(id);


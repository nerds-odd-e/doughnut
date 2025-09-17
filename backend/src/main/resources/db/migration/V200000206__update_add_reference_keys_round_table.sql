ALTER TABLE rounds
ADD FOREIGN KEY (player_id) REFERENCES players(id),
ADD FOREIGN KEY (game_id) REFERENCES games(id);


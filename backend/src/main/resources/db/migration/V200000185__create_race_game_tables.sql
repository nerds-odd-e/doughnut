CREATE TABLE race_game_progress (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    player_id VARCHAR(255) NOT NULL,
    car_position INTEGER NOT NULL DEFAULT 0,
    round_count INTEGER NOT NULL DEFAULT 0,
    last_dice_face INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT unique_player_id UNIQUE (player_id),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Index for quick lookups by player_id
CREATE INDEX idx_race_game_progress_player_id ON race_game_progress(player_id); 
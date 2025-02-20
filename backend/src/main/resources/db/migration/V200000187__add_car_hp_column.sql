-- Add HP column to car table
ALTER TABLE car
ADD COLUMN hp INTEGER NOT NULL DEFAULT 6;

-- Add HP column to race_game_progress table
ALTER TABLE race_game_progress
ADD COLUMN car_hp INTEGER NOT NULL DEFAULT 6; 
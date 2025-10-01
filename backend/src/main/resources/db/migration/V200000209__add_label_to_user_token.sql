-- Add label column to user_token table

ALTER TABLE user_token
  ADD COLUMN label VARCHAR(255);

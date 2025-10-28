-- Add last_used_at column to user_token table
ALTER TABLE user_token
ADD COLUMN last_used_at TIMESTAMP(3) NULL;


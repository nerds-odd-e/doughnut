-- Add ignored_points column to note table
-- The column stores a list of ignored points as text
ALTER TABLE note ADD COLUMN ignored_points TEXT DEFAULT NULL;

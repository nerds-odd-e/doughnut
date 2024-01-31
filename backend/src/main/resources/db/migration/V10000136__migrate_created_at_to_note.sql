-- Step 1: Add created_at field to note table
ALTER TABLE note ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Step 2: Migrate data from thing to note
-- Assuming that thing.note_id is a foreign key to note.id
UPDATE note n
INNER JOIN thing t ON n.id = t.note_id
SET n.created_at = t.created_at;

-- Step 3: Remove created_at field from thing table
ALTER TABLE thing DROP COLUMN created_at;

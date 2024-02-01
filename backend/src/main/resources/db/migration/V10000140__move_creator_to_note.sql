-- Step 1: Add creator_id field to note table
ALTER TABLE note ADD COLUMN creator_id INT UNSIGNED;

-- Step 2: Migrate data from thing to note
UPDATE note n
INNER JOIN thing t ON n.id = t.note_id
SET n.creator_id = t.creator_id;

-- Step 3: Drop foreign key constraint from thing table
ALTER TABLE thing DROP FOREIGN KEY FK_thing_user_id;

-- Step 4: Remove creator_id field from thing table
ALTER TABLE thing DROP COLUMN creator_id;

-- Step 5: Add foreign key constraint to note table
-- Adjust 'user' table and 'id' column as per your database schema
ALTER TABLE note ADD CONSTRAINT FK_note_creator_id FOREIGN KEY (creator_id) REFERENCES user(id) ON DELETE SET NULL;

-- Add relation_type column to note table
-- The column stores the RelationType enum value as a string (VARCHAR)
ALTER TABLE note ADD COLUMN relation_type VARCHAR(50) DEFAULT NULL AFTER note_type;

-- Make title nullable to support relation notes (which have null title)
ALTER TABLE note MODIFY COLUMN title VARCHAR(150) DEFAULT NULL;

-- Migrate existing relation types from title to relation_type
-- For notes where title starts with ":", extract the relation type and set title to NULL
UPDATE note 
SET relation_type = SUBSTRING(title, 2),
    title = NULL
WHERE title LIKE ':%' AND target_note_id IS NOT NULL;


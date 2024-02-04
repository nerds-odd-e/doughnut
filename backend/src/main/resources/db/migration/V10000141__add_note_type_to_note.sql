-- Step 1: Add the note_type column with a default value to cover existing rows
ALTER TABLE note
ADD COLUMN note_type VARCHAR(10) DEFAULT 'note' NOT NULL;

-- Step 2: Update the note_type based on the condition of topic_constructor starting with ":"
UPDATE note
SET note_type = CASE
    WHEN topic_constructor LIKE ':%' THEN 'link'
    ELSE 'note'
END;

-- Step 3: Make the note_type column NOT NULL and remove the default value
ALTER TABLE note
ALTER COLUMN note_type DROP DEFAULT;

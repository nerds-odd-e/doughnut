-- Assuming category_link_id currently stores note.id values and you've already migrated data from note to linking_note

-- Step 1: Drop the existing foreign key constraint for note_id if it's no longer needed or if you need to recreate it later
ALTER TABLE quiz_question DROP FOREIGN KEY quiz_question_fk_note_id;

-- Step 2: Add a new column for linking_note_id to temporarily hold the new foreign key values
ALTER TABLE quiz_question ADD COLUMN linking_note_id INT UNSIGNED DEFAULT NULL;

-- Step 3: Populate the linking_note_id column based on existing category_link_id values
-- This step assumes that you've already populated linking_note with entries from note, including those referenced by category_link_id
UPDATE quiz_question qq
JOIN linking_note ln ON qq.category_link_id = ln.note_id
SET qq.linking_note_id = ln.id;

-- Step 4: Drop the old category_link_id column if it's no longer needed
ALTER TABLE quiz_question DROP COLUMN category_link_id;

-- Step 5: Rename linking_note_id back to category_link_id to maintain naming consistency
ALTER TABLE quiz_question CHANGE COLUMN linking_note_id category_link_id INT UNSIGNED DEFAULT NULL;

-- Step 6: Add the new foreign key constraint from category_link_id to linking_note.id
ALTER TABLE quiz_question ADD CONSTRAINT quiz_question_fk_linking_note_id FOREIGN KEY (category_link_id) REFERENCES linking_note (id) ON DELETE CASCADE;

-- Optional: If you removed the foreign key for note_id and still need it, recreate it as necessary
ALTER TABLE quiz_question ADD CONSTRAINT quiz_question_fk_note_id FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE;

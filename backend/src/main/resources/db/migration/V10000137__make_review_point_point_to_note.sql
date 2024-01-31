-- Step 1: Add note_id field to review_point table
ALTER TABLE review_point ADD COLUMN note_id INT UNSIGNED;

-- Step 2: Populate the note_id field
UPDATE review_point rp
INNER JOIN thing t ON rp.thing_id = t.id
SET rp.note_id = t.note_id;

-- Step 3: Drop the old foreign key constraint
ALTER TABLE review_point DROP FOREIGN KEY review_point_ibfk_1;

-- Step 4: Remove the thing_id field
ALTER TABLE review_point DROP INDEX user_thing;
ALTER TABLE review_point DROP COLUMN thing_id;

-- Step 5: Add the new foreign key constraint
ALTER TABLE review_point ADD CONSTRAINT review_point_fk_note_id FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE;
ALTER TABLE review_point ADD UNIQUE INDEX user_note (user_id, note_id);

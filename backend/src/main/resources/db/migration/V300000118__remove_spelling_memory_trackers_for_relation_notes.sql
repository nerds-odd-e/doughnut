-- Clean up dirty data for relation notes (notes with target_note_id)
-- Relation notes should not have spelling questions

-- 1. Remove spelling memory trackers for relation notes
DELETE mt FROM memory_tracker mt
INNER JOIN note n ON mt.note_id = n.id
WHERE mt.spelling = TRUE
AND n.target_note_id IS NOT NULL;

-- 2. Reset remember_spelling to false for relation notes
UPDATE note
SET remember_spelling = FALSE
WHERE target_note_id IS NOT NULL
AND remember_spelling = TRUE;

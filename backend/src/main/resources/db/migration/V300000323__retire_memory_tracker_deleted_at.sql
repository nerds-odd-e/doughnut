-- Retire the duplicate memory_tracker.deleted_at persistence state.
-- Tracker activity is now derived from the owning note's deletion state
-- (note.deleted_at); the tracker column and its soft-delete functional index
-- are no longer needed. Replace the functional unique index (which allowed
-- duplicate keys among soft-deleted trackers) with a plain unique index over
-- (user_id, note_id, type, property_key). The duplicate-key state allowed by
-- the old functional index is unreachable through application workflows:
-- assimilation refuses to create a tracker when one with the same
-- (user, note, type, property_key) already exists, note deletion mirrors to all
-- trackers without removing rows, and tracker hard-delete removes rows entirely.
ALTER TABLE memory_tracker DROP INDEX user_note_spelling_active;
ALTER TABLE memory_tracker DROP COLUMN deleted_at;
ALTER TABLE memory_tracker ADD UNIQUE KEY user_note_spelling_active (user_id, note_id, type, property_key);

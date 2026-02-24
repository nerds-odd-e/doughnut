UPDATE memory_tracker mt
JOIN note n ON mt.note_id = n.id
SET mt.deleted_at = n.deleted_at
WHERE n.deleted_at IS NOT NULL;

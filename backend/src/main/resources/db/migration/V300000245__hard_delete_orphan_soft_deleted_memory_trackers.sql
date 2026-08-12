-- Hard-delete memory_tracker rows that are soft-deleted while their note is still live.
-- Trackers on soft-deleted notes keep deleted_at for note-restore cascade.

DELETE mt
FROM `memory_tracker` mt
INNER JOIN `note` n ON n.id = mt.note_id
WHERE mt.deleted_at IS NOT NULL
  AND n.deleted_at IS NULL;

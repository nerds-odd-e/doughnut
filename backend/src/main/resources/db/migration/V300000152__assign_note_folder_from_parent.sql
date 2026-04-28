UPDATE note n
INNER JOIN folder f ON f.backfill_source_note_id = n.parent_id
SET n.folder_id = f.id
WHERE n.parent_id IS NOT NULL
  AND n.deleted_at IS NULL;

ALTER TABLE folder DROP FOREIGN KEY fk_folder_backfill_source_note;

ALTER TABLE folder DROP INDEX uk_folder_backfill_source_note;

ALTER TABLE folder DROP COLUMN backfill_source_note_id;

UPDATE folder f
INNER JOIN note container ON container.id = f.backfill_source_note_id
INNER JOIN folder parent_folder ON parent_folder.backfill_source_note_id = container.parent_id
SET f.parent_folder_id = parent_folder.id
WHERE container.parent_id IS NOT NULL;

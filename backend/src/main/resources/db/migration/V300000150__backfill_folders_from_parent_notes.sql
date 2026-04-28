ALTER TABLE folder
  ADD COLUMN backfill_source_note_id INT UNSIGNED NULL AFTER name,
  ADD UNIQUE KEY uk_folder_backfill_source_note (backfill_source_note_id),
  ADD CONSTRAINT fk_folder_backfill_source_note FOREIGN KEY (backfill_source_note_id) REFERENCES note (id) ON DELETE SET NULL;

INSERT INTO folder (
  notebook_id,
  parent_folder_id,
  name,
  created_at,
  updated_at,
  backfill_source_note_id
)
SELECT
  p.notebook_id,
  NULL,
  COALESCE(NULLIF(TRIM(p.title), ''), CONCAT('note-', p.id)),
  p.created_at,
  p.updated_at,
  p.id
FROM note p
WHERE EXISTS (
  SELECT 1
  FROM note c
  WHERE c.parent_id = p.id
    AND c.deleted_at IS NULL
);

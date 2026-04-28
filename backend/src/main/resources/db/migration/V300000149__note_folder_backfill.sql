-- Backfill derived folders from parent-note containment and attach notes to folders (Phase 1.3–1.6).

ALTER TABLE folder
  ADD COLUMN tmp_derived_from_note_id INT UNSIGNED NULL AFTER updated_at,
  ADD UNIQUE KEY uk_folder_tmp_derived_from_note_id (tmp_derived_from_note_id);

INSERT INTO folder (notebook_id, parent_folder_id, name, created_at, updated_at, tmp_derived_from_note_id)
SELECT p.notebook_id, NULL, COALESCE(p.title, ''), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, p.id
FROM note p
WHERE p.deleted_at IS NULL
  AND p.notebook_id IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM note c
    WHERE c.parent_id = p.id AND c.deleted_at IS NULL
  );

UPDATE folder child_f
INNER JOIN note parent_note ON parent_note.id = child_f.tmp_derived_from_note_id
LEFT JOIN folder parent_f ON parent_f.tmp_derived_from_note_id = parent_note.parent_id
SET child_f.parent_folder_id = parent_f.id;

ALTER TABLE note
  ADD COLUMN folder_id INT UNSIGNED NULL AFTER notebook_id,
  ADD KEY idx_note_folder_id (folder_id),
  ADD CONSTRAINT fk_note_folder FOREIGN KEY (folder_id) REFERENCES folder (id) ON DELETE SET NULL;

UPDATE note n
INNER JOIN folder f ON f.tmp_derived_from_note_id = n.parent_id
SET n.folder_id = f.id
WHERE n.parent_id IS NOT NULL;

ALTER TABLE folder
  DROP COLUMN tmp_derived_from_note_id;

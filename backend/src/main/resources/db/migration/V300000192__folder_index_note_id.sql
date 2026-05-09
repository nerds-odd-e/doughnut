-- Designated folder index note pointer (cached FK). Title-based repair stays in application code.
ALTER TABLE folder
  ADD COLUMN index_note_id INT UNSIGNED NULL DEFAULT NULL,
  ADD KEY fk_folder_index_note_id (index_note_id),
  ADD CONSTRAINT fk_folder_index_note_id FOREIGN KEY (index_note_id) REFERENCES note (id) ON DELETE SET NULL ON UPDATE RESTRICT;

UPDATE folder f
INNER JOIN (
  SELECT folder_id, MIN(id) AS note_id
  FROM note
  WHERE deleted_at IS NULL
    AND folder_id IS NOT NULL
    AND LOWER(title) = 'index'
  GROUP BY folder_id
  HAVING COUNT(*) = 1
) eligible ON f.id = eligible.folder_id
SET f.index_note_id = eligible.note_id;

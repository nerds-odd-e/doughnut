-- Designated notebook index note pointer (cached FK). Title-based repair stays in application code.
ALTER TABLE notebook
  ADD COLUMN index_note_id INT UNSIGNED NULL DEFAULT NULL,
  ADD KEY fk_notebook_index_note_id (index_note_id),
  ADD CONSTRAINT fk_notebook_index_note_id FOREIGN KEY (index_note_id) REFERENCES note (id) ON DELETE SET NULL ON UPDATE RESTRICT;

UPDATE notebook nb
INNER JOIN (
  SELECT notebook_id, MIN(id) AS note_id
  FROM note
  WHERE deleted_at IS NULL
    AND folder_id IS NULL
    AND LOWER(title) = 'index'
  GROUP BY notebook_id
  HAVING COUNT(*) = 1
) eligible ON nb.id = eligible.notebook_id
SET nb.index_note_id = eligible.note_id;

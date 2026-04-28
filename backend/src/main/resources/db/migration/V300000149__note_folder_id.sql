ALTER TABLE note
  ADD COLUMN folder_id INT UNSIGNED NULL AFTER parent_id,
  ADD KEY idx_note_folder_id (folder_id),
  ADD CONSTRAINT fk_note_folder FOREIGN KEY (folder_id) REFERENCES folder (id) ON DELETE SET NULL;

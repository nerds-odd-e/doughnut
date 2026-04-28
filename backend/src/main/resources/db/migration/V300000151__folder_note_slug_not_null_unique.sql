ALTER TABLE folder
  MODIFY COLUMN slug VARCHAR(767) NOT NULL,
  ADD UNIQUE KEY uk_folder_notebook_slug (notebook_id, slug);

ALTER TABLE note
  MODIFY COLUMN slug VARCHAR(767) NOT NULL,
  ADD UNIQUE KEY uk_note_notebook_slug (notebook_id, slug);

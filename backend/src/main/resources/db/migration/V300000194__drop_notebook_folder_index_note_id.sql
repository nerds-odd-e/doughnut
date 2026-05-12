ALTER TABLE notebook
  DROP FOREIGN KEY fk_notebook_index_note_id,
  DROP COLUMN index_note_id;

ALTER TABLE folder
  DROP FOREIGN KEY fk_folder_index_note_id,
  DROP COLUMN index_note_id;

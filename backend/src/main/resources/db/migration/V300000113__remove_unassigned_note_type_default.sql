-- Remove UNASSIGNED note type: migrate existing 'unassigned' values to NULL and remove DEFAULT constraint
UPDATE note SET note_type = NULL WHERE note_type = 'unassigned';
ALTER TABLE note MODIFY COLUMN note_type VARCHAR(50) DEFAULT NULL;


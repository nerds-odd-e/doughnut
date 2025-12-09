-- Add note_type column to note table
-- The column stores the NoteType enum value as a string (VARCHAR)
-- Default value is 'unassigned' for existing notes
ALTER TABLE note ADD COLUMN note_type VARCHAR(50) DEFAULT 'unassigned' AFTER details;

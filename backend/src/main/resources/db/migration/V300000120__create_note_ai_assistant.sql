-- Create note_ai_assistant table for storing AI custom instructions per note
CREATE TABLE note_ai_assistant (
  id int unsigned NOT NULL AUTO_INCREMENT,
  note_id int unsigned NOT NULL UNIQUE,
  additional_instructions_to_ai TEXT,
  apply_to_children BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create index for faster lookups by note_id
CREATE INDEX idx_note_ai_assistant_note_id ON note_ai_assistant(note_id);

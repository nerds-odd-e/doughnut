-- Drop table and index if they exist first
DROP TABLE IF EXISTS note_embeddings;

-- Create note_embeddings table (shared migration using Flyway placeholders)
CREATE TABLE note_embeddings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  note_id BIGINT NOT NULL,
  kind ENUM('TITLE','DETAILS') NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Common indexes
CREATE INDEX idx_note_embeddings_note_id ON note_embeddings(note_id);

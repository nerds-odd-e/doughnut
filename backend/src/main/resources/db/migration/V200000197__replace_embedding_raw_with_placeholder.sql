-- Add environment-specific embedding column
ALTER TABLE note_embeddings ADD COLUMN ${embedding_column};

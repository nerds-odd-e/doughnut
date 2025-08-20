-- Add environment-specific embedding column
ALTER TABLE note_embeddings ADD COLUMN ${embedding_column};

-- Optional env-specific vector index (empty in local)
${vector_index_statement}



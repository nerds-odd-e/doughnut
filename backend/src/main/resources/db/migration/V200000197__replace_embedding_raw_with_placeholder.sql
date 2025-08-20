-- Switch note_embeddings to use Flyway placeholder-based embedding column

-- Remove the old raw embedding column
ALTER TABLE note_embeddings DROP COLUMN IF EXISTS embedding_raw;

-- Add environment-specific embedding column
ALTER TABLE note_embeddings ADD COLUMN ${embedding_column};

-- Optional env-specific vector index (empty in local)
${vector_index_statement}



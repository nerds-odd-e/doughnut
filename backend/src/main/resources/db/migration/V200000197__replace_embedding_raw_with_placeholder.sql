-- Switch note_embeddings to use Flyway placeholder-based embedding column

-- Remove the old raw embedding column
SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'note_embeddings'
  AND column_name = 'embedding_raw';

IF @exists > 0 THEN
  ALTER TABLE note_embeddings DROP COLUMN embedding_raw;
END IF;

-- Add environment-specific embedding column
ALTER TABLE note_embeddings ADD COLUMN ${embedding_column};

-- Optional env-specific vector index (empty in local)
${vector_index_statement}



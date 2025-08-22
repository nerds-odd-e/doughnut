-- Remove DETAILS embeddings and drop the kind column as we now store a single embedding per note
DELETE FROM note_embeddings WHERE kind = 'DETAILS';

ALTER TABLE note_embeddings DROP COLUMN kind;



-- Drop the retired note.deleted_at column. Legacy soft-deleted notes were migrated into
-- location-based _trash by V300000326, so note availability now follows folder location alone.
-- idx_note_structural_peer included deleted_at as a leading filter column; recreate it without
-- the dropped column so structural-peer lookups keep their covering index.
ALTER TABLE note DROP INDEX idx_note_structural_peer;
ALTER TABLE note DROP COLUMN deleted_at;
ALTER TABLE note ADD INDEX idx_note_structural_peer (notebook_id, folder_id, id);
